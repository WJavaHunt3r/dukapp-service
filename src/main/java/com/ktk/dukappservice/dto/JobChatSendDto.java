package com.ktk.dukappservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JobChatSendDto {
    @NotBlank
    @Size(max = 2000)
    private String text;
}
