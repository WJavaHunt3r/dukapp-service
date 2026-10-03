package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobCounts;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.JobCompleteDto;
import com.ktk.dukappservice.dto.JobDto;
import com.ktk.dukappservice.dto.JobRegisterDto;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.JobStatus;
import com.ktk.dukappservice.mapper.ActivityMapper;
import com.ktk.dukappservice.mapper.JobMapper;
import com.ktk.dukappservice.service.notifications.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Jobs people register for before they happen. Rules (deadlines, capacity, waitlist, age/gender, who may act for whom)
 * live in {@link JobService}; it reports problems as {@link ResponseStatusException}, which this controller turns
 * into the plain-text error bodies the other controllers use.
 */
@RestController
@RequestMapping("/api/job")
public class JobController {

    private final JobService jobService;
    private final JobMapper jobMapper;
    private final UserService userService;
    private final ActivityMapper activityMapper;
    private final PushNotificationService pushNotificationService;

    public JobController(JobService jobService, JobMapper jobMapper, UserService userService, ActivityMapper activityMapper,
                         PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
        this.jobService = jobService;
        this.jobMapper = jobMapper;
        this.userService = userService;
        this.activityMapper = activityMapper;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    @GetMapping()
    public ResponseEntity<?> getJobs(@RequestParam(value = "status", required = false) JobStatus status,
                                     @RequestParam(value = "responsibleId", required = false) Long responsibleId,
                                     @RequestParam(value = "registeredUserId", required = false) Long registeredUserId,
                                     @RequestParam(value = "openOnly", required = false, defaultValue = "false") boolean openOnly,
                                     @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                     @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                     @RequestParam(value = "searchText", required = false) String searchText,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     @PageableDefault(sort = "jobDateTime") Pageable pageable) {
        User user = userService.getCurrentUser(userDetails);
        Page<Job> jobs = jobService.fetchByQuery(status, responsibleId, registeredUserId, openOnly, dateFrom, dateTo, searchText, pageable);
        List<Long> ids = jobs.getContent().stream().map(Job::getId).toList();
        Map<Long, JobCounts> counts = jobService.countsFor(ids);
        Map<Long, JobRegistrationStatus> mine = jobService.statusesForUser(ids, user.getId());
        return ResponseEntity.ok(jobs.map(j -> jobMapper.toDto(j, counts.getOrDefault(j.getId(), JobCounts.NONE), mine.get(j.getId()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        Job job = findJob(id);
        return ResponseEntity.ok(toDto(job, user));
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('JOB_CREATE')")
    public ResponseEntity<?> postJob(@Valid @RequestBody JobDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        User employer = findUser(dto.getEmployerId());
        User responsible = findUser(dto.getResponsibleId());
        Job job = jobMapper.dtoToEntity(dto, new Job());
        job.setCreateUser(user);
        job.setEmployer(employer);
        job.setResponsible(responsible);
        Job created = jobService.create(job);
        pushNotificationService.jobCreated(created.getId(), user.getId());
        return ResponseEntity.ok(toDto(created, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> putJob(@Valid @RequestBody JobDto dto, @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        if (!Objects.equals(dto.getId(), id)) {
            return ResponseEntity.status(400).body("Invalid jobId");
        }
        User employer = findUser(dto.getEmployerId());
        User responsible = findUser(dto.getResponsibleId());
        Job job = jobService.update(id, user, j -> {
            jobMapper.dtoToEntity(dto, j);
            j.setEmployer(employer);
            j.setResponsible(responsible);
        });
        return ResponseEntity.ok(toDto(job, user));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelJob(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        Job cancelled = jobService.cancelJob(id, user);
        pushNotificationService.jobCancelled(id, user.getId());
        return ResponseEntity.ok(toDto(cancelled, user));
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<?> getRegistrations(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        Job job = findJob(id);
        boolean organizer = jobService.isOrganizer(user, job);
        List<Object> result = new ArrayList<>();
        int waitlistPosition = 0;
        for (JobRegistration r : jobService.findActiveRegistrations(id)) {
            boolean includeComment = organizer
                    || r.getUser().getId().equals(user.getId())
                    || r.getRegisteredBy().getId().equals(user.getId())
                    || JobService.isParentOf(user, r.getUser());
            Integer position = r.getStatus() == JobRegistrationStatus.WAITLISTED ? ++waitlistPosition : null;
            result.add(jobMapper.registrationToDto(r, includeComment, position));
        }
        return ResponseEntity.ok(result);
    }

    /** Registers the current user, or {@code userId} (their child, or anyone with JOB_MANAGE_ALL). */
    @PostMapping("/{id}/register")
    public ResponseEntity<?> register(@PathVariable Long id, @Valid @RequestBody(required = false) JobRegisterDto body,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User actor = userService.getCurrentUser(userDetails);
        User target = resolveTarget(body == null ? null : body.getUserId(), actor);
        JobRegistration registration = jobService.register(id, target, actor, body == null ? null : body.getComment());
        pushNotificationService.registeredByOther(id, target, actor, registration.getStatus());
        return ResponseEntity.ok(jobMapper.registrationToDto(registration, true, null));
    }

    @PutMapping("/{id}/register")
    public ResponseEntity<?> updateRegistration(@PathVariable Long id, @Valid @RequestBody JobRegisterDto body,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User actor = userService.getCurrentUser(userDetails);
        User target = resolveTarget(body.getUserId(), actor);
        return ResponseEntity.ok(jobMapper.registrationToDto(jobService.updateComment(id, target, actor, body.getComment()), true, null));
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<?> cancelRegistration(@PathVariable Long id,
                                                @RequestParam(value = "userId", required = false) Long userId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User actor = userService.getCurrentUser(userDetails);
        User target = resolveTarget(userId, actor);
        return ResponseEntity.ok(jobMapper.registrationToDto(jobService.cancelRegistration(id, target, actor), true, null));
    }

    /**
     * Submits the hours of every registered user and closes the job. Returns the created activity, which is then
     * registered through the existing {@code POST /api/activity/{id}/register}.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeJob(@PathVariable Long id, @Valid @RequestBody JobCompleteDto body,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User actor = userService.getCurrentUser(userDetails);
        List<JobService.HoursEntry> entries = body.getItems().stream()
                .map(i -> new JobService.HoursEntry(i.getUserId(), i.getHours(), i.getDescription()))
                .toList();
        Activity activity = jobService.complete(id, actor, entries);
        return ResponseEntity.ok(activityMapper.entityToDto(activity));
    }

    private JobDto toDto(Job job, User user) {
        return jobMapper.toDto(job, jobService.countsFor(job.getId()),
                jobService.statusesForUser(List.of(job.getId()), user.getId()).get(job.getId()));
    }

    private Job findJob(Long id) {
        return jobService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id: " + id));
    }

    private User findUser(Long id) {
        return userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user with id:" + id));
    }

    private User resolveTarget(Long userId, User actor) {
        if (userId == null || userId.equals(actor.getId())) {
            return actor;
        }
        return userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user with id: " + userId));
    }
}
