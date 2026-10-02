package com.ktk.dukappservice.data.auditlog;

import com.ktk.dukappservice.data.users.UserRepository;
import com.ktk.dukappservice.enums.AuditAction;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Records audit entries. Entries are captured synchronously (actor, IP and request are read from the calling
 * thread) but queued and written in batches by {@link #flush()}, so auditing never runs inside — or breaks — the
 * transaction or Hibernate flush that produced the event.
 */
@Service
public class AuditLogService {
    public static final String SYSTEM = "SYSTEM";
    public static final String ANONYMOUS = "anonymous";

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final int MAX_BATCH = 1000;

    private final BlockingQueue<AuditLog> queue = new LinkedBlockingQueue<>();
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;

    @Value("${app.audit.retentionDays:365}")
    private int retentionDays;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository, JsonMapper jsonMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
    }

    /** Records an action performed by the currently authenticated user (or SYSTEM / anonymous). */
    public void record(AuditAction action, String entityType, Object entityId, Map<String, ?> details) {
        recordAs(currentUsername(), action, entityType, entityId, details);
    }

    /** Records an action for an explicitly given actor, e.g. during login when nobody is authenticated yet. */
    public void recordAs(String username, AuditAction action, String entityType, Object entityId, Map<String, ?> details) {
        AuditLog entry = new AuditLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId == null ? null : truncate(entityId.toString(), 50));
        entry.setUsername(username == null ? ANONYMOUS : truncate(username, 200));
        entry.setDetails(details == null || details.isEmpty() ? null : toJson(details));
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            entry.setIpAddress(truncate(clientIp(request), 64));
            entry.setRequest(truncate(request.getMethod() + " " + request.getRequestURI(), 300));
        }
        AUDIT.info("{}{} by {}{}{}", entry.getAction(),
                entry.getEntityType() == null ? "" : " " + entry.getEntityType() + (entry.getEntityId() == null ? "" : "#" + entry.getEntityId()),
                entry.getUsername(),
                entry.getRequest() == null ? "" : " (" + entry.getRequest() + " from " + entry.getIpAddress() + ")",
                entry.getDetails() == null ? "" : " " + entry.getDetails());
        queue.add(entry);
    }

    @Scheduled(fixedDelayString = "${app.audit.flushIntervalMs:2000}")
    @PreDestroy
    public void flush() {
        List<AuditLog> batch = new ArrayList<>();
        while (queue.drainTo(batch, MAX_BATCH) > 0) {
            resolveUserIds(batch);
            try {
                auditLogRepository.saveAll(batch);
            } catch (Exception e) {
                LOG.error("Failed to write {} audit log entries (they are still in the AUDIT application log)", batch.size(), e);
            }
            batch = new ArrayList<>();
        }
    }

    public Page<AuditLog> fetchByQuery(String dateFrom, String dateTo, AuditAction action, Long userId, String username,
                                       String entityType, String entityId, String keyword, Pageable pageable) {
        LocalDateTime from = parseDate(dateFrom, false);
        LocalDateTime to = parseDate(dateTo, true);
        return auditLogRepository.fetchByQuery(
                from == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : from,
                to == null ? LocalDateTime.now().plusDays(1) : to,
                action, userId, blankToEmpty(username), blankToNull(entityType), blankToNull(entityId), blankToEmpty(keyword), pageable);
    }

    public Optional<AuditLog> findById(Long id) {
        return auditLogRepository.findById(id);
    }

    public List<String> findEntityTypes() {
        return auditLogRepository.findEntityTypes();
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteExpiredEntries() {
        int deleted = auditLogRepository.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
        LOG.info("Deleted {} audit log entries older than {} days", deleted, retentionDays);
    }

    private void resolveUserIds(List<AuditLog> batch) {
        Map<String, Long> ids = new HashMap<>();
        for (AuditLog entry : batch) {
            String username = entry.getUsername();
            if (entry.getUserId() != null || SYSTEM.equals(username) || ANONYMOUS.equals(username)) {
                continue;
            }
            entry.setUserId(ids.computeIfAbsent(username, u -> {
                List<Long> found = userRepository.findIdsByUsernameOrEmail(u);
                return found.size() == 1 ? found.getFirst() : null;
            }));
        }
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return auth.getName();
        }
        return RequestContextHolder.getRequestAttributes() != null ? ANONYMOUS : SYSTEM;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Map<String, ?> details) {
        try {
            return jsonMapper.writeValueAsString(details);
        } catch (Exception e) {
            return String.valueOf(details);
        }
    }

    /** Accepts "yyyy-MM-dd" (a date "to" is inclusive) or an ISO date-time. */
    private static LocalDateTime parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
            }
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date: " + value);
        }
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
