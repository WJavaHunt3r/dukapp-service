package com.ktk.dukappservice.data.paceuserround;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaceUserRoundRepository extends JpaRepository<PaceUserRound, Long> {
    Optional<PaceUserRound> findByUserAndRound(User u, Round r);

    @Query("SELECT sum(ur.roundCoins) FROM PaceUserRound ur WHERE ur.round.activeRound = true and ur.user = ?1 AND ur.round.season.seasonYear = ?2 ")
    Double sumByUserAndSeason(User u, Integer seasonYear);

    @Query(value = "SELECT COUNT(ur.id) FROM user_status us "
            + "RIGHT JOIN public.pace_user_rounds ur ON ur.id = us.users "
            + "LEFT JOIN public.users u ON u.id = ur.users "
            + "WHERE u.pace_teams = ?2 AND ur.rounds = ?1 ",
            nativeQuery = true)
    int countByRoundAndTeam(Round r, PaceTeam t);

    @Query(value = "SELECT sum(ur.round_coins) FROM user_status us "
            + "RIGHT JOIN public.pace_user_rounds ur ON ur.id = us.users "
            + "LEFT JOIN public.users u ON u.id = ur.users "
            + "WHERE u.pace_teams = ?1 AND ur.rounds = ?2 ",
            nativeQuery = true)
    Integer calculatePaceTeamRoundCoins(PaceTeam t, Round round);

    @Query("SELECT sum(ur.samvirkPayments) FROM PaceUserRound  ur where ur.user.paceTeam = ?1 " +
            " and ur.round = ?2 ")
    double calculateTeamRoundSamvirkPayments(PaceTeam t, Round round);

    @Query("SELECT ur FROM PaceUserRound ur WHERE ( ur.user.id = ?1 OR ?1 IS NULL ) " +
            " AND ( ur.round.id = ?2 OR ?2 IS NULL) " +
            " AND ( ur.round.season.seasonYear = ?3 OR ?3 IS NULL) " +
            " AND ( ur.user.paceTeam.id = ?4 OR ?4 IS NULL) ")
    List<PaceUserRound> findByQuery(Long userId, Long roundId, Integer seasonYear, Long paceTeamId);

    @Query("SELECT count(ur) FROM PaceUserRound ur WHERE ur.round = ?2 and ur.user.paceTeam = ?1 AND ur.onTrack = true ")
    Integer countOnTrackByTeamAndRound(PaceTeam team, Round round);
}
