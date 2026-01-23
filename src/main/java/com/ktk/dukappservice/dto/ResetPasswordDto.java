package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ResetPasswordDto {

    private Long userId;

    private Long changerId;
}
