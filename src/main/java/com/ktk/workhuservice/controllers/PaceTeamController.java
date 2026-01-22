package com.ktk.workhuservice.controllers;

import com.ktk.workhuservice.data.paceteam.PaceTeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paceTeams")
public class PaceTeamController {

    private final PaceTeamService paceTeamService;

    public PaceTeamController(PaceTeamService teamService) {
        this.paceTeamService = teamService;
    }

    @GetMapping
    public ResponseEntity<?> getTeams() {
        return ResponseEntity.status(200).body(paceTeamService.findActiveTeams());
    }
}
