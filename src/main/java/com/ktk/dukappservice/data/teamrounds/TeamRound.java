package com.ktk.dukappservice.data.teamrounds;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.teams.Team;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "TEAM_ROUNDS")
@FieldNameConstants
public class TeamRound extends BaseEntity<TeamRound, Long> {
    @JoinColumn(name = "TEAM")
    @ManyToOne
    @NotNull
    private Team team;

    @JoinColumn(name = "ROUND")
    @ManyToOne
    @NotNull
    private Round round;

    @Column(name = "TEAM_POINTS", columnDefinition = "float8 default 0")
    private double teamPoints;

    @Column(name = "SAMVIRK_PAYMENTS", columnDefinition = "float8 default 0")
    private double samvirkPayments;
}
