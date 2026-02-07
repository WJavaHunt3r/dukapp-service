package com.ktk.dukappservice.data.paceteamround;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.paceteam.PaceTeam;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "PACE_TEAM_ROUNDS")
@FieldNameConstants
public class PaceTeamRound extends BaseEntity<PaceTeamRound, Long> {
    @JoinColumn(name = "TEAM")
    @ManyToOne
    @NotNull
    private PaceTeam team;

    @JoinColumn(name = "ROUND")
    @ManyToOne
    @NotNull
    private Round round;

    @Column(name = "TEAM_ROUND_COINS")
    @ColumnDefault("0.0")
    private Double teamRoundCoins;

    @Column(name = "MAX_TEAM_ROUND_COINS", columnDefinition = "integer default 0")
    private Integer maxTeamRoundCoins;

    @Column(name = "TEAM_ROUND_STATUS")
    @ColumnDefault("0.0")
    private Double teamRoundStatus;

    @Column(name = "TEAM_HOURS")
    @ColumnDefault("0.0")
    private Double teamHours;

    @Column(name = "ON_TRACK", columnDefinition = "integer default 0")
    private Integer onTrack;

    @Column(name = "PAYMENTS", columnDefinition = "integer default 0")
    private Integer payments;
}
