package com.ktk.dukappservice.data.camps;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampRepository extends JpaRepository<Camp, Long> {

    @Query(value = "SELECT r FROM Camp r join fetch r.season where " +
            " (r.season.seasonYear = ?1 OR ?1 IS NULL) ",
            countQuery = "SELECT r FROM Camp r where " +
                    " (r.season.seasonYear = ?1 OR ?1 IS NULL) ")
    Page<Camp> fetchByQuery(Integer season, Pageable pageable);

    Optional<Camp> findByCampName(String name);
}
