package com.ktk.dukappservice.data.donation;

import com.ktk.dukappservice.data.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "DONATIONS")
@FieldNameConstants
public class Donation extends BaseEntity<Donation, Long> {
    @NotNull
    @NotEmpty
    @Column(name = "DESCRIPTION", length = 60)
    @Size(max = 60)
    private String description;

    @NotNull
    @NotEmpty
    @Column(name = "DESCRIPTION_NO", length = 60)
    @Size(max = 60)
    private String descriptionNO;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "START_DATE_TIME")
    private LocalDateTime startDateTime;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "END_DATE_TIME")
    private LocalDateTime endDateTime;

}
