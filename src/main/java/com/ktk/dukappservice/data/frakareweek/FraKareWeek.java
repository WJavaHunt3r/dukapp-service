package com.ktk.dukappservice.data.frakareweek;

import com.ktk.dukappservice.data.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "FRA_KARE_WEEK", uniqueConstraints =
        {@UniqueConstraint(name = "UniqueYearAndWeekNumber", columnNames = {"YEAR", "WEEK_NUMBER"})})
@FieldNameConstants
public class FraKareWeek extends BaseEntity<FraKareWeek, Long> {

    @Column(name = "WEEK_NUMBER", columnDefinition = "integer default 0")
    @NotNull
    private Integer weekNumber;

    @Column(name = "ACTIVE_WEEK", columnDefinition = "boolean default true")
    @NotNull
    private boolean activeWeek;

    @Column(name = "YEAR")
    @NotNull
    private Integer year;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "WEEK_START_DATE")
    private LocalDate weekStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "WEEK_END_DATE")
    private LocalDate weekEndDate;

    @Column(name = "LOCKED", columnDefinition = "boolean default true")
    @NotNull
    private boolean locked;

    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

}
