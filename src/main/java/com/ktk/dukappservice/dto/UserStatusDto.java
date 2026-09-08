package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UserStatusDto {
    private Long id;

    private String name;

    private Long userId;

    private Integer goal;

    private double status;

    private Integer transactions;

    private Integer transition ;

    private Integer seasonYear;

    private boolean onTrack;

    private boolean localOnTrack;

    private Integer toOnTrack;

    private Integer toLocalOnTrack;

}
