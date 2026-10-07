package com.ktk.dukappservice.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A user with jobs that are over but still wait for their hours. */
public record OverdueJobsDto(Long userId, String userName, List<Job> jobs) {
    public record Job(Long id, String description, LocalDateTime jobDateTime, LocalDateTime jobEndDateTime) {
    }
}
