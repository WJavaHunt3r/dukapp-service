package com.ktk.dukappservice.data.paceuserround;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "PACE_USER_ROUNDS", uniqueConstraints =
        {@UniqueConstraint(name = "UniqueRoundAndUser", columnNames = {"ROUNDS", "USERS"})})
@FieldNameConstants
public class PaceUserRound extends BaseEntity<PaceUserRound, Long> {

    @JoinColumn(name = "USERS")
    @ManyToOne
    @NotNull
    private User user;

    @JoinColumn(name = "ROUNDS")
    @ManyToOne
    @NotNull
    private Round round;

    @Column(name = "SAMVIRK_PAYMENTS", columnDefinition = "integer default 0")
    private Integer samvirkPayments = 0;

    @Column(name = "ROUND_COINS")
    @ColumnDefault("0.0")
    private Double roundCoins;

    @Column(name = "ROUND_CREDITS", columnDefinition = "integer default 0")
    private Integer roundCredits;

    @Column(name = "BMM_PERFECT_WEEK_POINTS")
    @ColumnDefault("0.0")
    private double bMMPerfectWeekPoints;

    @Column(name = "ROUND_MYSHARE_GOAL", columnDefinition = "integer default 0")
    private Integer roundMyShareGoal;

    @Column(name = "ON_TRACK", columnDefinition = "boolean default false")
    private boolean onTrack;

    @Column(name = "MYSHARE_ON_TRACK_POINTS", columnDefinition = "boolean default false")
    private boolean myShareOnTrackPoints;

    public void addSamvirkPayment(Integer payment) {
        samvirkPayments += payment;
    }

    public void addRoundCoins(double coins) {
        roundCoins += coins;
    }

    public void addBMMPerfectWeekPoints(double points) {
        bMMPerfectWeekPoints += points;
    }
}
