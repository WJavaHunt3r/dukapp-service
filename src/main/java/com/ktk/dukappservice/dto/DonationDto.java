package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
public class DonationDto {
    private Long id;

    private String description;

    private String descriptionNO;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Integer sum;
}
