package com.ktk.dukappservice.data.challenges;

import com.ktk.dukappservice.data.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "CHALLENGES")
@FieldNameConstants
public class Challenges extends BaseEntity<Challenges, Long> {

    @NotNull
    @NotEmpty
    @Column(name = "NAME", length = 30)
    @Size(max = 30)
    private String name;

    @NotNull
    @NotEmpty
    @Column(name = "DESCRIPTION", length = 200)
    @Size(max = 200)
    private String description;

    @NotNull
    @NotEmpty
    @Column(name = "IMAGE_PATH", length = 200)
    @Size(max = 200)
    private String imagePath;

    @NotNull
    @Column(name = "START_DATE")
    private LocalDate startDate;

    @NotNull
    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "POINTS", columnDefinition = "integer default 0")
    private Integer points;

    @Column(name = "ACTIVE", columnDefinition = "boolean default true")
    private Boolean active;

}
