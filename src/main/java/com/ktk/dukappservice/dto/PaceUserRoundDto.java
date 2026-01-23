package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.rounds.Round;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class PaceUserRoundDto {
    private Long id;

    private UserDto user;

    private Round round;

    private Integer samvirkPayments = 0;

    private Integer roundCoins;

    private Integer roundCredits;

    private double bMMPerfectWeekPoints;

    private Integer roundMyShareGoal;

    private boolean onTrack;
}
