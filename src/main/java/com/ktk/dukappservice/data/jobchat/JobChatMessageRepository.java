package com.ktk.dukappservice.data.jobchat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobChatMessageRepository extends JpaRepository<JobChatMessage, Long> {

    /** The newest messages, newest first. */
    List<JobChatMessage> findTop50ByJobIdOrderByIdDesc(Long jobId);

    /** Older messages than {@code id}, newest first. */
    List<JobChatMessage> findTop50ByJobIdAndIdLessThanOrderByIdDesc(Long jobId, Long id);

    /** Messages newer than {@code id}, oldest first. */
    List<JobChatMessage> findTop200ByJobIdAndIdGreaterThanOrderByIdAsc(Long jobId, Long id);

    @Query("SELECT COUNT(m) FROM JobChatMessage m WHERE m.job.id = ?1")
    long countByJob(Long jobId);
}
