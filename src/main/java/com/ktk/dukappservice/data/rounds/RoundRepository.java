package com.ktk.dukappservice.data.rounds;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    Optional<Round> findByRoundNumber(Integer roundNumber);

    @Query("SELECT s FROM Round s WHERE s.startDateTime <= ?1 AND s.endDateTime >= ?1 ")
    Optional<Round> findRoundByDate(LocalDateTime date);

    @Query("SELECT round FROM Round round WHERE round.roundNumber = (SELECT MAX(r.roundNumber) FROM Round r) " +
            " and round.season.seasonYear = ?1 " +
            " and round.activeRound = true ")
    Round getLastRound(Integer year);

    List<Round> findAllBySeasonSeasonYearAndActiveRound(int year, boolean isActive);
}
