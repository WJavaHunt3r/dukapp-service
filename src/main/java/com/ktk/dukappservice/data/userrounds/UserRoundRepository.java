package com.ktk.dukappservice.data.userrounds;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.data.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoundRepository extends JpaRepository<UserRound, Long> {

    Optional<UserRound> findByUserAndRound(User u, Round r);

    List<UserRound> findByUser(User u);

    @Query("SELECT ur FROM UserRound ur WHERE ur.user = ?1 AND ur.round.season.seasonYear = ?2 ")
    List<UserRound> findByUserAndSeason(User u, Integer seasonYear);

    Iterable<UserRound> findByRound(Round r);

    @Query("SELECT ur FROM UserRound ur WHERE ur.round = ?1 AND ur.user.team = ?2 ")
    Iterable<UserRound> findByRoundAndTeam(Round r, Team t);

    @Query("SELECT sum(ur.roundPoints) FROM UserRound  ur where ur.user.team = ?1 " +
            " and ur.round = ?2 ")
    double calculateTeamRoundPoints(Team t, Round round);

    @Query("SELECT sum(ur.samvirkPayments) FROM UserRound  ur where ur.user.team = ?1 " +
            " and ur.round = ?2 ")
    double calculateTeamRoundSamvirkPayments(Team t, Round round);
}
