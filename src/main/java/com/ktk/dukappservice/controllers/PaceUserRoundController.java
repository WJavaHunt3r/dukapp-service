package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.paceuserround.PaceUserRound;
import com.ktk.dukappservice.data.paceuserround.PaceUserRoundService;
import com.ktk.dukappservice.dto.PaceUserRoundDto;
import com.ktk.dukappservice.mapper.PaceUserRoundMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paceUserRound")
public class PaceUserRoundController {

    private final PaceUserRoundService paceUserRoundService;
    private final PaceUserRoundMapper modelMapper;

    public PaceUserRoundController(PaceUserRoundService paceUserRoundService, PaceUserRoundMapper modelMapper) {
        this.paceUserRoundService = paceUserRoundService;
        this.modelMapper = modelMapper;
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
}
