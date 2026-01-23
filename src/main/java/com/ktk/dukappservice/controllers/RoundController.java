package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/round")
public class RoundController {
    private RoundService roundService;

    public RoundController(RoundService roundService) {
        this.roundService = roundService;
    }

    @GetMapping()
    public ResponseEntity getRounds(@Nullable @RequestParam("seasonYear") Integer seasonYear, @Nullable @RequestParam("activeRounds") Boolean activeRounds) {
        if (seasonYear != null) {
            return ResponseEntity.status(200).body(roundService.findAllByRoundYear(seasonYear));
        }
        return ResponseEntity.status(200).body(roundService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity getRound(@Nullable @PathVariable Long id) {
        if (id != null) {
            var round = roundService.findById(id);
            if (round.isEmpty()) {
                return ResponseEntity.status(404).body("No round with id: " + id);
            }
            return ResponseEntity.status(200).body(round.get());
        }
        return ResponseEntity.status(200).body(roundService.findRoundByDate(LocalDateTime.now()));
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
        return ResponseEntity.status(200).body(roundService.findRoundByDate(LocalDateTime.now()));
    }
}
