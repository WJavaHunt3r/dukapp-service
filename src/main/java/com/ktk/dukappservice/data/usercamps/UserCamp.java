package com.ktk.dukappservice.data.usercamps;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.camps.Camp;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "USER_CAMPS", uniqueConstraints =
        {@UniqueConstraint(name = "UniqueCampAndUser", columnNames = {"CAMP", "USERS"})})
@FieldNameConstants
public class UserCamp extends BaseEntity<UserCamp, Long> {

    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    @NotNull
    @JoinColumn(name = "CAMP")
    @ManyToOne
    private Camp camp;

    @Column(name = "PRICE", columnDefinition = "integer default 0")
    @NotNull
    private Integer price;

    @Column(name = "PARTICIPATES", columnDefinition = "boolean default false")
    private boolean participates;
}
