package com.ktk.dukappservice.data.auditlog;

import com.ktk.dukappservice.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * {@code username} and {@code keyword} must be '' (not null) when unused: a null inside concat() is bound
     * without a type and PostgreSQL then treats it as bytea ("function lower(bytea) does not exist").
     */
    @Query("SELECT a FROM AuditLog a " +
            "WHERE a.timestamp >= :dateFrom AND a.timestamp < :dateTo " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:userId IS NULL OR a.userId = :userId) " +
            "AND (:username = '' OR lower(a.username) LIKE lower(concat('%', :username, '%'))) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (:kw = '' OR (" +
            "   lower(a.details) LIKE lower(concat('%', :kw, '%')) OR " +
            "   lower(a.request) LIKE lower(concat('%', :kw, '%'))" +
            "))")
    Page<AuditLog> fetchByQuery(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("action") AuditAction action,
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("kw") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a WHERE a.entityType IS NOT NULL ORDER BY a.entityType")
    List<String> findEntityTypes();

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < ?1")
    int deleteOlderThan(LocalDateTime timestamp);
}
