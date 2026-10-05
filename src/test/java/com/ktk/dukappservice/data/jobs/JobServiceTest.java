package com.ktk.dukappservice.data.jobs;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.activity.ActivityService;
import com.ktk.dukappservice.data.activityitems.ActivityItem;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobregistrations.JobRegistrationService;
import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the job rules. The registration service is backed by an in-memory list so capacity, waitlist
 * promotion and completion can be checked end to end without a database.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobServiceTest {

    @Mock JobRepository repository;
    @Mock JobRegistrationService registrations;
    @Mock ActivityService activityService;
    @Mock ActivityItemService activityItemService;
    @Mock RoundService roundService;
    @Mock com.ktk.dukappservice.data.users.UserService userService;

    private final List<JobRegistration> store = new ArrayList<>();
    private JobService service;
    private Job job;
    private User admin;
    private User organizer;
    private long nextId = 100;

    @BeforeEach
    void setUp() {
        service = new JobService(repository, registrations, activityService, activityItemService, roundService, userService);

        when(registrations.findByJobAndUser(anyLong(), anyLong())).thenAnswer(i -> store.stream()
                .filter(r -> r.getJob().getId().equals(i.getArgument(0)) && r.getUser().getId().equals(i.getArgument(1)))
                .findFirst());
        when(registrations.findByJobAndStatus(anyLong(), any())).thenAnswer(i -> store.stream()
                .filter(r -> r.getJob().getId().equals(i.getArgument(0)) && r.getStatus() == i.getArgument(1))
                .sorted(Comparator.comparing(JobRegistration::getRegisteredDateTime))
                .toList());
        when(registrations.countByJobAndStatus(anyLong(), any())).thenAnswer(i -> store.stream()
                .filter(r -> r.getJob().getId().equals(i.getArgument(0)) && r.getStatus() == i.getArgument(1))
                .count());
        when(registrations.save(any())).thenAnswer(i -> {
            JobRegistration r = i.getArgument(0);
            if (!store.contains(r)) {
                store.add(r);
            }
            return r;
        });
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityService.save(any())).thenAnswer(i -> i.getArgument(0));

        admin = user(1, "Admin", 40, Gender.MALE, null, Permission.JOB_MANAGE_ALL);
        organizer = user(2, "Organizer", 40, Gender.MALE, null, Permission.JOB_CREATE);

        job = new Job();
        job.setId(1L);
        job.setCreateUser(organizer);
        job.setResponsible(organizer);
        job.setEmployer(user(3, "Employer", 50, Gender.MALE, null));
        job.setDescription("Moving boxes");
        job.setAccount(Account.MYSHARE);
        job.setTransactionType(TransactionType.HOURS);
        job.setJobDateTime(LocalDateTime.now().plusDays(10));
        job.setRegistrationDeadline(LocalDateTime.now().plusDays(5));
        job.setCancellationDeadline(LocalDateTime.now().plusDays(3));
        job.setMaxParticipants(2);
        job.setWaitlistEnabled(true);
        job.setStatus(JobStatus.OPEN);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));
    }

    // ------------------------------------------------------------ capacity and waitlist

    @Test
    void fillsPlacesThenWaitlists() {
        User a = adult(10), b = adult(11), c = adult(12);
        assertThat(service.register(1L, a, a, null).getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        assertThat(service.register(1L, b, b, "late shift ok").getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        assertThat(service.register(1L, c, c, null).getStatus()).isEqualTo(JobRegistrationStatus.WAITLISTED);
    }

    @Test
    void fullJobWithoutWaitlistRejects() {
        job.setWaitlistEnabled(false);
        User a = adult(10), b = adult(11), c = adult(12);
        service.register(1L, a, a, null);
        service.register(1L, b, b, null);
        assertThat(statusOf(() -> service.register(1L, c, c, null))).isEqualTo(409);
    }

    @Test
    void unlimitedJobNeverWaitlists() {
        job.setMaxParticipants(null);
        for (int i = 0; i < 5; i++) {
            User u = adult(10 + i);
            assertThat(service.register(1L, u, u, null).getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        }
    }

    @Test
    void cancelPromotesOldestWaitlistedUser() {
        User a = adult(10), b = adult(11), c = adult(12), d = adult(13);
        JobRegistration ra = service.register(1L, a, a, null);
        service.register(1L, b, b, null);
        JobRegistration rc = service.register(1L, c, c, null);
        JobRegistration rd = service.register(1L, d, d, null);
        rc.setRegisteredDateTime(LocalDateTime.now().minusMinutes(2));
        rd.setRegisteredDateTime(LocalDateTime.now().minusMinutes(1));

        service.cancelRegistration(1L, a, a);

        assertThat(ra.getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
        assertThat(rc.getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        assertThat(rd.getStatus()).isEqualTo(JobRegistrationStatus.WAITLISTED);
    }

    @Test
    void leavingTheWaitlistPromotesNobody() {
        User a = adult(10), b = adult(11), c = adult(12), d = adult(13);
        service.register(1L, a, a, null);
        service.register(1L, b, b, null);
        JobRegistration rc = service.register(1L, c, c, null);
        JobRegistration rd = service.register(1L, d, d, null);

        service.cancelRegistration(1L, c, c);

        assertThat(rc.getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
        assertThat(rd.getStatus()).isEqualTo(JobRegistrationStatus.WAITLISTED);
    }

    @Test
    void registeringAgainAfterCancelReusesTheRow() {
        User a = adult(10);
        JobRegistration first = service.register(1L, a, a, "first");
        service.cancelRegistration(1L, a, a);
        JobRegistration second = service.register(1L, a, a, "second");

        assertThat(second).isSameAs(first);
        assertThat(second.getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        assertThat(second.getComment()).isEqualTo("second");
        assertThat(second.getCancelledDateTime()).isNull();
        assertThat(store).hasSize(1);
    }

    @Test
    void doubleRegistrationIsRejected() {
        User a = adult(10);
        service.register(1L, a, a, null);
        assertThat(statusOf(() -> service.register(1L, a, a, null))).isEqualTo(409);
    }

    // ------------------------------------------------------------ deadlines and state

    @Test
    void registrationAfterDeadlineIsRejectedEvenForAdmins() {
        job.setRegistrationDeadline(LocalDateTime.now().minusMinutes(1));
        User a = adult(10);
        assertThat(statusOf(() -> service.register(1L, a, a, null))).isEqualTo(409);
        assertThat(statusOf(() -> service.register(1L, a, admin, null))).isEqualTo(409);
    }

    @Test
    void cancellingAfterDeadlineNeedsAnAdmin() {
        User a = adult(10);
        service.register(1L, a, a, null);
        job.setCancellationDeadline(LocalDateTime.now().minusMinutes(1));

        assertThat(statusOf(() -> service.cancelRegistration(1L, a, a))).isEqualTo(409);
        service.cancelRegistration(1L, a, admin);
        assertThat(store.get(0).getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
    }

    @Test
    void waitlistedUserCanLeaveAfterCancellationDeadline() {
        User a = adult(10), b = adult(11), c = adult(12);
        service.register(1L, a, a, null);
        service.register(1L, b, b, null);
        service.register(1L, c, c, null);
        job.setCancellationDeadline(LocalDateTime.now().minusMinutes(1));

        service.cancelRegistration(1L, c, c);
        assertThat(store.get(2).getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
    }

    @Test
    void closedJobsTakeNoRegistrations() {
        User a = adult(10);
        job.setStatus(JobStatus.CANCELLED);
        assertThat(statusOf(() -> service.register(1L, a, a, null))).isEqualTo(409);
        job.setStatus(JobStatus.COMPLETED);
        assertThat(statusOf(() -> service.register(1L, a, a, null))).isEqualTo(409);
    }

    // ------------------------------------------------------------ eligibility

    @Test
    void genderRestriction() {
        job.setGenderRestriction(Gender.FEMALE);
        User man = user(10, "Man", 30, Gender.MALE, null);
        User woman = user(11, "Woman", 30, Gender.FEMALE, null);
        User unknown = user(12, "Unknown", 30, null, null);

        assertThat(statusOf(() -> service.register(1L, man, man, null))).isEqualTo(422);
        assertThat(statusOf(() -> service.register(1L, unknown, unknown, null))).isEqualTo(422);
        assertThat(service.register(1L, woman, woman, null).getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
    }

    @Test
    void ageLimitsAreInclusiveAndMeasuredOnTheJobDay() {
        job.setMinAge(16);
        job.setMaxAge(25);
        LocalDate jobDay = job.getJobDateTime().toLocalDate();
        User turns16OnJobDay = user(10, "A", 0, Gender.MALE, null);
        turns16OnJobDay.setBirthDate(jobDay.minusYears(16));
        User day_short = user(11, "B", 0, Gender.MALE, null);
        day_short.setBirthDate(jobDay.minusYears(16).plusDays(1));
        User turns26OnJobDay = user(12, "C", 0, Gender.MALE, null);
        turns26OnJobDay.setBirthDate(jobDay.minusYears(26));
        User is25 = user(13, "D", 0, Gender.MALE, null);
        is25.setBirthDate(jobDay.minusYears(26).plusDays(1));

        assertThat(service.register(1L, turns16OnJobDay, turns16OnJobDay, null)).isNotNull();
        assertThat(statusOf(() -> service.register(1L, day_short, day_short, null))).isEqualTo(422);
        assertThat(statusOf(() -> service.register(1L, turns26OnJobDay, turns26OnJobDay, null))).isEqualTo(422);
        assertThat(service.register(1L, is25, is25, null)).isNotNull();
    }

    @Test
    void ageLimitNeedsABirthDate() {
        job.setMinAge(16);
        User noBirthDate = user(10, "A", 0, Gender.MALE, null);
        noBirthDate.setBirthDate(null);
        assertThat(statusOf(() -> service.register(1L, noBirthDate, noBirthDate, null))).isEqualTo(422);
    }

    @Test
    void unrestrictedJobNeedsNeitherGenderNorBirthDate() {
        User nothingSet = user(10, "A", 0, null, null);
        nothingSet.setBirthDate(null);
        assertThat(service.register(1L, nothingSet, nothingSet, null)).isNotNull();
    }

    // ------------------------------------------------------------ registering for someone else

    @Test
    void parentRegistersChildButNotOthers() {
        User parent = user(10, "Parent", 40, Gender.MALE, 7L);
        User child = user(11, "Child", 12, Gender.MALE, 7L);
        User otherFamilyChild = user(12, "Other", 12, Gender.MALE, 8L);
        User spouse = user(13, "Spouse", 40, Gender.FEMALE, 7L);
        User noFamilyParent = user(14, "Loner", 40, Gender.MALE, null);
        User noFamilyChild = user(15, "Kid", 12, Gender.MALE, null);

        JobRegistration r = service.register(1L, child, parent, "mine");
        assertThat(r.getUser()).isSameAs(child);
        assertThat(r.getRegisteredBy()).isSameAs(parent);
        assertThat(statusOf(() -> service.register(1L, otherFamilyChild, parent, null))).isEqualTo(403);
        assertThat(statusOf(() -> service.register(1L, spouse, parent, null))).isEqualTo(403);
        assertThat(statusOf(() -> service.register(1L, noFamilyChild, noFamilyParent, null))).isEqualTo(403);
    }

    @Test
    void childCannotRegisterTheParent() {
        User parent = user(10, "Parent", 40, Gender.MALE, 7L);
        User child = user(11, "Child", 12, Gender.MALE, 7L);
        assertThat(statusOf(() -> service.register(1L, parent, child, null))).isEqualTo(403);
    }

    @Test
    void parentCancelsAndEditsCommentForChild() {
        User parent = user(10, "Parent", 40, Gender.MALE, 7L);
        User child = user(11, "Child", 12, Gender.MALE, 7L);
        service.register(1L, child, parent, "a");

        assertThat(service.updateComment(1L, child, parent, "b").getComment()).isEqualTo("b");
        assertThat(service.cancelRegistration(1L, child, parent).getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
    }

    @Test
    void adminRegistersAnyoneButEligibilityStillApplies() {
        job.setMinAge(18);
        User adult = adult(10);
        User kid = user(11, "Kid", 12, Gender.MALE, null);

        assertThat(service.register(1L, adult, admin, null).getRegisteredBy()).isSameAs(admin);
        assertThat(statusOf(() -> service.register(1L, kid, admin, null))).isEqualTo(422);
    }

    @Test
    void strangersCannotCancelOthers() {
        User a = adult(10), stranger = adult(11);
        service.register(1L, a, a, null);
        assertThat(statusOf(() -> service.cancelRegistration(1L, a, stranger))).isEqualTo(403);
    }

    // ------------------------------------------------------------ editing

    @Test
    void limitCannotDropBelowRegisteredCount() {
        User a = adult(10), b = adult(11);
        service.register(1L, a, a, null);
        service.register(1L, b, b, null);

        assertThat(statusOf(() -> service.update(1L, organizer, j -> j.setMaxParticipants(1)))).isEqualTo(409);
    }

    @Test
    void raisingTheLimitPromotesTheWaitlist() {
        User a = adult(10), b = adult(11), c = adult(12), d = adult(13), e = adult(14);
        service.register(1L, a, a, null);
        service.register(1L, b, b, null);
        JobRegistration rc = service.register(1L, c, c, null);
        JobRegistration rd = service.register(1L, d, d, null);
        JobRegistration re = service.register(1L, e, e, null);
        rc.setRegisteredDateTime(LocalDateTime.now().minusMinutes(3));
        rd.setRegisteredDateTime(LocalDateTime.now().minusMinutes(2));
        re.setRegisteredDateTime(LocalDateTime.now().minusMinutes(1));

        service.update(1L, organizer, j -> j.setMaxParticipants(4));

        assertThat(rc.getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        assertThat(rd.getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
        assertThat(re.getStatus()).isEqualTo(JobRegistrationStatus.WAITLISTED);
    }

    @Test
    void onlyCreatorWithPermissionOrAdminMayEdit() {
        User other = user(20, "Other", 40, Gender.MALE, null, Permission.JOB_CREATE);
        User noPermission = user(21, "NoPerm", 40, Gender.MALE, null);

        assertThat(statusOf(() -> service.update(1L, other, j -> j.setDescription("x")))).isEqualTo(403);
        assertThat(statusOf(() -> service.update(1L, noPermission, j -> j.setDescription("x")))).isEqualTo(403);
        service.update(1L, admin, j -> j.setDescription("by admin"));
        service.update(1L, organizer, j -> j.setDescription("by creator"));
        assertThat(job.getDescription()).isEqualTo("by creator");
    }

    @Test
    void theJobMustEndAfterItStarts() {
        // the fake has no rollback, so restore the job after every rejected change
        LocalDateTime end = job.getJobEndDateTime();

        assertThat(statusOf(() -> service.update(1L, organizer, j -> j.setJobEndDateTime(j.getJobDateTime())))).isEqualTo(400);
        assertThat(statusOf(() -> service.update(1L, organizer, j -> j.setJobEndDateTime(j.getJobDateTime().minusHours(1))))).isEqualTo(400);
        job.setJobEndDateTime(end);

        service.update(1L, organizer, j -> j.setJobEndDateTime(j.getJobDateTime().plusHours(3)));
        assertThat(job.getJobEndDateTime()).isEqualTo(job.getJobDateTime().plusHours(3));
    }

    @Test
    void deadlinesMustNotBeAfterTheJob() {
        // the fake has no rollback, so restore the job after every rejected change
        LocalDateTime registrationDeadline = job.getRegistrationDeadline();
        LocalDateTime cancellationDeadline = job.getCancellationDeadline();

        assertThat(statusOf(() -> service.update(1L, organizer, j -> j.setRegistrationDeadline(j.getJobDateTime().plusHours(1))))).isEqualTo(400);
        job.setRegistrationDeadline(registrationDeadline);

        assertThat(statusOf(() -> service.update(1L, organizer, j -> j.setCancellationDeadline(j.getJobDateTime().plusHours(1))))).isEqualTo(400);
        job.setCancellationDeadline(cancellationDeadline);

        assertThat(statusOf(() -> service.update(1L, organizer, j -> {
            j.setMinAge(20);
            j.setMaxAge(10);
        }))).isEqualTo(400);
        job.setMinAge(null);
        job.setMaxAge(null);

        assertThat(statusOf(() -> service.update(1L, organizer, j -> j.setMaxParticipants(0)))).isEqualTo(400);
    }

    @Test
    void cancellingAJobClosesIt() {
        service.cancelJob(1L, organizer);
        assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(statusOf(() -> service.cancelJob(1L, organizer))).isEqualTo(409);
    }

    // ------------------------------------------------------------ completion

    @Test
    void completeCreatesActivityWithItemsForUsersWithHours() {
        startedJob();
        User a = adult(10), b = adult(11), waitlisted = adult(12);
        JobRegistration ra = registration(a, JobRegistrationStatus.REGISTERED);
        JobRegistration rb = registration(b, JobRegistrationStatus.REGISTERED);
        JobRegistration rw = registration(waitlisted, JobRegistrationStatus.WAITLISTED);
        Round round = new Round();
        round.setId(5L);
        when(roundService.findRoundByDate(job.getJobDateTime())).thenReturn(Optional.of(round));

        Activity activity = service.complete(1L, organizer, List.of(
                new JobService.HoursEntry(10L, 3.5, null),
                new JobService.HoursEntry(11L, 0, null)));

        assertThat(activity.getDescription()).isEqualTo("Moving boxes");
        assertThat(activity.getCreateUser()).isSameAs(organizer);
        assertThat(activity.getEmployer()).isSameAs(job.getEmployer());
        assertThat(activity.getResponsible()).isSameAs(job.getResponsible());
        assertThat(activity.getAccount()).isEqualTo(Account.MYSHARE);
        assertThat(activity.getTransactionType()).isEqualTo(TransactionType.HOURS);
        assertThat(activity.getActivityDateTime()).isEqualTo(job.getJobDateTime());
        assertThat(activity.isRegisteredInApp()).isFalse();

        ArgumentCaptor<ActivityItem> items = ArgumentCaptor.forClass(ActivityItem.class);
        verify(activityItemService, times(1)).save(items.capture());
        assertThat(items.getValue().getUser()).isSameAs(a);
        assertThat(items.getValue().getHours()).isEqualTo(3.5);
        assertThat(items.getValue().getRound()).isSameAs(round);
        assertThat(items.getValue().getActivity()).isSameAs(activity);

        assertThat(ra.getHours()).isEqualTo(3.5);
        assertThat(rb.getHours()).isEqualTo(0);
        assertThat(rw.getStatus()).isEqualTo(JobRegistrationStatus.WAITLISTED);
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getActivity()).isSameAs(activity);
        assertThat(job.getCompletedDateTime()).isNotNull();
    }

    @Test
    void completeTruncatesLongItemDescriptions() {
        startedJob();
        registration(adult(10), JobRegistrationStatus.REGISTERED);
        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));
        job.setDescription("x".repeat(200));

        service.complete(1L, organizer, List.of(new JobService.HoursEntry(10L, 1, null)));

        ArgumentCaptor<ActivityItem> items = ArgumentCaptor.forClass(ActivityItem.class);
        verify(activityItemService).save(items.capture());
        assertThat(items.getValue().getDescription()).hasSize(150);
    }

    @Test
    void completeRequiresHoursForEveryRegisteredUser() {
        startedJob();
        registration(adult(10), JobRegistrationStatus.REGISTERED);
        registration(adult(11), JobRegistrationStatus.REGISTERED);
        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));

        assertThat(statusOf(() -> service.complete(1L, organizer, List.of(new JobService.HoursEntry(10L, 2, null))))).isEqualTo(400);
        verify(activityService, never()).save(any());
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
    }

    @Test
    void completeRejectsUnregisteredDuplicateNegativeAndEmptyHours() {
        startedJob();
        registration(adult(10), JobRegistrationStatus.REGISTERED);
        registration(adult(12), JobRegistrationStatus.WAITLISTED);
        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));

        assertThat(statusOf(() -> service.complete(1L, organizer, List.of(
                new JobService.HoursEntry(10L, 2, null), new JobService.HoursEntry(99L, 2, null))))).isEqualTo(400);
        assertThat(statusOf(() -> service.complete(1L, organizer, List.of(
                new JobService.HoursEntry(10L, 2, null), new JobService.HoursEntry(12L, 2, null))))).isEqualTo(400);
        assertThat(statusOf(() -> service.complete(1L, organizer, List.of(
                new JobService.HoursEntry(10L, 2, null), new JobService.HoursEntry(10L, 2, null))))).isEqualTo(400);
        assertThat(statusOf(() -> service.complete(1L, organizer, List.of(new JobService.HoursEntry(10L, -1, null))))).isEqualTo(400);
        assertThat(statusOf(() -> service.complete(1L, organizer, List.of(new JobService.HoursEntry(10L, 0, null))))).isEqualTo(400);
        verify(activityService, never()).save(any());
    }

    @Test
    void completeNeedsTheResponsibleUserOrAdmin() {
        startedJob();
        User responsible = user(30, "Responsible", 40, Gender.MALE, null);
        job.setResponsible(responsible);
        registration(adult(10), JobRegistrationStatus.REGISTERED);
        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));
        List<JobService.HoursEntry> hours = List.of(new JobService.HoursEntry(10L, 2, null));

        // the creator is not enough
        assertThat(statusOf(() -> service.complete(1L, organizer, hours))).isEqualTo(403);
        assertThat(statusOf(() -> service.complete(1L, adult(31), hours))).isEqualTo(403);
        assertThat(service.complete(1L, responsible, hours)).isNotNull();
    }

    @Test
    void completeNeedsAStartedOpenJobAndARound() {
        registration(adult(10), JobRegistrationStatus.REGISTERED);
        List<JobService.HoursEntry> hours = List.of(new JobService.HoursEntry(10L, 2, null));

        // job is still in the future
        assertThat(statusOf(() -> service.complete(1L, organizer, hours))).isEqualTo(409);

        startedJob();
        when(roundService.findRoundByDate(any())).thenReturn(Optional.empty());
        assertThat(statusOf(() -> service.complete(1L, organizer, hours))).isEqualTo(409);

        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));
        service.complete(1L, organizer, hours);
        assertThat(statusOf(() -> service.complete(1L, organizer, hours))).isEqualTo(409);
    }

    // ------------------------------------------------------------ helpers

    /** Moves the job into the past so hours can be submitted; deadlines stay before it. */
    private void startedJob() {
        job.setJobDateTime(LocalDateTime.now().minusHours(2));
        job.setRegistrationDeadline(LocalDateTime.now().minusDays(3));
        job.setCancellationDeadline(LocalDateTime.now().minusDays(2));
    }

    private JobRegistration registration(User user, JobRegistrationStatus status) {
        JobRegistration r = new JobRegistration();
        r.setId(nextId++);
        r.setJob(job);
        r.setUser(user);
        r.setRegisteredBy(user);
        r.setStatus(status);
        r.setRegisteredDateTime(LocalDateTime.now().minusMinutes(60 - store.size()));
        store.add(r);
        return r;
    }

    private User adult(long id) {
        return user(id, "Adult" + id, 30, Gender.MALE, null);
    }

    private static User user(long id, String name, int age, Gender gender, Long familyId, Permission... permissions) {
        User u = new User();
        u.setId(id);
        u.setFirstname(name);
        u.setLastname("Test");
        u.setGender(gender);
        u.setFamilyId(familyId);
        u.setBirthDate(LocalDate.now().minusYears(age).minusDays(1));
        if (permissions.length > 0) {
            u.setRoles(new HashSet<>(Set.of(AppRole.of("R" + id, "test", Set.of(permissions)))));
        }
        return u;
    }

    private static int statusOf(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    // ---------------------------------------------------------------- repeating jobs

    private Job template(LocalDateTime start) {
        Job t = new Job();
        t.setCreateUser(organizer);
        t.setResponsible(organizer);
        t.setEmployer(job.getEmployer());
        t.setDescription("Cleaning");
        t.setAccount(Account.MYSHARE);
        t.setTransactionType(TransactionType.HOURS);
        t.setJobDateTime(start);
        t.setJobEndDateTime(start.plusHours(2));
        t.setRegistrationDeadline(start.minusDays(1));
        t.setCancellationDeadline(start.minusHours(3));
        return t;
    }

    @Test
    void weeklyJobIsCreatedOnTheSameWeekdayAndTime() {
        LocalDateTime start = LocalDate.now().plusDays(3).atTime(18, 30);
        List<Job> jobs = service.createSeries(template(start), Set.of(), start.toLocalDate().plusWeeks(3));

        assertThat(jobs).hasSize(4);
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            assertThat(j.getJobDateTime()).isEqualTo(start.plusWeeks(i));
            assertThat(j.getJobEndDateTime()).isEqualTo(start.plusWeeks(i).plusHours(2));
            assertThat(j.getRegistrationDeadline()).isEqualTo(start.plusWeeks(i).minusDays(1));
            assertThat(j.getCancellationDeadline()).isEqualTo(start.plusWeeks(i).minusHours(3));
            assertThat(j.getSeriesId()).isNotNull().isEqualTo(jobs.get(0).getSeriesId());
        }
    }

    @Test
    void selectedWeekdaysBetweenTwoDatesOnlyMatchThoseDays() {
        LocalDate monday = LocalDate.now().plusDays(10);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
        LocalDateTime start = monday.atTime(9, 0);
        List<Job> jobs = service.createSeries(template(start), Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), monday.plusDays(13));

        assertThat(jobs).extracting(j -> j.getJobDateTime().getDayOfWeek())
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        assertThat(jobs).allSatisfy(j -> assertThat(j.getJobDateTime().toLocalTime()).isEqualTo(start.toLocalTime()));
    }

    @Test
    void withoutRepeatEndDateASingleJobIsCreated() {
        List<Job> jobs = service.createSeries(template(LocalDateTime.now().plusDays(2)), Set.of(), null);

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getSeriesId()).isNull();
    }

    @Test
    void repeatEndBeforeTheStartOrWithoutMatchingDayIsRejected() {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        assertThatThrownBy(() -> service.createSeries(template(start), Set.of(), start.toLocalDate().minusDays(1)))
                .isInstanceOf(ResponseStatusException.class);
        DayOfWeek other = start.getDayOfWeek().plus(1);
        assertThatThrownBy(() -> service.createSeries(template(start), Set.of(other), start.toLocalDate().plusDays(0)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void tooManyOccurrencesAreRejected() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        assertThatThrownBy(() -> service.createSeries(template(start), Set.of(DayOfWeek.values()), start.toLocalDate().plusDays(200)))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---------------------------------------------------------------- scheduled registration

    @Test
    void registrationBeforeItOpensIsRejectedAndWorksAfter() {
        User a = adult(10);
        job.setRegistrationOpensAt(LocalDateTime.now().plusHours(2));
        assertThatThrownBy(() -> service.register(1L, a, a, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));

        job.setRegistrationOpensAt(LocalDateTime.now().minusMinutes(1));
        assertThat(service.register(1L, a, a, null).getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
    }

    @Test
    void registrationMustOpenBeforeItsDeadline() {
        Job t = template(LocalDateTime.now().plusDays(5));
        t.setRegistrationOpensAt(t.getRegistrationDeadline().plusHours(1));
        assertThatThrownBy(() -> service.create(t)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void seriesShiftsTheOpeningTimeAndOnlyTheFirstJobAnnounces() {
        LocalDateTime start = LocalDate.now().plusDays(10).atTime(18, 0);
        Job t = template(start);
        t.setRegistrationOpensAt(start.minusDays(3));
        t.setSendNotification(true);
        List<Job> jobs = service.createSeries(t, Set.of(), start.toLocalDate().plusWeeks(2));

        assertThat(jobs).hasSize(3);
        for (int i = 0; i < jobs.size(); i++) {
            assertThat(jobs.get(i).getRegistrationOpensAt()).isEqualTo(start.plusWeeks(i).minusDays(3));
            assertThat(jobs.get(i).isSendNotification()).isEqualTo(i == 0);
        }
    }

    @Test
    void dueAnnouncementsAreMarkedSoTheyAreSentOnce() {
        job.setSendNotification(true);
        service.markAnnounced(1L);
        assertThat(job.getAnnouncedDateTime()).isNotNull();
    }

    // ---------------------------------------------------------------- optional deadlines, closed registration

    @Test
    void closedRegistrationRejectsEveryone() {
        User a = adult(10);
        job.setRegistrationClosed(true);
        assertThat(statusOf(() -> service.register(1L, a, a, null))).isEqualTo(409);

        job.setRegistrationClosed(false);
        assertThat(service.register(1L, a, a, null).getStatus()).isEqualTo(JobRegistrationStatus.REGISTERED);
    }

    @Test
    void jobsWithoutDeadlinesStayOpenForRegistrationAndCancellation() {
        User a = adult(10);
        job.setRegistrationDeadline(null);
        job.setCancellationDeadline(null);
        service.register(1L, a, a, null);
        assertThat(service.cancelRegistration(1L, a, a).getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
    }

    @Test
    void cancellationNotAllowedBlocksUsersButNotManagers() {
        User a = adult(10), b = adult(11);
        service.register(1L, a, a, null);
        service.register(1L, b, b, null);
        job.setCancellationAllowed(false);

        assertThat(statusOf(() -> service.cancelRegistration(1L, a, a))).isEqualTo(409);
        assertThat(service.cancelRegistration(1L, b, admin).getStatus()).isEqualTo(JobRegistrationStatus.CANCELLED);
    }

    @Test
    void cancellationDeadlineMayBeTheEndOfTheJob() {
        Job t = template(LocalDateTime.now().plusDays(5));
        t.setCancellationDeadline(t.getJobEndDateTime());
        assertThat(service.create(t)).isNotNull();

        Job late = template(LocalDateTime.now().plusDays(5));
        late.setCancellationDeadline(late.getJobEndDateTime().plusMinutes(1));
        assertThat(statusOf(() -> service.create(late))).isEqualTo(400);
    }

    @Test
    void releasingAnActivityReopensTheCompletedJob() {
        job.setStatus(JobStatus.COMPLETED);
        job.setActivity(new Activity());
        job.setCompletedDateTime(LocalDateTime.now());
        when(repository.findByActivityId(5L)).thenReturn(List.of(job));

        service.releaseActivity(5L);

        assertThat(job.getActivity()).isNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(job.getCompletedDateTime()).isNull();
    }

    @Test
    void usersWhoWereNotRegisteredCanBeAddedWhenCompleting() {
        User registered = adult(10), walkIn = adult(20);
        service.register(1L, registered, registered, null);
        job.setJobDateTime(LocalDateTime.now().minusHours(3));
        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));
        when(userService.findById(20L)).thenReturn(Optional.of(walkIn));
        when(activityItemService.save(any())).thenAnswer(i -> i.getArgument(0));

        service.complete(1L, organizer, List.of(new JobService.HoursEntry(10L, 2, null), new JobService.HoursEntry(20L, 3, null)));

        ArgumentCaptor<ActivityItem> items = ArgumentCaptor.forClass(ActivityItem.class);
        verify(activityItemService, times(2)).save(items.capture());
        assertThat(items.getAllValues()).extracting(i -> i.getUser().getId()).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void anUnknownExtraUserIsRejected() {
        User registered = adult(10);
        service.register(1L, registered, registered, null);
        job.setJobDateTime(LocalDateTime.now().minusHours(3));
        when(roundService.findRoundByDate(any())).thenReturn(Optional.of(new Round()));
        when(userService.findById(99L)).thenReturn(Optional.empty());

        assertThat(statusOf(() -> service.complete(1L, organizer,
                List.of(new JobService.HoursEntry(10L, 2, null), new JobService.HoursEntry(99L, 1, null))))).isEqualTo(400);
    }
}
