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

    long countByJobIdAndStatus(Long jobId, JobRegistrationStatus status);

    /** Rows of {jobId, status, count} for the given statuses. */
    @Query("SELECT r.job.id, r.status, COUNT(r) FROM JobRegistration r WHERE r.job.id IN (?1) AND r.status IN (?2) GROUP BY r.job.id, r.status")
    List<Object[]> countByJobsAndStatuses(Collection<Long> jobIds, Collection<JobRegistrationStatus> statuses);
}
