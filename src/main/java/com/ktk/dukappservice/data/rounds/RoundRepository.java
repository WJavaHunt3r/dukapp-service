package com.ktk.dukappservice.data.rounds;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {

    @Query("SELECT s FROM Round s WHERE s.startDateTime <= ?1 AND s.endDateTime >= ?1 ")
    Optional<Round> findRoundByDate(LocalDateTime date);

    @Query("SELECT r FROM Round r order by r.id desc limit 1")
    Round getLastRound();

    @Query(value = "SELECT r FROM Round r join fetch r.season where " +
            " (r.activeRound = ?2 OR ?2 IS NULL) " +
            " AND (r.season.seasonYear = ?1 OR ?1 IS NULL) ",
            countQuery = "SELECT r FROM Round r where " +
                    " (r.activeRound = ?2 OR ?2 IS NULL) " +
                    " AND (r.season.seasonYear = ?1 OR ?1 IS NULL) ")
    Page<Round> fetchByQuery(int year, boolean isActive, Pageable pageable);

}
