package com.ktk.dukappservice.data.auditlog;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "AUDIT_LOG", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "TIMESTAMP"),
        @Index(name = "idx_audit_entity", columnList = "ENTITY_TYPE, ENTITY_ID"),
        @Index(name = "idx_audit_username", columnList = "USERNAME"),
        @Index(name = "idx_audit_action", columnList = "ACTION")
})
@FieldNameConstants
public class AuditLog extends BaseEntity<AuditLog, Long> {

    @Column(name = "TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ACTION", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "ENTITY_TYPE", length = 100)
    private String entityType;

    @Column(name = "ENTITY_ID", length = 50)
    private String entityId;

    /** Acting user; null for anonymous requests and scheduled jobs. */
    @Column(name = "USER_ID")
    private Long userId;

    /** Acting username, or "anonymous" / "SYSTEM". For LOGIN_FAILED the username that was tried. */
    @Column(name = "USERNAME", length = 200)
    private String username;

    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    /** HTTP method and path of the request that caused the entry, e.g. "DELETE /dukapp/api/donations/5". */
    @Column(name = "REQUEST", length = 300)
    private String request;

    /** JSON object with action-specific data (changed fields, snapshot of deleted entity, ...). */
    @Column(name = "DETAILS", columnDefinition = "text")
    private String details;
}
