package com.ktk.dukappservice.data.paceteam;

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

@Getter
@Setter
@Entity
@Table(name = "PACE_TEAMS")
@FieldNameConstants
public class PaceTeam extends BaseEntity<PaceTeam, Long> {

    @Column(name="TEAM_LEADER_ID")
    private Long teamLeaderId;

    @Column(name = "COINS", columnDefinition = "float8 default 0")
    private double coins;

    @Size(max = 30)
    @Column(name = "TEAM_NAME", length = 30)
    @NotNull
    @NotEmpty
    private String teamName;

    @Column(name = "ACTIVE", columnDefinition = "boolean default true")
    private Boolean active;

    @Column(name = "COLOR", length = 10)
    @Size(max = 10)
    private String color;

    @Column(name = "START_COLOR", length = 10)
    @Size(max = 10)
    private String startColor;

    @Column(name = "END_COLOR", length = 10)
    @Size(max = 10)
    private String endColor;

    @Column(name = "ICON_ASSET_PATH")
    @Size(max = 50)
    private String iconAssetPath;

    public void addCoins(Double coins){
        this.coins += coins;
    }
}
