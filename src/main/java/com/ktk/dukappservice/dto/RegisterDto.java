package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RegisterDto {
    private String email;
    private String password;
    private String firstname;
    private String lastname;
}
