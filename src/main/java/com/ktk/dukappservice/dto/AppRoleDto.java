package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.Permission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
public class AppRoleDto {
    private Long id;

    @NotEmpty
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String description;

    private Set<Permission> permissions = new HashSet<>();

    private long userCount;
}
