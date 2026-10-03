package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/** A Firebase Cloud Messaging registration token of one of the user's devices / browsers. */
@Getter
@Setter
@Entity
@Table(name = "DEVICE_TOKENS", indexes = {
        @Index(name = "idx_device_token", columnList = "TOKEN", unique = true)
})
@FieldNameConstants
public class DeviceToken extends BaseEntity<DeviceToken, Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    @NotNull
    private User user;

    @Column(name = "TOKEN", length = 512, nullable = false)
    private String token;

    /** Free text from the client, e.g. "web", "android", "ios". */
    @Column(name = "PLATFORM", length = 20)
    private String platform;

    @Column(name = "CREATE_DATE_TIME")
    private LocalDateTime createDateTime;

    @Column(name = "LAST_SEEN_DATE_TIME")
    private LocalDateTime lastSeenDateTime;
}
