package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.teams.TeamService;
import com.ktk.dukappservice.data.users.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TeamsController {

    private TeamService teamService;

    public TeamsController(TeamService teamService, UserService userService) {
        this.teamService = teamService;
    }

    @GetMapping("/teams")
    public ResponseEntity<?> getTeams(@Nullable @RequestParam("roundId") Long roundId ) {
        return ResponseEntity.status(200).body(teamService.findAll());
    }

    @GetMapping("/recalculate")
    public ResponseEntity<?> recalculateAllPoint(){
        teamService.recalculateAllTeamPoints();
        return ResponseEntity.status(200).build();
    }
}
