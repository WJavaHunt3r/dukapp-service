package com.ktk.dukappservice.data.camps;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.seasons.Season;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "CAMPS")
@FieldNameConstants
public class Camp extends BaseEntity<Camp, Long> {

    @NotNull
    @JoinColumn(name = "SEASON")
    @ManyToOne
    private Season season;

    @NotNull
    @NotEmpty
    @Column(name = "CAMP_NAME", length = 30)
    @Size(max = 30)
    private String campName;

    @NotNull
    @Column(name = "CAMP_DATE")
    private LocalDate campDate;

    @NotNull
    @Column(name = "FINANCE_CHECK_DATE")
    private LocalDate financeCheckDate;

    @NotNull
    @Column(name = "U18_BRUNSTAD_FEE", columnDefinition = "integer default 0")
    private Integer u18BrunstadFee;

    @NotNull
    @Column(name = "U18_LOCAL_FEE", columnDefinition = "integer default 0")
    private Integer u18LocalFee;

    @NotNull
    @Column(name = "O18_BRUNSTAD_FEE", columnDefinition = "integer default 0")
    private Integer o18BrunstadFee;

    @NotNull
    @Column(name = "O18_LOCAL_FEE", columnDefinition = "integer default 0")
    private Integer o18LocalFee;
}
