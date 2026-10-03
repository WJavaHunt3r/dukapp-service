package com.ktk.dukappservice.service.notifications;

import com.ktk.dukappservice.data.notifications.NotificationSchedule;
import com.ktk.dukappservice.data.notifications.NotificationScheduleService;
import com.ktk.dukappservice.enums.NotificationType;
import com.ktk.dukappservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Runs the weekly schedules (admin push notifications and the "on track" e-mail) at their configured time. */
@Component
public class NotificationScheduler implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationScheduleService scheduleService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    public NotificationScheduler(NotificationScheduleService scheduleService, PushNotificationService pushNotificationService,
                                 NotificationService notificationService) {
        this.scheduleService = scheduleService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        scheduleService.ensureOnTrackEmailSchedule();
    }

    @Scheduled(cron = "0 * * * * *")
    public void runDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        for (NotificationSchedule schedule : scheduleService.findDue(now)) {
            // Mark first, so a slow or failing send is never repeated
            schedule.setLastSentDateTime(now);
            scheduleService.save(schedule);
            try {
                if (schedule.getType() == NotificationType.ON_TRACK_EMAIL) {
                    notificationService.sendOnTrackEmails();
                } else {
                    pushNotificationService.sendWeekly(schedule);
                }
            } catch (Exception e) {
                LOG.error("Scheduled notification {} ({}) failed", schedule.getId(), schedule.getType(), e);
            }
        }
    }
}
