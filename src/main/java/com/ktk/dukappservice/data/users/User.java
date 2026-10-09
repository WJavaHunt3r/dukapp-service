package com.ktk.dukappservice.data.users;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.church.Church;
import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.enums.Gender;
import com.ktk.dukappservice.enums.Permission;
import com.ktk.dukappservice.enums.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "USERS", indexes = {
        @Index(name = "idx_username", columnList = "USERNAME", unique = true)
})
@FieldNameConstants
public class User extends BaseEntity<User, Long> {

    /** Stored when a name is unknown (e.g. Google didn't send it); see {@link #isProfileIncomplete()}. */
    public static final String NAME_PLACEHOLDER = "-";

    @Size(max = 50)
    @Column(name = "FIRSTNAME", length = 50)
    @NotNull
    @NotEmpty
    private String firstname;

    @Size(max = 50)
    @Column(name = "LASTNAME", length = 50)
    @NotNull
    @NotEmpty
    private String lastname;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @Column(name = "GENDER", length = 10)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @ManyToOne
    @JoinColumn(name = "TEAMS")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "PACE_TEAMS")
    private PaceTeam paceTeam;

    @ManyToOne
    @JoinColumn(name = "CHURCH")
    private Church church;

    @Size(max = 30)
    @Column(name = "USERNAME", length = 30)
    @NotNull
    @NotEmpty
    private String username;

    @Size(max = 1000)
    @Column(name = "PASSWORD", length = 1000)
    @NotNull
    @NotEmpty
    private String password;

    /**
     * Legacy single role, kept in sync with {@link #roles} for older clients and the booking system.
     * Use {@link #roles} / {@link #hasPermission(Permission)} for authorization.
     */
    @Column(name = "ROLE")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "USER_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    @BatchSize(size = 50)
    private Set<AppRole> roles = new HashSet<>();

    @Column(name = "MYSHARE_ID")
    private Long myShareID;

    @Column(name = "BASE_MYSHARE_CREDIT")
    private Integer baseMyShareCredit;

    @Column(name = "CURRENT_MYSHARE_CREDIT")
    private Integer currentMyShareCredit;

    @Column(name = "SAMVIRK_PAYMENTS")
    private Integer samvirkPayments = 0;

    @Column(name = "POINTS", columnDefinition = "float8 default 0")
    private double points;

    @Column(name = "CHANGED_PASSWORD")
    private boolean changedPassword;

    @JoinColumn(name = "SPOUSE_ID")
    private Long spouseId;

    @Column(name = "FAMILY_ID")
    private Long familyId;

    @Column(name = "PHONE_NUMBER")
    private Long phoneNumber;

    @Size(max = 200)
    @Column(name = "EMAIL", length = 200)
    private String email;

    @Column(name = "BUFE_ID")
    private Long bufeId;

    /** Secret in the user's calendar subscription link (see CalendarController); null until they ask for one. */
    @Column(name = "CALENDAR_TOKEN", length = 64)
    private String calendarToken;

    public String getFullName() {
        return lastname + " " + firstname;
    }

    /** True until the user has replaced a placeholder first/last name with their real one. */
    public boolean isProfileIncomplete() {
        return NAME_PLACEHOLDER.equals(firstname) || NAME_PLACEHOLDER.equals(lastname);
    }

    public void setRoles(Set<AppRole> roles) {
        this.roles = roles;
        this.role = deriveLegacyRole(roles);
    }

    public Set<Permission> getPermissions() {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        roles.forEach(r -> permissions.addAll(r.getPermissions()));
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return roles.stream().anyMatch(r -> r.getPermissions().contains(permission));
    }

    private static Role deriveLegacyRole(Set<AppRole> roles) {
        for (Role legacy : new Role[]{Role.ADMIN, Role.TEAM_LEADER, Role.HELPER}) {
            if (roles.stream().anyMatch(r -> legacy.name().equals(r.getName()))) {
                return legacy;
            }
        }
        return Role.USER;
    }

    public int getAge() {
        if(birthDate == null){
            return 0;
        }
        return getAgeAtDate(LocalDate.now());
    }

    public int getAgeAtDate(LocalDate date) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, date).getYears();
    }
}
