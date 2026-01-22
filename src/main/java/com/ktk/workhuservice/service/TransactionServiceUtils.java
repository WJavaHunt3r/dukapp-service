package com.ktk.workhuservice.service;

import com.ktk.workhuservice.data.goals.GoalService;
import com.ktk.workhuservice.data.paceteamround.PaceTeamRoundService;
import com.ktk.workhuservice.data.paceuserround.PaceUserRoundService;
import com.ktk.workhuservice.data.rounds.Round;
import com.ktk.workhuservice.data.users.User;
import com.ktk.workhuservice.data.userstatus.UserStatusService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TransactionServiceUtils {

    private final PaceUserRoundService paceUserRoundService;
    private final PaceTeamRoundService paceTeamRoundService;
    private final UserStatusService userStatusService;
    private final GoalService goalService;

    public TransactionServiceUtils(PaceUserRoundService paceUserRoundService, PaceTeamRoundService paceTeamRoundService, UserStatusService userStatusService, GoalService goalService) {
        this.paceUserRoundService = paceUserRoundService;
        this.paceTeamRoundService = paceTeamRoundService;
        this.userStatusService = userStatusService;
        this.goalService = goalService;
    }

    public void updateUserStatus(Round round, User user) {
        goalService.findByUserAndSeasonYear(user, LocalDate.now().getYear()).ifPresent(g -> userStatusService.calculateUserStatus(user, g.getGoal()));
        paceUserRoundService.calculateUserRoundStatus(round, user);
    }

    public void calculateAllTeamStatus() {
        paceTeamRoundService.calculateAllTeamRoundPoints();
    }

    public void calculateAllTeamStatus(Round round) {
        paceTeamRoundService.calculateAllTeamRoundPoints(round);
    }

}
