package com.ktk.dukappservice.data.rounds;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.seasons.Season;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ROUNDS")
@FieldNameConstants
public class Round extends BaseEntity<Round, Long> {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "START_DATE")
    private LocalDateTime startDateTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "END_DATE")
    private LocalDateTime endDateTime;

    @Column(name = "MYSHARE_GOAL")
    @NotNull
    private Double myShareGoal;

    @Column(name = "LOCAL_MYSHARE_GOAL")
    private Double localMyShareGoal;

    @Column(name = "SAMVIRK_GOAL")
    @NotNull
    private Integer samvirkGoal;

    @Column(name = "ROUND_NUMBER")
    @NotNull
    private Integer roundNumber;

    @Column(name = "SAMVIRK_CHURCH_GOAL", columnDefinition = "integer default 0")
    @NotNull
    private Integer samvirkChurchGoal;

    @Column(name = "SAMVIRK_MAX_POINTS")
    @ColumnDefault("0.0")
    private double samvirkMaxPoints;

    @Column(name = "SAMVIRK_ON_TRACK_POINTS")
    @ColumnDefault("0.0")
    private double samvirkOnTrackPoints;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "FREEZE_DATE_TIME")
    private LocalDateTime freezeDateTime;

    @JoinColumn(name = "SEASON")
    @ManyToOne
    private Season season;

    @Column(name = "USER_ROUNDS_CREATED", columnDefinition = "boolean default false")
    private Boolean userRoundsCreated;

    @Column(name = "ACTIVE_ROUND", columnDefinition = "boolean default true")
    private Boolean activeRound;

    @JoinColumn(name = "WINNER_TEAM")
    @ManyToOne
    private PaceTeam winnerTeam;


}
