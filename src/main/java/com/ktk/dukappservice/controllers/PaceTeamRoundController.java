package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.paceteamround.PaceTeamRoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paceTeamRounds")
public class PaceTeamRoundController {

    private final PaceTeamRoundService service;

    public PaceTeamRoundController(PaceTeamRoundService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getAllPaceTeamRounds(@RequestParam("seasonYear") int seasonYear) {
        return ResponseEntity.status(200).body(service.findAllActiveTeamRounds(seasonYear));
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasAuthority('SEASON_MANAGE')")
    public ResponseEntity<?> recalculateTeamRoundsScore() {
        service.calculateAllTeamRoundPoints();
        return ResponseEntity.status(200).body("Recalculation successful");
    }

    @PostMapping("/recalculateAll")
    @PreAuthorize("hasAuthority('SEASON_MANAGE')")
    public ResponseEntity<?> recalculateAllTeamRoundsScore() {
        service.calculateAllTeamAllRoundPoints();
        return ResponseEntity.status(200).body("Recalculation successful");
    }

}
