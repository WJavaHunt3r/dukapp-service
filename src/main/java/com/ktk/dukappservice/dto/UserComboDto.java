package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UserComboDto {
    private Long id;

    private String firstname;

    private String lastname;

    private Integer age;

    private String comboText;

    private String churchName;
}
