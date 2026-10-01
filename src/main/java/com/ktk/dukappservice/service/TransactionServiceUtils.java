package com.ktk.dukappservice.service;

import com.ktk.dukappservice.data.goals.GoalService;
import com.ktk.dukappservice.data.paceteamround.PaceTeamRoundService;
import com.ktk.dukappservice.data.paceuserround.PaceUserRoundService;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceUtils {

    private final PaceUserRoundService paceUserRoundService;
    private final PaceTeamRoundService paceTeamRoundService;
    private final UserStatusService userStatusService;
    private final GoalService goalService;
    private final UserService userService;

    public TransactionServiceUtils(PaceUserRoundService paceUserRoundService, PaceTeamRoundService paceTeamRoundService, UserStatusService userStatusService, GoalService goalService, UserService userService) {
        this.paceUserRoundService = paceUserRoundService;
        this.paceTeamRoundService = paceTeamRoundService;
        this.userStatusService = userStatusService;
        this.goalService = goalService;
        this.userService = userService;
    }

    public void updateUserStatus(Round round, User user) {
        userStatusService.calculateUserStatus(user, round.getSeason());
        paceUserRoundService.calculateUserRoundStatus(round, user);
        // The spouse shares the couple's status, so their round status changes too
        if (user.getSpouseId() != null) {
            userService.findById(user.getSpouseId())
                    .ifPresent(spouse -> paceUserRoundService.calculateUserRoundStatus(round, spouse));
        }
    }

    public void calculateAllTeamStatus() {
        paceTeamRoundService.calculateAllTeamRoundPoints();
    }

    public void calculateAllTeamStatus(Round round) {
        paceTeamRoundService.calculateAllTeamRoundPoints(round);
    }

}
