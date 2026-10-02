package com.ktk.dukappservice.data.jobs;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.activity.ActivityService;
import com.ktk.dukappservice.data.activityitems.ActivityItem;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobregistrations.JobRegistrationService;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.JobStatus;
import com.ktk.dukappservice.enums.Permission;
import com.ktk.dukappservice.service.BaseService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.ktk.dukappservice.enums.JobRegistrationStatus.*;

/**
 * Job lifecycle: create/edit/cancel, registration with capacity, waitlist and eligibility rules, and completion
 * (submitting hours), which turns the job into a regular {@link Activity}.
 * <p>
 * Failures are reported as {@link ResponseStatusException}: 400 invalid input, 403 not allowed, 404 not found,
 * 409 wrong state (closed, deadline passed, full, already registered), 422 user not eligible (age / gender).
 */
@Service
public class JobService extends BaseService<Job, Long> {

    /** Same boundary as {@code UserController#getFamily}: older than this is a parent, this age or younger a child. */
    static final int ADULT_AGE = 18;
    private static final int ACTIVITY_ITEM_DESCRIPTION_MAX = 150;
    private static final List<JobRegistrationStatus> ACTIVE = List.of(REGISTERED, WAITLISTED);

    private final JobRepository repository;
    private final JobRegistrationService registrations;
    private final ActivityService activityService;
    private final ActivityItemService activityItemService;
    private final RoundService roundService;

    public JobService(JobRepository repository, JobRegistrationService registrations, ActivityService activityService,
                      ActivityItemService activityItemService, RoundService roundService) {
        this.repository = repository;
        this.registrations = registrations;
        this.activityService = activityService;
        this.activityItemService = activityItemService;
        this.roundService = roundService;
    }

    /** Hours of one registered user, as submitted when completing a job. {@code description} is optional. */
    public record HoursEntry(Long userId, double hours, String description) {
    }

    // ---------------------------------------------------------------- queries

