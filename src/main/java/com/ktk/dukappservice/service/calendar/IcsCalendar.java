package com.ktk.dukappservice.service.calendar;

import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.enums.JobStatus;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds iCalendar (RFC 5545) text for jobs, so they can be imported into Google, Apple or Outlook calendars or
 * subscribed to. Times are written in UTC, which every calendar app understands without a time zone definition.
 */
public final class IcsCalendar {
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    /** Jobs without an end time are shown with this length. */
    static final Duration DEFAULT_LENGTH = Duration.ofHours(2);

    private IcsCalendar() {
    }

    /** A calendar with one event per job. {@code zone}: the time zone the job times are meant in. */
    public static String calendar(String name, List<Job> jobs, ZoneId zone, String webUrl, LocalDateTime now) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//DukApp//Jobs//HU");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "X-WR-CALNAME:" + escape(name));
        // Hints for subscribed calendars on how often to look for changes
        line(out, "X-PUBLISHED-TTL:PT1H");
        line(out, "REFRESH-INTERVAL;VALUE=DURATION:PT1H");
        for (Job job : jobs) {
            event(out, job, zone, webUrl, now);
        }
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    private static void event(StringBuilder out, Job job, ZoneId zone, String webUrl, LocalDateTime now) {
        LocalDateTime end = job.getJobEndDateTime() != null ? job.getJobEndDateTime() : job.getJobDateTime().plus(DEFAULT_LENGTH);
        String url = webUrl + "/jobs/" + job.getId();
        StringBuilder description = new StringBuilder();
        if (job.getComment() != null && !job.getComment().isBlank()) {
            description.append(job.getComment().trim()).append("\n\n");
        }
        if (job.getEmployer() != null) {
            description.append(job.getEmployer().getFullName()).append("\n");
        }
        description.append(url);

        line(out, "BEGIN:VEVENT");
        line(out, "UID:job-" + job.getId() + "@dukapp");
        line(out, "DTSTAMP:" + utc(now, zone));
        line(out, "DTSTART:" + utc(job.getJobDateTime(), zone));
        line(out, "DTEND:" + utc(end, zone));
        line(out, "SUMMARY:" + escape(job.getDescription()));
        line(out, "DESCRIPTION:" + escape(description.toString()));
        line(out, "URL:" + url);
        line(out, "STATUS:" + (job.getStatus() == JobStatus.CANCELLED ? "CANCELLED" : "CONFIRMED"));
        line(out, "END:VEVENT");
    }

    static String utc(LocalDateTime time, ZoneId zone) {
        return UTC.format(time.atZone(zone).withZoneSameInstant(ZoneOffset.UTC));
    }

    /** Escapes text values: backslash, semicolon, comma and line breaks. */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    private static void line(StringBuilder out, String content) {
        out.append(fold(content)).append("\r\n");
    }

    /** Lines are at most 75 bytes long; longer ones continue on the next line after a space. */
    static String fold(String content) {
        StringBuilder out = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            String ch = new String(Character.toChars(codePoint));
            int size = ch.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + size > 75) {
                out.append("\r\n ");
                bytes = 1;
            }
            out.append(ch);
            bytes += size;
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }
}
