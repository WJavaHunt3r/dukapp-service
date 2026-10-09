package com.ktk.dukappservice.service.calendar;

import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IcsCalendarTest {
    private static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

    private Job job(LocalDateTime start, LocalDateTime end) {
        User employer = new User();
        employer.setFirstname("Anna");
        employer.setLastname("Kis");
        Job job = new Job();
        job.setId(12L);
        job.setDescription("Kerti munka, takarítás; sok");
        job.setEmployer(employer);
        job.setJobDateTime(start);
        job.setJobEndDateTime(end);
        job.setStatus(JobStatus.OPEN);
        return job;
    }

    @Test
    void timesAreConvertedToUtcWithSummerAndWinterOffsets() {
        assertThat(IcsCalendar.utc(LocalDateTime.of(2026, 7, 10, 9, 0), BUDAPEST)).isEqualTo("20260710T070000Z");
        assertThat(IcsCalendar.utc(LocalDateTime.of(2026, 12, 10, 9, 0), BUDAPEST)).isEqualTo("20261210T080000Z");
    }

    @Test
    void textIsEscapedAndLongLinesAreFolded() {
        assertThat(IcsCalendar.escape("a,b;c\\d\ne")).isEqualTo("a\\,b\\;c\\\\d\\ne");

        String folded = IcsCalendar.fold("DESCRIPTION:" + "é".repeat(80));
        for (String part : folded.split("\r\n")) {
            assertThat(part.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
    }

    @Test
    void anEventHasTheJobsTimesTitleAndLink() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 10, 9, 0);
        String ics = IcsCalendar.calendar("DukApp", List.of(job(start, start.plusHours(3))), BUDAPEST, "https://dukapp.example", NOW);

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("UID:job-12@dukapp\r\n", "DTSTART:20261010T070000Z\r\n", "DTEND:20261010T100000Z\r\n",
                "SUMMARY:Kerti munka\\, takarítás\\; sok\r\n", "URL:https://dukapp.example/jobs/12\r\n", "STATUS:CONFIRMED\r\n");
    }

    @Test
    void jobsWithoutAnEndGetTwoHoursAndCancelledOnesAreMarked() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 10, 9, 0);
        Job cancelled = job(start, null);
        cancelled.setStatus(JobStatus.CANCELLED);

        String ics = IcsCalendar.calendar("DukApp", List.of(cancelled), BUDAPEST, "https://dukapp.example", NOW);

        assertThat(ics).contains("DTEND:20261010T090000Z\r\n", "STATUS:CANCELLED\r\n");
    }
}
