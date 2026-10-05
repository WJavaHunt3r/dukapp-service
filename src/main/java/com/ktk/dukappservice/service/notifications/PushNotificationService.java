package com.ktk.dukappservice.service.notifications;

import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobchat.JobChatService;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.notifications.*;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.NotificationType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Decides who gets which push notification. The event methods return immediately and send in the background,
 * so call them after the change is committed (from the controller, after the service call returned).
 * Users never get notified about their own actions.
 */
@Service
public class PushNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationService.class);
    /** JPQL "IN" needs a non-empty list even when the role filter is switched off. */
    private static final List<Long> NO_ROLE = List.of(-1L);

    private final PushService pushService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final GeneralNotificationRepository generalNotificationRepository;
    private final JobService jobService;
    private final JobChatService jobChatService;
    private final ExecutorService executor;

    @Value("${app.users.baseChurch}")
    private Long baseChurchId;

    @Autowired
    public PushNotificationService(PushService pushService, DeviceTokenRepository deviceTokenRepository,
                                   GeneralNotificationRepository generalNotificationRepository, JobService jobService,
                                   JobChatService jobChatService) {
        this(pushService, deviceTokenRepository, generalNotificationRepository, jobService, jobChatService, Executors.newVirtualThreadPerTaskExecutor());
    }

    PushNotificationService(PushService pushService, DeviceTokenRepository deviceTokenRepository,
                            GeneralNotificationRepository generalNotificationRepository, JobService jobService,
                            JobChatService jobChatService, ExecutorService executor) {
        this.pushService = pushService;
        this.deviceTokenRepository = deviceTokenRepository;
        this.generalNotificationRepository = generalNotificationRepository;
        this.jobService = jobService;
        this.jobChatService = jobChatService;
        this.executor = executor;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    /**
     * Sends the "new job" notification of every job that is due: wanted, not announced yet, and registration has
     * opened (jobs without an opening time are due at once). Marks a job before sending so it is never sent twice.
     * Called when a job is created and every minute by the scheduler.
     */
    public void announceDue() {
        for (Job job : jobService.findDueAnnouncements(java.time.LocalDateTime.now())) {
            jobService.markAnnounced(job.getId());
            jobCreated(job.getId(), job.getCreateUser().getId());
        }
    }

    /** New job: eligible users of the base church (app.users.baseChurch), except its creator. */
    public void jobCreated(Long jobId, Long actorId) {
        inBackground("job created " + jobId, () -> jobService.findById(jobId).ifPresent(job -> {
            List<DeviceToken> devices = deviceTokenRepository.findForChurch(baseChurchId, NotificationType.JOB_NEW).stream()
                    .filter(d -> !d.getUser().getId().equals(actorId) && jobService.isEligible(job, d.getUser()))
                    .toList();
            NotificationTexts.Text text = NotificationTexts.jobNew(job);
            pushService.send(devices, NotificationType.JOB_NEW, text.title(), text.body(), jobData(job));
        }));
    }

    /** New chat message: everyone taking part except the sender and who muted the chat (see {@link JobChatService}). */
    public void chatMessage(Long jobId, User sender, String text) {
        inBackground("chat message " + jobId, () -> jobService.findById(jobId).ifPresent(job -> {
            Set<Long> recipients = jobChatService.recipientIds(job, sender.getId());
            if (recipients.isEmpty()) {
                return;
            }
            NotificationTexts.Text notification = NotificationTexts.chatMessage(job, sender, text);
            pushService.send(deviceTokenRepository.findForUsers(recipients, NotificationType.JOB_CHAT_MESSAGE),
                    NotificationType.JOB_CHAT_MESSAGE, notification.title(), notification.body(), jobData(job));
        }));
    }

    /** Reminders older than this are ignored, so jobs that were forgotten long ago aren't announced out of the blue. */
    static final int CLOSE_REMINDER_MAX_AGE_DAYS = 7;
    /** The second reminder goes out at this time on the day after the job. */
    static final java.time.LocalTime SECOND_REMINDER_TIME = java.time.LocalTime.of(9, 0);

    /**
     * Reminds the responsible user of jobs that are over but not closed: one hour after the end, and again on the
     * next day. Marks each reminder before sending so it is never repeated. Runs every minute on the scheduler.
     */
    public void sendCloseReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Job job : jobService.findOverdueOpenJobs(now.minusHours(1))) {
            LocalDateTime end = JobService.endOf(job);
            if (end.isBefore(now.minusDays(CLOSE_REMINDER_MAX_AGE_DAYS))) {
                continue;
            }
            LocalDateTime first = job.getCloseReminderSentDateTime();
            boolean second;
            if (first == null) {
                second = false;
            } else if (job.getCloseReminder2SentDateTime() == null
                    && !now.isBefore(end.toLocalDate().plusDays(1).atTime(SECOND_REMINDER_TIME))
                    && !now.isBefore(first.plusHours(1))) {
                second = true;
            } else {
                continue;
            }
            jobService.markCloseReminderSent(job.getId(), second);
            NotificationTexts.Text text = NotificationTexts.jobNotClosed(job, second);
            pushService.send(deviceTokenRepository.findForUsers(List.of(job.getResponsible().getId()), NotificationType.JOB_NOT_CLOSED),
                    NotificationType.JOB_NOT_CLOSED, text.title(), text.body(), jobData(job));
        }
    }

    /** Over-and-still-open jobs grouped by the responsible user (the one who can submit the hours). */
    public Map<User, List<Job>> overdueJobsByResponsible() {
        Map<User, List<Job>> result = new LinkedHashMap<>();
        for (Job job : jobService.findOverdueOpenJobs(LocalDateTime.now())) {
            result.computeIfAbsent(job.getResponsible(), u -> new ArrayList<>()).add(job);
        }
        return result;
    }

    /**
     * Admin action: tells users that jobs of theirs still wait for their hours. {@code userIds} empty = everyone who
     * has such jobs. One notification per user, however many jobs.
     */
    public PushService.Result remindOverdue(Set<Long> userIds) {
        int users = 0, devices = 0, delivered = 0, failed = 0;
        for (Map.Entry<User, List<Job>> entry : overdueJobsByResponsible().entrySet()) {
            if (!userIds.isEmpty() && !userIds.contains(entry.getKey().getId())) {
                continue;
            }
            NotificationTexts.Text text = NotificationTexts.jobsNotClosed(entry.getValue());
            Map<String, String> data = entry.getValue().size() == 1 ? jobData(entry.getValue().getFirst()) : Map.of();
            PushService.Result result = pushService.send(
                    deviceTokenRepository.findForUsers(List.of(entry.getKey().getId()), NotificationType.JOB_NOT_CLOSED),
                    NotificationType.JOB_NOT_CLOSED, text.title(), text.body(), data);
            users += result.users();
            devices += result.devices();
            delivered += result.delivered();
            failed += result.failed();
        }
        return new PushService.Result(users, devices, delivered, failed);
    }

    /** Cancelled job: everyone registered or waitlisted, except whoever cancelled it. */
    public void jobCancelled(Long jobId, Long actorId) {
        inBackground("job cancelled " + jobId, () -> jobService.findById(jobId).ifPresent(job -> {
            Set<Long> userIds = new HashSet<>();
            for (JobRegistration registration : jobService.findActiveRegistrations(jobId)) {
                userIds.add(registration.getUser().getId());
            }
            userIds.remove(actorId);
            if (userIds.isEmpty()) {
                return;
            }
            NotificationTexts.Text text = NotificationTexts.jobCancelled(job);
            pushService.send(deviceTokenRepository.findForUsers(userIds, NotificationType.JOB_CANCELLED),
                    NotificationType.JOB_CANCELLED, text.title(), text.body(), jobData(job));
        }));
    }

    /** Someone registered {@code target} for a job (only when that someone isn't the target). */
    public void registeredByOther(Long jobId, User target, User actor, JobRegistrationStatus status) {
        if (target.getId().equals(actor.getId())) {
            return;
        }
        Long targetId = target.getId();
        inBackground("registered by other " + jobId, () -> jobService.findById(jobId).ifPresent(job -> {
            NotificationTexts.Text text = NotificationTexts.registeredByOther(job, actor, status);
            pushService.send(deviceTokenRepository.findForUsers(List.of(targetId), NotificationType.JOB_REGISTERED_BY_OTHER),
                    NotificationType.JOB_REGISTERED_BY_OTHER, text.title(), text.body(), jobData(job));
        }));
    }

    /**
     * Manually created transaction items, as user id -> descriptions of that user's new items. One notification per
     * user; the creator isn't notified about items for themselves.
     */
    public void transactionsCreated(Map<Long, List<String>> descriptionsByUser, Long actorId) {
        Map<Long, List<String>> recipients = new HashMap<>(descriptionsByUser);
        recipients.remove(actorId);
        if (recipients.isEmpty()) {
            return;
        }
        inBackground("transactions created", () -> {
            Map<Long, List<DeviceToken>> devicesByUser = new HashMap<>();
            for (DeviceToken device : deviceTokenRepository.findForUsers(recipients.keySet(), NotificationType.TRANSACTION_CREATED)) {
                devicesByUser.computeIfAbsent(device.getUser().getId(), id -> new ArrayList<>()).add(device);
            }
            devicesByUser.forEach((userId, devices) -> {
                NotificationTexts.Text text = NotificationTexts.transactionsCreated(recipients.get(userId));
                pushService.send(devices, NotificationType.TRANSACTION_CREATED, text.title(), text.body(), Map.of());
            });
        });
    }

    /** Sends an admin's one-off notification right away and stores it in the history. */
    public GeneralNotification sendGeneral(String title, String body, Set<Long> roleIds, User sender) {
        PushService.Result result = pushService.send(findForAudience(NotificationType.GENERAL, roleIds),
                NotificationType.GENERAL, title, body, Map.of());
        GeneralNotification notification = new GeneralNotification();
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRoleIds(new HashSet<>(roleIds));
        notification.setSentBy(sender);
        notification.setSentDateTime(LocalDateTime.now());
        notification.setRecipientUsers(result.users());
        notification.setDelivered(result.delivered());
        notification.setFailed(result.failed());
        return generalNotificationRepository.save(notification);
    }

    /** Sends one occurrence of an admin-defined weekly notification (runs on the scheduler thread). */
    public PushService.Result sendWeekly(NotificationSchedule schedule) {
        Map<String, String> data = Map.of("scheduleId", String.valueOf(schedule.getId()));
        return pushService.send(findForAudience(NotificationType.WEEKLY, schedule.getRoleIds()),
                NotificationType.WEEKLY, schedule.getTitle(), schedule.getBody(), data);
    }

    private List<DeviceToken> findForAudience(NotificationType type, Set<Long> roleIds) {
        boolean allUsers = roleIds == null || roleIds.isEmpty();
        return deviceTokenRepository.findForBroadcast(type, allUsers, allUsers ? NO_ROLE : roleIds);
    }

    private static Map<String, String> jobData(Job job) {
        return Map.of("jobId", String.valueOf(job.getId()));
    }

    private void inBackground(String what, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.error("Failed to send push notifications for {}", what, e);
            }
        });
    }
}
