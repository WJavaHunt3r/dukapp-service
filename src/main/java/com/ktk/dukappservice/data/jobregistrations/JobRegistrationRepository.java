package com.ktk.dukappservice.data.jobregistrations;

import com.ktk.dukappservice.enums.JobRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRegistrationRepository extends JpaRepository<JobRegistration, Long> {

    Optional<JobRegistration> findByJobIdAndUserId(Long jobId, Long userId);

    List<JobRegistration> findByJobIdAndStatusOrderByRegisteredDateTimeAscIdAsc(Long jobId, JobRegistrationStatus status);

    List<JobRegistration> findByJobIdInAndUserId(Collection<Long> jobIds, Long userId);

    /** The jobs the user is registered for that haven't been cancelled and start at or after {@code from}. */
    @Query("SELECT r.job FROM JobRegistration r WHERE r.user.id = ?1 AND r.status = ?2 AND r.job.status <> ?3 AND r.job.jobDateTime >= ?4 ORDER BY r.job.jobDateTime")
    List<com.ktk.dukappservice.data.jobs.Job> findJobsOf(Long userId, JobRegistrationStatus status,
                                                          com.ktk.dukappservice.enums.JobStatus excluded, java.time.LocalDateTime from);

    long countByJobIdAndStatus(Long jobId, JobRegistrationStatus status);

    /** Rows of {jobId, status, count} for the given statuses. */
    @Query("SELECT r.job.id, r.status, COUNT(r) FROM JobRegistration r WHERE r.job.id IN (?1) AND r.status IN (?2) GROUP BY r.job.id, r.status")
    List<Object[]> countByJobsAndStatuses(Collection<Long> jobIds, Collection<JobRegistrationStatus> statuses);
}
