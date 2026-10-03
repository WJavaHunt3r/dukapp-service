package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
public class NotificationScheduleDto {
    private Long id;

    /** WEEKLY for admin push notifications; ON_TRACK_EMAIL for the e-mail schedule (read-only, can't be created). */
    private NotificationType type;

    /** Required for WEEKLY. */
    @Size(max = 100)
    private String title;

    /** Required for WEEKLY. */
    @Size(max = 500)
    private String body;

    @NotNull
    private DayOfWeek dayOfWeek;

    /** "HH:mm", server time. */
    @NotNull
    private LocalTime time;

    private boolean active;

    /** Target role ids; empty = all users. Not used for ON_TRACK_EMAIL. */
    private Set<Long> roleIds = new HashSet<>();

    private LocalDateTime lastSentDateTime;
}
