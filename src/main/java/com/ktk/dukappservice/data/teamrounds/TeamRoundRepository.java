package com.ktk.dukappservice.data.teamrounds;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.teams.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRoundRepository extends JpaRepository<TeamRound, Long> {
    Optional<TeamRound> findByTeamAndRound(Team team, Round round);

    Iterable<TeamRound> findAllByTeam(Team team);

    Iterable<TeamRound> findAllByRound(Round round);

    @Query("SELECT tr from TeamRound tr where round.season.seasonYear = ?1")
    Iterable<TeamRound> findAllBySeasonYear(Integer seasonYear);
}
