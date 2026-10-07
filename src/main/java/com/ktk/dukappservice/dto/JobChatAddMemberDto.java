package com.ktk.dukappservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JobChatAddMemberDto {
    @NotNull
    private Long userId;
}