    public Page<Job> fetchByQuery(JobStatus status, Long responsibleId, Long registeredUserId, boolean openOnly,
                                  LocalDate dateFrom, LocalDate dateTo, String searchText, Pageable pageable) {
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get(Job.Fields.status), status));
            }
            if (responsibleId != null) {
                predicates.add(cb.equal(root.get(Job.Fields.responsible).get("id"), responsibleId));
            }
            if (openOnly) {
                predicates.add(cb.equal(root.get(Job.Fields.status), JobStatus.OPEN));
                predicates.add(cb.greaterThan(root.<LocalDateTime>get(Job.Fields.registrationDeadline), LocalDateTime.now()));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get(Job.Fields.jobDateTime), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get(Job.Fields.jobDateTime), dateTo.plusDays(1).atStartOfDay()));
            }
            if (searchText != null && !searchText.isBlank()) {
                predicates.add(cb.like(cb.lower(root.<String>get(Job.Fields.description)), "%" + searchText.trim().toLowerCase() + "%"));
            }
            if (registeredUserId != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<JobRegistration> reg = sub.from(JobRegistration.class);
                sub.select(reg.<Long>get("id")).where(
                        cb.equal(reg.get(JobRegistration.Fields.job), root),
                        cb.equal(reg.get(JobRegistration.Fields.user).get("id"), registeredUserId),
                        reg.get(JobRegistration.Fields.status).in(ACTIVE));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public Map<Long, JobCounts> countsFor(Collection<Long> jobIds) {
        Map<Long, JobCounts> result = new HashMap<>();
        for (Object[] row : registrations.countByJobsAndStatuses(jobIds, ACTIVE)) {
            Long jobId = ((Number) row[0]).longValue();
            JobRegistrationStatus status = (JobRegistrationStatus) row[1];
            long count = ((Number) row[2]).longValue();
            JobCounts current = result.getOrDefault(jobId, JobCounts.NONE);
            result.put(jobId, status == REGISTERED
                    ? new JobCounts(count, current.waitlisted())
                    : new JobCounts(current.registered(), count));
        }
        return result;
    }

    public JobCounts countsFor(Long jobId) {
        return countsFor(List.of(jobId)).getOrDefault(jobId, JobCounts.NONE);
    }

    /** The user's own non-cancelled registration status per job. */
    public Map<Long, JobRegistrationStatus> statusesForUser(Collection<Long> jobIds, Long userId) {
        Map<Long, JobRegistrationStatus> result = new HashMap<>();
        for (JobRegistration r : registrations.findByJobsAndUser(jobIds, userId)) {
            if (r.getStatus() != CANCELLED) {
                result.put(r.getJob().getId(), r.getStatus());
            }
        }
        return result;
    }

    /** Registered users first, then the waitlist in promotion order. */
    public List<JobRegistration> findActiveRegistrations(Long jobId) {
        List<JobRegistration> result = new ArrayList<>(registrations.findByJobAndStatus(jobId, REGISTERED));
        result.addAll(registrations.findByJobAndStatus(jobId, WAITLISTED));
        return result;
    }

    // ---------------------------------------------------------------- permissions

    /** Edit or cancel: admins, or the creator while they still have {@link Permission#JOB_CREATE}. */
    public boolean canEdit(User actor, Job job) {
        return actor.hasPermission(Permission.JOB_MANAGE_ALL)
                || (actor.hasPermission(Permission.JOB_CREATE) && job.getCreateUser().getId().equals(actor.getId()));
    }

    public boolean canComplete(User actor, Job job) {
        return actor.hasPermission(Permission.JOB_MANAGE_ALL) || job.getResponsible().getId().equals(actor.getId());
    }

    /** Registration comments are visible to the people running the job, in addition to the registrant and their parents. */
    public boolean isOrganizer(User actor, Job job) {
        return actor.hasPermission(Permission.JOB_MANAGE_ALL)
                || job.getResponsible().getId().equals(actor.getId())
                || job.getCreateUser().getId().equals(actor.getId());
    }

    /** Yourself, your child (same familyId, you are an adult and they are not), or anyone with JOB_MANAGE_ALL. */
    public boolean canActFor(User actor, User target) {
        return actor.getId().equals(target.getId())
                || actor.hasPermission(Permission.JOB_MANAGE_ALL)
                || isParentOf(actor, target);
    }

    public static boolean isParentOf(User parent, User child) {
        return parent.getFamilyId() != null
                && parent.getFamilyId().equals(child.getFamilyId())
                && parent.getBirthDate() != null && child.getBirthDate() != null
                && parent.getAge() > ADULT_AGE
                && child.getAge() <= ADULT_AGE;
    }

    /** Age is measured on the day of the job. A missing birth date / gender never passes a limit that needs it. */
    public void checkEligibility(Job job, User user) {
        if (job.getGenderRestriction() != null) {
            if (user.getGender() == null) {
                throw unprocessable(user.getFullName() + " has no gender set, which this job requires. Ask an admin to set it.");
            }
            if (user.getGender() != job.getGenderRestriction()) {
                throw unprocessable("This job is only open to " + job.getGenderRestriction().name().toLowerCase() + " participants.");
            }
        }
        if (job.getMinAge() != null || job.getMaxAge() != null) {
            if (user.getBirthDate() == null) {
                throw unprocessable(user.getFullName() + " has no birth date set, which this job requires. Ask an admin to set it.");
            }
            int age = user.getAgeAtDate(job.getJobDateTime().toLocalDate());
            if (job.getMinAge() != null && age < job.getMinAge()) {
                throw unprocessable("Participants must be at least " + job.getMinAge() + " years old on the day of the job.");
            }
            if (job.getMaxAge() != null && age > job.getMaxAge()) {
                throw unprocessable("Participants must be at most " + job.getMaxAge() + " years old on the day of the job.");
            }
        }
    }

    // ---------------------------------------------------------------- job lifecycle

    @Transactional
    public Job create(Job job) {
        job.setStatus(JobStatus.OPEN);
        job.setCreateDateTime(LocalDateTime.now());
        validate(job);
        if (!job.getJobDateTime().isAfter(LocalDateTime.now())) {
            throw badRequest("The job date must be in the future.");
        }
        return save(job);
    }

    /**
     * Applies {@code applyChanges} to the job under the row lock. The number of places can't drop below the number of
     * registered users; raising it promotes waitlisted users. Existing registrations are never evicted when the age or
     * gender limits change.
     */
    @Transactional
    public Job update(Long id, User actor, java.util.function.Consumer<Job> applyChanges) {
        Job job = lock(id);
        if (!canEdit(actor, job)) {
            throw forbidden();
        }
        requireOpen(job);
        applyChanges.accept(job);
        validate(job);
        if (job.getMaxParticipants() != null) {
            long registered = registrations.countByJobAndStatus(id, REGISTERED);
            if (job.getMaxParticipants() < registered) {
                throw conflict(registered + " users are already registered, the limit can't be lower than that.");
            }
        }
        Job saved = save(job);
        promoteFromWaitlist(saved);
        return saved;
    }

    @Transactional
    public Job cancelJob(Long id, User actor) {
        Job job = lock(id);
        if (!canEdit(actor, job)) {
            throw forbidden();
        }
        requireOpen(job);
        job.setStatus(JobStatus.CANCELLED);
        return save(job);
    }

    // ---------------------------------------------------------------- registration

    /** Registers {@code target} (taking a place, or the waitlist when full), or re-registers after a cancellation. */
    @Transactional
    public JobRegistration register(Long jobId, User target, User actor, String comment) {
        Job job = lock(jobId);
        requireCanActFor(actor, target);
        requireOpen(job);
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(job.getRegistrationDeadline())) {
            throw conflict("The registration deadline has passed.");
        }
        checkEligibility(job, target);

        Optional<JobRegistration> existing = registrations.findByJobAndUser(jobId, target.getId());
        if (existing.isPresent() && existing.get().getStatus() != CANCELLED) {
            throw conflict(target.getFullName() + " is already registered for this job.");
        }

        JobRegistrationStatus status;
        if (job.getMaxParticipants() == null || registrations.countByJobAndStatus(jobId, REGISTERED) < job.getMaxParticipants()) {
            status = REGISTERED;
        } else if (job.isWaitlistEnabled()) {
            status = WAITLISTED;
        } else {
            throw conflict("This job is full.");
        }

        JobRegistration registration = existing.orElseGet(JobRegistration::new);
        registration.setJob(job);
        registration.setUser(target);
        registration.setRegisteredBy(actor);
        registration.setComment(comment);
        registration.setStatus(status);
        registration.setRegisteredDateTime(now);
        registration.setCancelledDateTime(null);
        registration.setHours(0);
        return registrations.save(registration);
    }

    @Transactional
    public JobRegistration updateComment(Long jobId, User target, User actor, String comment) {
        Job job = lock(jobId);
        requireCanActFor(actor, target);
        requireOpen(job);
        JobRegistration registration = findActiveRegistration(jobId, target);
        registration.setComment(comment);
        return registrations.save(registration);
    }

    /**
     * Cancels the registration and gives a freed place to the oldest waitlisted user. Registered users can only cancel
     * until the cancellation deadline (admins always can); leaving the waitlist is possible any time while the job is open.
     */
    @Transactional
    public JobRegistration cancelRegistration(Long jobId, User target, User actor) {
        Job job = lock(jobId);
        requireCanActFor(actor, target);
        requireOpen(job);
        JobRegistration registration = findActiveRegistration(jobId, target);
        LocalDateTime now = LocalDateTime.now();
        boolean heldPlace = registration.getStatus() == REGISTERED;
        if (heldPlace && now.isAfter(job.getCancellationDeadline()) && !actor.hasPermission(Permission.JOB_MANAGE_ALL)) {
            throw conflict("The cancellation deadline has passed. Contact the responsible user.");
        }
        registration.setStatus(CANCELLED);
        registration.setCancelledDateTime(now);
        registrations.save(registration);
        if (heldPlace) {
            promoteFromWaitlist(job);
        }
        return registration;
    }

    // ---------------------------------------------------------------- completion

    /**
     * Submits the hours of every registered user (0 for no-shows) and closes the job. This creates a regular
     * {@link Activity} with one {@link ActivityItem} per user with hours, which then goes through the existing
     * "register activity" step. Waitlisted users are ignored. All or nothing.
     */
    @Transactional
    public Activity complete(Long jobId, User actor, List<HoursEntry> entries) {
        Job job = lock(jobId);
        if (!canComplete(actor, job)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the responsible user can submit the hours.");
        }
        requireOpen(job);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(job.getJobDateTime())) {
            throw conflict("The job hasn't started yet.");
        }

        List<JobRegistration> registered = registrations.findByJobAndStatus(jobId, REGISTERED);
        Map<Long, HoursEntry> hoursByUser = new HashMap<>();
        for (HoursEntry entry : entries) {
            if (entry.userId() == null || Double.isNaN(entry.hours()) || Double.isInfinite(entry.hours()) || entry.hours() < 0) {
                throw badRequest("Hours must be a number of 0 or more for every user.");
            }
            if (hoursByUser.put(entry.userId(), entry) != null) {
                throw badRequest("Hours for user " + entry.userId() + " were submitted twice.");
            }
        }
        Set<Long> registeredUserIds = registered.stream().map(r -> r.getUser().getId()).collect(Collectors.toSet());
        for (Long userId : hoursByUser.keySet()) {
            if (!registeredUserIds.contains(userId)) {
                throw badRequest("User " + userId + " is not registered for this job.");
            }
        }
        for (JobRegistration registration : registered) {
            if (!hoursByUser.containsKey(registration.getUser().getId())) {
                throw badRequest("Hours are missing for " + registration.getUser().getFullName() + " (use 0 for no-shows).");
            }
        }
        if (hoursByUser.values().stream().mapToDouble(HoursEntry::hours).sum() <= 0) {
            throw badRequest("No hours were submitted.");
        }
        Round round = roundService.findRoundByDate(job.getJobDateTime())
                .orElseThrow(() -> conflict("No round covers the job date " + job.getJobDateTime().toLocalDate() + "."));

        Activity activity = new Activity();
        activity.setCreateDateTime(now);
        activity.setCreateUser(actor);
        activity.setActivityDateTime(job.getJobDateTime());
        activity.setDescription(job.getDescription());
        activity.setEmployer(job.getEmployer());
        activity.setResponsible(job.getResponsible());
        activity.setAccount(job.getAccount());
        activity.setTransactionType(job.getTransactionType());
        activity = activityService.save(activity);

        for (JobRegistration registration : registered) {
            HoursEntry entry = hoursByUser.get(registration.getUser().getId());
            registration.setHours(entry.hours());
            registrations.save(registration);
            if (entry.hours() > 0) {
                ActivityItem item = new ActivityItem();
                item.setActivity(activity);
                item.setUser(registration.getUser());
                item.setCreateUser(actor);
                item.setCreateDateTime(now);
                item.setDescription(truncate(entry.description() == null || entry.description().isBlank()
                        ? job.getDescription() : entry.description().trim(), ACTIVITY_ITEM_DESCRIPTION_MAX));
                item.setTransactionType(job.getTransactionType());
                item.setAccount(job.getAccount());
                item.setHours(entry.hours());
                item.setRound(round);
                activityItemService.save(item);
            }
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setActivity(activity);
        job.setCompletedDateTime(now);
        save(job);
        return activity;
    }

    // ---------------------------------------------------------------- helpers

    private void promoteFromWaitlist(Job job) {
        List<JobRegistration> waiting = registrations.findByJobAndStatus(job.getId(), WAITLISTED);
        if (waiting.isEmpty()) {
            return;
        }
        long free = job.getMaxParticipants() == null
                ? waiting.size()
                : job.getMaxParticipants() - registrations.countByJobAndStatus(job.getId(), REGISTERED);
        for (int i = 0; i < waiting.size() && i < free; i++) {
            waiting.get(i).setStatus(REGISTERED);
            registrations.save(waiting.get(i));
        }
    }

    private JobRegistration findActiveRegistration(Long jobId, User target) {
        return registrations.findByJobAndUser(jobId, target.getId())
                .filter(r -> r.getStatus() != CANCELLED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, target.getFullName() + " is not registered for this job."));
    }

    private Job lock(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id: " + id));
    }

    private void requireOpen(Job job) {
        if (job.getStatus() != JobStatus.OPEN) {
            throw conflict("The job is " + job.getStatus().name().toLowerCase() + ".");
        }
    }

    private void requireCanActFor(User actor, User target) {
        if (!canActFor(actor, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only do this for yourself or your children.");
        }
    }

    private void validate(Job job) {
        LocalDateTime date = job.getJobDateTime();
        if (job.getJobEndDateTime() != null && !job.getJobEndDateTime().isAfter(date)) {
            throw badRequest("The job must end after it starts.");
        }
        if (job.getRegistrationDeadline().isAfter(date)) {
            throw badRequest("The registration deadline must not be after the job date.");
        }
        if (job.getCancellationDeadline().isAfter(date)) {
            throw badRequest("The cancellation deadline must not be after the job date.");
        }
        if (job.getMaxParticipants() != null && job.getMaxParticipants() < 1) {
            throw badRequest("The number of places must be at least 1.");
        }
        if (job.getMinAge() != null && job.getMinAge() < 0 || job.getMaxAge() != null && job.getMaxAge() < 0) {
            throw badRequest("Age limits can't be negative.");
        }
        if (job.getMinAge() != null && job.getMaxAge() != null && job.getMinAge() > job.getMaxAge()) {
            throw badRequest("The minimum age can't be higher than the maximum age.");
        }
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied!");
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatusCode.valueOf(422), message);
    }

    @Override
    protected JpaRepository<Job, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<Job> getEntityClass() {
        return Job.class;
    }

    @Override
    public Job createEntity() {
        return new Job();
    }
}
