package com.ktk.dukappservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Hours for every registered user of the job (0 for no-shows). */
@NoArgsConstructor
@Getter
@Setter
public class JobCompleteDto {

    @NotEmpty
    @Valid
    private List<Item> items;

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Item {
        @NotNull
        private Long userId;

        @NotNull
        @PositiveOrZero
        private Double hours;

        /** Optional, defaults to the job description. */
        @Size(max = 150)
        private String description;
    }
}
