package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.AuditAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class AuditLogDto {
    private Long id;
    private LocalDateTime timestamp;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private Long userId;
    private String username;
    private String ipAddress;
    private String request;
    /** Parsed JSON object (or null). */
    private Object details;
}
