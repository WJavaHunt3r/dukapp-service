package com.ktk.dukappservice.data.goals;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "GOALS", uniqueConstraints =
        { @UniqueConstraint(name = "UniqueSeasonAndUser", columnNames = { "SEASON", "USERS" })})
@FieldNameConstants
@RequiredArgsConstructor
public class Goal extends BaseEntity<Goal, Long> {

    @NotNull
    @JoinColumn(name = "SEASON")
    @ManyToOne
    private Season season;

    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    @Column(name = "GOAL", columnDefinition = "integer default 0")
    @NotNull
    private Integer goal;
}
