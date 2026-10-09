package com.ktk.dukappservice.data.jobregistrations;

import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class JobRegistrationService extends BaseService<JobRegistration, Long> {

    private final JobRegistrationRepository repository;

    public List<com.ktk.dukappservice.data.jobs.Job> findJobsOf(Long userId, java.time.LocalDateTime from) {
        return repository.findJobsOf(userId, com.ktk.dukappservice.enums.JobRegistrationStatus.REGISTERED,
                com.ktk.dukappservice.enums.JobStatus.CANCELLED, from);
    }

    public JobRegistrationService(JobRegistrationRepository repository) {
        this.repository = repository;
    }

    public Optional<JobRegistration> findByJobAndUser(Long jobId, Long userId) {
        return repository.findByJobIdAndUserId(jobId, userId);
    }

    /** Oldest first, which is also the waitlist order. */
    public List<JobRegistration> findByJobAndStatus(Long jobId, JobRegistrationStatus status) {
        return repository.findByJobIdAndStatusOrderByRegisteredDateTimeAscIdAsc(jobId, status);
    }

    public List<JobRegistration> findByJobsAndUser(Collection<Long> jobIds, Long userId) {
        if (jobIds.isEmpty()) {
            return List.of();
        }
        return repository.findByJobIdInAndUserId(jobIds, userId);
    }

    public long countByJobAndStatus(Long jobId, JobRegistrationStatus status) {
        return repository.countByJobIdAndStatus(jobId, status);
    }

    public List<Object[]> countByJobsAndStatuses(Collection<Long> jobIds, Collection<JobRegistrationStatus> statuses) {
        if (jobIds.isEmpty()) {
            return List.of();
        }
        return repository.countByJobsAndStatuses(jobIds, statuses);
    }

    @Override
    protected JpaRepository<JobRegistration, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<JobRegistration> getEntityClass() {
        return JobRegistration.class;
    }

    @Override
    public JobRegistration createEntity() {
        return new JobRegistration();
    }
}
