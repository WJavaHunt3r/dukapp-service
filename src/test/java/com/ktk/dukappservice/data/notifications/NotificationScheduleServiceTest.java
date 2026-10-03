package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationScheduleServiceTest {

    // 2026-10-06 is a Tuesday
    private static final LocalDateTime TUESDAY_1700 = LocalDateTime.of(2026, 10, 6, 17, 0);

    @Test
    void lastOccurrenceIsThisWeekOnceTheTimeHasPassed() {
        assertThat(NotificationScheduleService.lastOccurrence(DayOfWeek.TUESDAY, LocalTime.of(17, 0), TUESDAY_1700))
                .isEqualTo(TUESDAY_1700);
        assertThat(NotificationScheduleService.lastOccurrence(DayOfWeek.TUESDAY, LocalTime.of(17, 0), TUESDAY_1700.minusMinutes(1)))
                .isEqualTo(TUESDAY_1700.minusWeeks(1));
        assertThat(NotificationScheduleService.lastOccurrence(DayOfWeek.MONDAY, LocalTime.of(8, 30), TUESDAY_1700))
                .isEqualTo(LocalDateTime.of(2026, 10, 5, 8, 30));
    }

    @Test
    void dueAtItsTimeOnlyOnce() {
        NotificationSchedule schedule = schedule(DayOfWeek.TUESDAY, LocalTime.of(17, 0));

        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700.minusMinutes(1))).isFalse();
        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700)).isTrue();

        schedule.setLastSentDateTime(TUESDAY_1700);
        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700.plusMinutes(1))).isFalse();
        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700.plusWeeks(1))).isTrue();
    }

    @Test
    void missedSendIsCaughtUpWithinAnHourButNotLater() {
        NotificationSchedule schedule = schedule(DayOfWeek.TUESDAY, LocalTime.of(17, 0));
        schedule.setLastSentDateTime(TUESDAY_1700.minusWeeks(1));

        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700.plusMinutes(59))).isTrue();
        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700.plusMinutes(61))).isFalse();
    }

    @Test
    void newScheduleForADayThatAlreadyPassedWaitsForNextWeek() {
        NotificationSchedule schedule = schedule(DayOfWeek.MONDAY, LocalTime.of(9, 0));

        assertThat(NotificationScheduleService.isDue(schedule, TUESDAY_1700)).isFalse();
    }

    private static NotificationSchedule schedule(DayOfWeek day, LocalTime time) {
        NotificationSchedule schedule = new NotificationSchedule();
        schedule.setType(NotificationType.WEEKLY);
        schedule.setDayOfWeek(day);
        schedule.setTime(time);
        schedule.setActive(true);
        return schedule;
    }
}
