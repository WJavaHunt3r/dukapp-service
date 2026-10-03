package com.ktk.dukappservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
public class GeneralNotificationDto {
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String body;

    /** Target role ids; empty = all users. */
    private Set<Long> roleIds = new HashSet<>();

    // Read-only, filled after sending
    private Long sentById;
    private String sentByName;
    private LocalDateTime sentDateTime;
    private int recipientUsers;
    private int delivered;
    private int failed;
}
