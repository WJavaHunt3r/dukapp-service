package com.ktk.dukappservice.data.activityitems;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ACTIVITY_ITEMS")
@FieldNameConstants
public class ActivityItem extends BaseEntity<ActivityItem, Long> {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "ACTIVITY")
    private Activity activity;

    @Size(max = 150)
    @Column(name = "DESCRIPTION", length = 150)
    @NotNull
    private String description;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "USERS")
    private User user;

    @ManyToOne
    @JoinColumn(name = "CREATE_USER")
    private User createUser;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "CREATE_DATE_TIME")
    private LocalDateTime createDateTime;

    @Column(name = "TRANSACTION_TYPE")
    @Enumerated(EnumType.STRING)
    @NotNull
    private TransactionType transactionType;

    @Column(name = "ACCOUNT")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Account account;

    @Column(name = "HOURS", columnDefinition = "float8 default 0")
    private double hours;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "ROUNDS")
    private Round round;
}
