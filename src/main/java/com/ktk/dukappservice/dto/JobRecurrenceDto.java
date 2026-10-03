package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Makes a new job repeat at the same time of day: one job is created for every date from the job's own date up to
 * {@code repeatUntil} (inclusive) whose weekday is in {@code daysOfWeek}. A weekly job is a single weekday; empty
 * means the weekday of the job's own date.
 */
@Getter
@Setter
@NoArgsConstructor
public class JobRecurrenceDto {
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    private LocalDate repeatUntil;
}
