package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.service.calendar.IcsCalendar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * A user's jobs as an iCalendar feed to subscribe to in Google, Apple or Outlook calendar. Calendar apps can't send a
 * login, so the link carries a secret token of the user instead ({@code /api/calendar/<token>.ics}, public). Anyone
 * with the link can read that user's jobs; the user can replace it (see UserController).
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {
    /** The feed starts this long before today, so recent jobs stay visible. */
    private static final int DAYS_BACK = 30;

    private final UserService userService;
    private final JobService jobService;

    @Value("${app.timezone:Europe/Budapest}")
    private String timezone;

    @Value("${app.webUrl:https://dukapp.bcc-ktk.org}")
    private String webUrl;

    public CalendarController(UserService userService, JobService jobService) {
        this.userService = userService;
        this.jobService = jobService;
    }

    @GetMapping("/{token:.+}")
    public ResponseEntity<?> feed(@PathVariable String token) {
        String clean = token.endsWith(".ics") ? token.substring(0, token.length() - 4) : token;
        User user = userService.findByCalendarToken(clean).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Unknown calendar link");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = jobService.findRegisteredJobs(user.getId(), now.minusDays(DAYS_BACK).toLocalDate().atStartOfDay());
        String ics = IcsCalendar.calendar("DukApp munkák", jobs, ZoneId.of(timezone), webUrl, now);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }
}
