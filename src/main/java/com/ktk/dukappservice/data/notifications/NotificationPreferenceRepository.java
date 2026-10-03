package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RepositoryRestResource(exported = false)
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    @Query("SELECT p.user.id FROM NotificationPreference p WHERE p.type = ?1 AND p.enabled = false")
    Set<Long> findDisabledUserIds(NotificationType type);
}
