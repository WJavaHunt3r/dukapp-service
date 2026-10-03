package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Something sent every week at {@link #dayOfWeek} {@link #time} (server time): an admin-defined push
 * ({@link NotificationType#WEEKLY}) or the "on track" status e-mail ({@link NotificationType#ON_TRACK_EMAIL},
 * exactly one row, created on startup; title, body and roles are not used for it).
 */
@Getter
@Setter
@Entity
@Table(name = "NOTIFICATION_SCHEDULES")
@FieldNameConstants
public class NotificationSchedule extends BaseEntity<NotificationSchedule, Long> {

    @Column(name = "TYPE", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private NotificationType type;

    @Column(name = "TITLE", length = 100)
    private String title;

    @Column(name = "BODY", length = 500)
    private String body;

    @Column(name = "DAY_OF_WEEK", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private DayOfWeek dayOfWeek;

    @Column(name = "SEND_TIME", nullable = false)
    @NotNull
    private LocalTime time;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    /** Ids of the roles whose users receive it; empty = all users. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "NOTIFICATION_SCHEDULE_ROLES", joinColumns = @JoinColumn(name = "SCHEDULE_ID"))
    @Column(name = "ROLE_ID")
    private Set<Long> roleIds = new HashSet<>();

    @Column(name = "LAST_SENT_DATE_TIME")
    private LocalDateTime lastSentDateTime;
}
