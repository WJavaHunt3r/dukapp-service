package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class GoalDto {
    private Long id;

    private Long userId;

    private String username;

    private Integer seasonYear;

    private Integer goal;
}
