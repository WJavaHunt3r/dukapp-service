package com.ktk.dukappservice.data.seasons;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findBySeasonYear(Integer year);

    @Query("SELECT s FROM Season s WHERE s.startDate < ?1 AND s.endDate > ?1 ")
    Optional<Season> findCurrentSeason(LocalDate now);
}
