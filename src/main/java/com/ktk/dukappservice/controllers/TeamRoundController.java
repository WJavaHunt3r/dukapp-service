package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.teamrounds.TeamRoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TeamRoundController {

    private TeamRoundService service;

    public TeamRoundController(TeamRoundService service) {
        this.service = service;
    }

    @GetMapping("/teamRounds")
    public ResponseEntity getAllTeamRounds(@RequestParam("seasonYear") int seasonYear) {
        return ResponseEntity.status(200).body(service.findAll(seasonYear));
    }
}
