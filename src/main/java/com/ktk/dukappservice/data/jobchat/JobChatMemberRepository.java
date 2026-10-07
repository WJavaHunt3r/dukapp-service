package com.ktk.dukappservice.data.jobchat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobChatMemberRepository extends JpaRepository<JobChatMember, Long> {

    List<JobChatMember> findByJobId(Long jobId);

    Optional<JobChatMember> findByJobIdAndUserId(Long jobId, Long userId);
}
