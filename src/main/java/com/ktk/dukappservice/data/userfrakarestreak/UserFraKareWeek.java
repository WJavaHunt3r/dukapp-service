package com.ktk.dukappservice.data.userfrakarestreak;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.frakareweek.FraKareWeek;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "USER_FRA_KARE_WEEK", uniqueConstraints =
        {@UniqueConstraint(name = "UniqueFraKareWeekAndUser", columnNames = {"FRA_KARE_WEEK", "USERS"})})
@FieldNameConstants
public class UserFraKareWeek extends BaseEntity<UserFraKareWeek, Long> {

    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    @NotNull
    @JoinColumn(name = "FRA_KARE_WEEK")
    @ManyToOne
    private FraKareWeek fraKareWeek;

    @Column(name = "LISTENED", columnDefinition = "boolean default false")
    @NotNull
    private boolean listened;

    @Column(name = "TRANSACTION_ITEM_ID")
    private Long transactionItemId;
}
