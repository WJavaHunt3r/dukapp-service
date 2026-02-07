package com.ktk.dukappservice.data.userrounds;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "USER_ROUNDS")
@FieldNameConstants
public class UserRound extends BaseEntity<UserRound, Long> {

    @JoinColumn(name = "USERS")
    @ManyToOne
    @NotNull
    private User user;

    @JoinColumn(name = "ROUNDS")
    @ManyToOne
    @NotNull
    private Round round;

    @Column(name = "MYSHARE_ON_TRACK_POINTS", columnDefinition = "boolean default false")
    private boolean myShareOnTrackPoints;

    @Column(name = "SAMVIRK_ON_TRACK_POINTS", columnDefinition = "boolean default false")
    private boolean samvirkOnTrackPoints;

    @Column(name = "SAMVIRK_POINTS", columnDefinition = "float8 default 0")
    private double samvirkPoints;

    @Column(name = "SAMVIRK_PAYMENTS", columnDefinition = "integer default 0")
    private Integer samvirkPayments = 0;

    @Column(name = "ROUND_POINTS", columnDefinition = "float8 default 0")
    private double roundPoints;

    @Column(name = "BMM_PERFECT_WEEK_POINTS", columnDefinition = "float8 default 0")
    private double bMMPerfectWeekPoints;

    public void addSamvirkPoints(double points) {
        samvirkPoints += points;
    }

    public void addSamvirkPayment(Integer payment) {
        samvirkPayments += payment;
    }

    public void addRoundPoints(double points) {
        roundPoints += points;
    }

    public void addBMMPerfectWeekPoints(double points) {
        bMMPerfectWeekPoints += points;
    }

}
