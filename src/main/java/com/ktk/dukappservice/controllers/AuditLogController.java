package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.auditlog.AuditLog;
import com.ktk.dukappservice.data.auditlog.AuditLogService;
import com.ktk.dukappservice.dto.AuditLogDto;
import com.ktk.dukappservice.enums.AuditAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/auditLog")
@PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final JsonMapper jsonMapper;

    public AuditLogController(AuditLogService auditLogService, JsonMapper jsonMapper) {
        this.auditLogService = auditLogService;
        this.jsonMapper = jsonMapper;
    }

    /**
     * All filters are optional. Dates are "yyyy-MM-dd" (dateTo inclusive) or ISO date-times.
     * {@code searchText} matches the details JSON and the request path. Default sort: newest first.
     */
    @GetMapping()
    public ResponseEntity<?> getAuditLogs(@RequestParam(value = "dateFrom", required = false) String dateFrom,
                                          @RequestParam(value = "dateTo", required = false) String dateTo,
                                          @RequestParam(value = "action", required = false) AuditAction action,
                                          @RequestParam(value = "userId", required = false) Long userId,
                                          @RequestParam(value = "username", required = false) String username,
                                          @RequestParam(value = "entityType", required = false) String entityType,
                                          @RequestParam(value = "entityId", required = false) String entityId,
                                          @RequestParam(value = "searchText", required = false) String searchText,
                                          @PageableDefault(size = 50, sort = AuditLog.Fields.timestamp, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.fetchByQuery(dateFrom, dateTo, action, userId, username, entityType, entityId, searchText, pageable)
                .map(this::entityToDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAuditLog(@PathVariable Long id) {
        return auditLogService.findById(id)
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(entityToDto(a)))
                .orElseGet(() -> ResponseEntity.status(404).body("No audit log entry with id: " + id));
    }

    @GetMapping("/actions")
    public ResponseEntity<?> getActions() {
        return ResponseEntity.ok(AuditAction.values());
    }

    @GetMapping("/entityTypes")
    public ResponseEntity<?> getEntityTypes() {
        return ResponseEntity.ok(auditLogService.findEntityTypes());
    }

    private AuditLogDto entityToDto(AuditLog entry) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(entry.getId());
        dto.setTimestamp(entry.getTimestamp());
        dto.setAction(entry.getAction());
        dto.setEntityType(entry.getEntityType());
        dto.setEntityId(entry.getEntityId());
        dto.setUserId(entry.getUserId());
        dto.setUsername(entry.getUsername());
        dto.setIpAddress(entry.getIpAddress());
        dto.setRequest(entry.getRequest());
        if (entry.getDetails() != null) {
            try {
                dto.setDetails(jsonMapper.readTree(entry.getDetails()));
            } catch (Exception e) {
                dto.setDetails(entry.getDetails());
            }
        }
        return dto;
    }
}
