package com.ktk.dukappservice.data.donation;

import com.ktk.dukappservice.data.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
