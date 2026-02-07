package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.paceuserround.PaceUserRound;
import com.ktk.dukappservice.data.paceuserround.PaceUserRoundService;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.dto.PaceUserRoundDto;
import com.ktk.dukappservice.mapper.PaceUserRoundMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paceUserRound")
public class PaceUserRoundController {

    private final PaceUserRoundService paceUserRoundService;
    private final PaceUserRoundMapper modelMapper;
    private final RoundService roundService;

    public PaceUserRoundController(PaceUserRoundService paceUserRoundService, PaceUserRoundMapper modelMapper, RoundService roundService) {
        this.paceUserRoundService = paceUserRoundService;
        this.modelMapper = modelMapper;
        this.roundService = roundService;
    }

    @GetMapping()
    public ResponseEntity<?> getPaceUserRounds(@Nullable @RequestParam("userId") Long userId,
                                               @Nullable @RequestParam("roundId") Long roundId,
                                               @Nullable @RequestParam("seasonYear") Integer seasonYear,
                                               @Nullable @RequestParam("paceTeamId") Long paceTeamId) {
        return ResponseEntity.status(200).body(paceUserRoundService.findByQuery(userId, roundId, seasonYear, paceTeamId).stream().map(this::entityToDto));
    }

    public PaceUserRoundDto entityToDto(PaceUserRound ur) {
        return modelMapper.entityToDto(ur);
    }

    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculateTeamRoundsScore() {
        paceUserRoundService.createAllPaceUserRounds(roundService.getLastRound());
        return ResponseEntity.status(200).body("Recalculation successful");
    }
}
