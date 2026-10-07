package com.ktk.dukappservice.data.jobs;

import com.ktk.dukappservice.enums.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    /**
     * Row lock used for everything that changes who holds a place (register, cancel, edit, complete), so two requests
     * can't both take the last place or both promote the same waitlisted user.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = ?1")
    Optional<Job> findByIdForUpdate(Long id);

    /** Jobs whose "new job" notification is due: wanted, not sent yet, still open and registration is open by now. */
    @Query("SELECT j FROM Job j WHERE j.sendNotification = true AND j.announcedDateTime IS NULL AND j.status = ?1 " +
            "AND (j.registrationOpensAt IS NULL OR j.registrationOpensAt <= ?2)")
    List<Job> findDueAnnouncements(JobStatus status, java.time.LocalDateTime now);

    /** Open jobs that ended at or before {@code endBefore} (the end time, or the start when there is none). */
    @Query("SELECT j FROM Job j WHERE j.status = ?1 AND COALESCE(j.jobEndDateTime, j.jobDateTime) <= ?2 ORDER BY j.jobDateTime")
    List<Job> findOverdueOpen(JobStatus status, java.time.LocalDateTime endBefore);

    List<Job> findByActivityId(Long activityId);

    List<Job> findBySeriesIdAndStatus(String seriesId, JobStatus status);

    @Override
    @EntityGraph(attributePaths = {"employer", "responsible", "createUser"})
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);
}
