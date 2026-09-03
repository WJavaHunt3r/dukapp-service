package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/round")
public class RoundController {
    private final RoundService roundService;

    public RoundController(RoundService roundService) {
        this.roundService = roundService;
    }

    @GetMapping()
    public ResponseEntity<?> getRounds(@RequestParam(value = "seasonYear", required = false) Integer seasonYear,
                                       @RequestParam(value = "activeRound", required = false) Boolean activeRounds, Pageable pageable) {
        return ResponseEntity.status(200).body(roundService.fetchByQuery(seasonYear, activeRounds, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRound(@PathVariable Long id) {
        var round = roundService.findById(id);
        if (round.isEmpty()) {
            return ResponseEntity.status(404).body("No round with id: " + id);
        }
        return ResponseEntity.status(200).body(round.get());
    }

    @PostMapping()
    public ResponseEntity postRound(@Valid @RequestBody Round round) {
        return ResponseEntity.status(200).body(roundService.save(round));
    }

    @PutMapping("/{id}")
    public ResponseEntity putRound(@Valid @RequestBody Round round, @PathVariable Long id) {
        if (roundService.findById(id).isEmpty() || !round.getId().equals(id)) {
            return ResponseEntity.status(400).body("Invalid roundId");
        }
        return ResponseEntity.status(200).body(roundService.save(round));
    }

    @GetMapping("/currentRound")
    public ResponseEntity getCurrentRound() {
        return ResponseEntity.status(200).body(roundService.getCurrentRound());
    }
}
