package com.ktk.dukappservice.data.roles;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.enums.Permission;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "ROLES", indexes = {
        @Index(name = "idx_role_name", columnList = "NAME", unique = true)
})
@FieldNameConstants
public class AppRole extends BaseEntity<AppRole, Long> {

    /** Name of the built-in role that always holds every permission and can't be deleted. */
    public static final String ADMIN = "ADMIN";

    /** Name of the role given to newly registered users. */
    public static final String DEFAULT = "USER";

    @Size(max = 50)
    @Column(name = "NAME", length = 50)
    @NotNull
    @NotEmpty
    private String name;

    @Size(max = 255)
    @Column(name = "DESCRIPTION")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ROLE_PERMISSIONS", joinColumns = @JoinColumn(name = "ROLE_ID"))
    @Column(name = "PERMISSION", length = 50)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = new HashSet<>();

    public boolean isAdminRole() {
        return ADMIN.equals(name);
    }

    public static AppRole of(String name, String description, Set<Permission> permissions) {
        AppRole role = new AppRole();
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions.isEmpty() ? new HashSet<>() : EnumSet.copyOf(permissions));
        return role;
    }
}
