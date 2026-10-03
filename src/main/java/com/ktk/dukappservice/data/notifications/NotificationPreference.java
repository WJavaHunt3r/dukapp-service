package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** A user's choice for one notification type. Without a row the type is enabled. */
@Getter
@Setter
@Entity
@Table(name = "NOTIFICATION_PREFERENCES", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_preference", columnNames = {"USER_ID", "TYPE"})
})
@FieldNameConstants
public class NotificationPreference extends BaseEntity<NotificationPreference, Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    @NotNull
    private User user;

    @Column(name = "TYPE", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;
}
