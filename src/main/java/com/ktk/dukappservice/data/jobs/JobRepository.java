package com.ktk.dukappservice.data.jobs;

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

    @Override
    @EntityGraph(attributePaths = {"employer", "responsible", "createUser"})
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);
}
