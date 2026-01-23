package com.ktk.dukappservice.data.teamrounds;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.data.teams.TeamService;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.userrounds.UserRoundService;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TeamRoundService extends BaseService<TeamRound, Long> {
    private TeamRoundRepository teamRoundRepository;
    private RoundService roundService;
    private TeamService teamService;
    private UserService userService;
    private UserRoundService userRoundService;

    public TeamRoundService(TeamRoundRepository teamRoundRepository, RoundService userRoundService, TeamService teamService, UserService userService, UserRoundService userRoundService1, TransactionItemService transactionItemService) {
        this.teamRoundRepository = teamRoundRepository;
        this.roundService = userRoundService;
        this.teamService = teamService;
        this.userService = userService;
        this.userRoundService = userRoundService1;
    }

    public Iterable<TeamRound> findAll(int seasonYear) {
        teamService.findAll().forEach(t ->
                roundService.findAllByRoundYear(seasonYear).forEach(r -> {
                            if (findByTeamAndRound(t, r).isEmpty()) {
                                save(createTeamRound(t, r));
                            }
                        }
                )
        );
        calculateTeamRoundPoints();
        return teamRoundRepository.findAllBySeasonYear(seasonYear);
    }

    private void calculateTeamRoundPoints() {
        for (TeamRound tr : teamRoundRepository.findAllByRound(roundService.getLastRound())) {

            if (!tr.getRound().getUserRoundsCreated()) {
                userRoundService.getUserRoundsOrCreate(tr.getRound());
                Round round = tr.getRound();
                round.setUserRoundsCreated(true);
                roundService.save(round);
            }

            double teamPoints = userRoundService.calculateTeamRoundPoints(tr.getTeam(), tr.getRound());
            double samvirkPayments = userRoundService.calculateTeamRoundSamvirkPayments(tr.getTeam(), tr.getRound());
//            for (UserRound ur : userRoundService.findByRoundAndTeam(tr.getRound(), tr.getTeam())) {
//                samvirkPayments += ur.getSamvirkPayments();
//                teamPoints += ur.getRoundPoints();
//
//            }
            tr.setSamvirkPayments(samvirkPayments);
            Long userCount = userService.countAllByTeam(tr.getTeam(), tr.getRound().getSeason().getSeasonYear());
            tr.setTeamPoints(userCount == 0 ? 0 : teamPoints / userCount);
            teamRoundRepository.save(tr);

        }

    }

    private TeamRound createTeamRound(Team t, Round r) {
        TeamRound teamRound = new TeamRound();
        teamRound.setTeam(t);
        teamRound.setRound(r);
        return teamRound;
    }

    private Optional<TeamRound> findByTeamAndRound(Team team, Round round) {
        return teamRoundRepository.findByTeamAndRound(team, round);
    }

    @Override
    protected JpaRepository<TeamRound, Long> getRepository() {
        return teamRoundRepository;
    }

    @Override
    public Class<TeamRound> getEntityClass() {
        return TeamRound.class;
    }

    @Override
    public TeamRound createEntity() {
        return new TeamRound();
    }
}
