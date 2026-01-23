package com.ktk.dukappservice.data.userchallenges;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.challenges.Challenges;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "USER_CHALLENGES", uniqueConstraints =
        {@UniqueConstraint(name = "UniqueChallengeAndUser", columnNames = {"CHALLENGE", "USERS"})})
@FieldNameConstants
public class UserChallenges extends BaseEntity<UserChallenges, Long> {

    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    @NotNull
    @JoinColumn(name = "CHALLENGE")
    @ManyToOne
    private Challenges challenge;

    @NotNull
    @Column(name = "COMPLETED", columnDefinition = "boolean default false")
    private boolean completed;

    @Column(name = "COMPLETION_DATE")
    private LocalDate completionDate;
}
