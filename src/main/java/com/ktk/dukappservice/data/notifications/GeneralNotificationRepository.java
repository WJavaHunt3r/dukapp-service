package com.ktk.dukappservice.data.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface GeneralNotificationRepository extends JpaRepository<GeneralNotification, Long> {
}
