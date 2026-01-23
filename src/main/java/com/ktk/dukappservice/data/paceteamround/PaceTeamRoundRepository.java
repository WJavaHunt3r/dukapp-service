package com.ktk.dukappservice.data.paceteamround;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.rounds.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaceTeamRoundRepository extends JpaRepository<PaceTeamRound, Long> {
    Optional<PaceTeamRound> findByTeamAndRound(PaceTeam team, Round round);

    Iterable<PaceTeamRound> findAllByRound(Round round);

    @Query("SELECT tr from PaceTeamRound tr where tr.round.season.seasonYear = ?1 and tr.team.active = true " +
            " and (tr.team = ?2 or ?2 is null)")
    List<PaceTeamRound> findAllActiveBySeasonYear(Integer SeasonYear, PaceTeam team);

    @Query("SELECT tr from PaceTeamRound tr where tr.round = ?1 ORDER BY tr.teamRoundStatus DESC")
    PaceTeamRound findRoundWinnerTeam(Round round);

    PaceTeamRound findTopByRoundOrderByTeamRoundStatusDesc(Round round);
}
