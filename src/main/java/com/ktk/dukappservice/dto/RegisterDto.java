package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.Gender;
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
    /** MALE / FEMALE. Optional for older clients, but users without it can't join gender-restricted jobs. */
    private Gender gender;
}
