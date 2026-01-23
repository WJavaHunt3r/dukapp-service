package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class MentorMenteeDto {
    private Long id;

    private UserDto mentor;

    private UserDto mentee;
}
