package com.ktk.dukappservice.data.jobchat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobChatMuteRepository extends JpaRepository<JobChatMute, Long> {

    Optional<JobChatMute> findByJobIdAndUserId(Long jobId, Long userId);

    List<JobChatMute> findByJobIdAndUserIdIn(Long jobId, Collection<Long> userIds);
}
