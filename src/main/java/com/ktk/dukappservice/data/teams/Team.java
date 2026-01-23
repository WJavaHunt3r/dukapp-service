package com.ktk.dukappservice.data.teams;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.enums.TeamColor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "TEAMS")
@FieldNameConstants
public class Team extends BaseEntity<Team, Long> {

    @Column(name="TEAM_LEADER_ID")
    private Long teamLeaderMyShareId;

    @Column(name = "TEAM_COLOR")
    @Enumerated(EnumType.STRING)
    @NotNull
    private TeamColor color;

    @Column(name = "POINTS")
    private double points;

}
