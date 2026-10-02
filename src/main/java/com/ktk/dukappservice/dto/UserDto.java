package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.enums.Gender;
import com.ktk.dukappservice.enums.Permission;
import com.ktk.dukappservice.enums.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
public class UserDto {
    private Long id;

    private String firstname;

    private String lastname;

    /** First or last name is still a placeholder: the frontend should ask the user to complete their profile. */
    private boolean profileIncomplete;

    private LocalDate birthDate;

    private Gender gender;

    private PaceTeam paceTeam;

    /** Legacy single role (read-only, derived from {@link #roleNames}). */
    private Role role;

    /** Names of all roles assigned to the user. Change them via PUT /api/user/{id}/roles. */
    private List<String> roleNames;

    /** Effective permissions (union over all roles), for the frontend to show/hide features. */
    private Set<Permission> permissions;

    private long myShareID;

    private Integer baseMyShareCredit;

    private Integer currentMyShareCredit;

    private boolean changedPassword;

    private Long spouseId;

    private Long familyId;

    private Long phoneNumber;

    private String email;

    private Long bufeId;

    private double points;
}
