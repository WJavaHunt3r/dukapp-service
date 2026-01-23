package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.camps.Camp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserCampDto {

    private Long id;

    private UserDto user;

    private Camp camp;

    private boolean participates;

    private Integer price;
}
