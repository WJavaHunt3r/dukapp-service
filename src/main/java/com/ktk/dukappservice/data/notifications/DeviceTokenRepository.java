package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The recipient queries return the tokens of users who haven't switched the notification type off
 * (no preference row means enabled), with the user fetched.
 */
@Repository
@RepositoryRestResource(exported = false)
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    String NOT_DISABLED = "NOT EXISTS (SELECT p FROM NotificationPreference p " +
            "WHERE p.user = u AND p.type = :type AND p.enabled = false)";

    Optional<DeviceToken> findByToken(String token);

    @Query("SELECT t FROM DeviceToken t JOIN FETCH t.user u WHERE u.id IN :userIds AND " + NOT_DISABLED)
    List<DeviceToken> findForUsers(@Param("userIds") Collection<Long> userIds, @Param("type") NotificationType type);

    @Query("SELECT t FROM DeviceToken t JOIN FETCH t.user u WHERE u.church.id = :churchId AND " + NOT_DISABLED)
    List<DeviceToken> findForChurch(@Param("churchId") Long churchId, @Param("type") NotificationType type);

    /** {@code roleIds} must not be empty (JPQL); pass a dummy id together with {@code allUsers = true}. */
    @Query("SELECT t FROM DeviceToken t JOIN FETCH t.user u WHERE " + NOT_DISABLED + " AND (:allUsers = true OR " +
            "EXISTS (SELECT r FROM User ru JOIN ru.roles r WHERE ru = u AND r.id IN :roleIds))")
    List<DeviceToken> findForBroadcast(@Param("type") NotificationType type, @Param("allUsers") boolean allUsers,
                                       @Param("roleIds") Collection<Long> roleIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM DeviceToken t WHERE t.token IN :tokens")
    int deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
