package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** History of the one-off notifications sent by admins. */
@Getter
@Setter
@Entity
@Table(name = "GENERAL_NOTIFICATIONS")
@FieldNameConstants
public class GeneralNotification extends BaseEntity<GeneralNotification, Long> {

    @Column(name = "TITLE", length = 100, nullable = false)
    private String title;

    @Column(name = "BODY", length = 500, nullable = false)
    private String body;

    /** Ids of the targeted roles; empty = all users. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "GENERAL_NOTIFICATION_ROLES", joinColumns = @JoinColumn(name = "NOTIFICATION_ID"))
    @Column(name = "ROLE_ID")
    private Set<Long> roleIds = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "SENT_BY")
    private User sentBy;

    @Column(name = "SENT_DATE_TIME")
    private LocalDateTime sentDateTime;

    @Column(name = "RECIPIENT_USERS")
    private int recipientUsers;

    @Column(name = "DELIVERED")
    private int delivered;

    @Column(name = "FAILED")
    private int failed;
}
