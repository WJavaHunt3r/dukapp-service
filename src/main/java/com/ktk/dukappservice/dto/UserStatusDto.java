package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.seasons.Season;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UserStatusDto {
    private Long id;

    private UserDto user;

    private Integer goal;

    private double status;

    private Integer transactions;

    private Integer transition ;

    private Season season;

    private boolean onTrack;

    private boolean localOnTrack;

}
