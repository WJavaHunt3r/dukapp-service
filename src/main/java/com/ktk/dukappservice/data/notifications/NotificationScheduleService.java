package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.enums.NotificationType;
import com.ktk.dukappservice.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class NotificationScheduleService extends BaseService<NotificationSchedule, Long> {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationScheduleService.class);

    /** A missed send (e.g. the server was down) is caught up only within this window, never days later. */
    static final Duration CATCH_UP_WINDOW = Duration.ofHours(1);

    private final NotificationScheduleRepository repository;

    public NotificationScheduleService(NotificationScheduleRepository repository) {
        this.repository = repository;
    }

    /** Creates the "on track" e-mail schedule with its previous hard-coded time (Tuesday 17:00) if it doesn't exist. */
    public void ensureOnTrackEmailSchedule() {
        if (repository.findFirstByType(NotificationType.ON_TRACK_EMAIL).isEmpty()) {
            NotificationSchedule schedule = new NotificationSchedule();
            schedule.setType(NotificationType.ON_TRACK_EMAIL);
            schedule.setDayOfWeek(DayOfWeek.TUESDAY);
            schedule.setTime(LocalTime.of(17, 0));
            schedule.setActive(true);
            repository.save(schedule);
            LOG.info("Created the on-track e-mail schedule (Tuesday 17:00)");
        }
    }

    public List<NotificationSchedule> findDue(LocalDateTime now) {
        return repository.findByActiveTrue().stream().filter(s -> isDue(s, now)).toList();
    }

    /** Due when its most recent weekly occurrence is at most {@link #CATCH_UP_WINDOW} ago and wasn't sent yet. */
    static boolean isDue(NotificationSchedule schedule, LocalDateTime now) {
        LocalDateTime occurrence = lastOccurrence(schedule.getDayOfWeek(), schedule.getTime(), now);
        if (Duration.between(occurrence, now).compareTo(CATCH_UP_WINDOW) > 0) {
            return false;
        }
        return schedule.getLastSentDateTime() == null || schedule.getLastSentDateTime().isBefore(occurrence);
    }

    static LocalDateTime lastOccurrence(DayOfWeek day, LocalTime time, LocalDateTime now) {
        LocalDateTime occurrence = now.with(TemporalAdjusters.previousOrSame(day))
                .with(time.truncatedTo(ChronoUnit.MINUTES));
        return occurrence.isAfter(now) ? occurrence.minusWeeks(1) : occurrence;
    }

    @Override
    protected JpaRepository<NotificationSchedule, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<NotificationSchedule> getEntityClass() {
        return NotificationSchedule.class;
    }

    @Override
    public NotificationSchedule createEntity() {
        return new NotificationSchedule();
    }
}
