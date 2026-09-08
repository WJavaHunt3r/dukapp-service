package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class PaceUserRoundHeadDto {
    private Integer onTrackCount;

    private Integer goalCount;

    private Integer churchGoal;

    private Integer toOnTrackCount;
}
