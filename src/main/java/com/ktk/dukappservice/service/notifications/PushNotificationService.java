package com.ktk.dukappservice.service.notifications;

import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
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
    private final ExecutorService executor;

    @Value("${app.users.baseChurch}")
    private Long baseChurchId;

    @Autowired
    public PushNotificationService(PushService pushService, DeviceTokenRepository deviceTokenRepository,
                                   GeneralNotificationRepository generalNotificationRepository, JobService jobService) {
        this(pushService, deviceTokenRepository, generalNotificationRepository, jobService, Executors.newVirtualThreadPerTaskExecutor());
    }

    PushNotificationService(PushService pushService, DeviceTokenRepository deviceTokenRepository,
                            GeneralNotificationRepository generalNotificationRepository, JobService jobService,
                            ExecutorService executor) {
        this.pushService = pushService;
        this.deviceTokenRepository = deviceTokenRepository;
        this.generalNotificationRepository = generalNotificationRepository;
        this.jobService = jobService;
        this.executor = executor;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
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
