package com.ktk.dukappservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class JobRegisterDto {
    /** Who to register: null for yourself, or your child / anyone if you have JOB_MANAGE_ALL. */
    private Long userId;

    @Size(max = 500)
    private String comment;
}
