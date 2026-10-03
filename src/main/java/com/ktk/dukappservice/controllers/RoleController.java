package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.auditlog.AuditLogService;
import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.roles.AppRoleService;
import com.ktk.dukappservice.dto.AppRoleDto;
import com.ktk.dukappservice.enums.AuditAction;
import com.ktk.dukappservice.enums.Permission;
import com.ktk.dukappservice.enums.Role;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final AppRoleService appRoleService;
    private final AuditLogService auditLogService;

    public RoleController(AppRoleService appRoleService, AuditLogService auditLogService) {
        this.appRoleService = appRoleService;
        this.auditLogService = auditLogService;
    }

    @GetMapping()
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(StreamSupport.stream(appRoleService.findAll().spliterator(), false).map(this::entityToDto).toList());
    }

    @GetMapping("/permissions")
    public ResponseEntity<?> getPermissions() {
        return ResponseEntity.ok(Permission.values());
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> postRole(@Valid @RequestBody AppRoleDto dto) {
        if (appRoleService.findByName(dto.getName()).isPresent()) {
            return ResponseEntity.status(409).body("Role already exists: " + dto.getName());
        }
        return ResponseEntity.ok(entityToDto(appRoleService.save(dtoToEntity(dto, new AppRole()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> putRole(@Valid @RequestBody AppRoleDto dto, @PathVariable Long id) {
        Optional<AppRole> role = appRoleService.findById(id);
        if (role.isEmpty() || !id.equals(dto.getId())) {
            return ResponseEntity.status(400).body("Invalid roleId");
        }
        if (role.get().isAdminRole()) {
            return ResponseEntity.status(409).body("The " + AppRole.ADMIN + " role can't be modified");
        }
        // Built-in roles are re-created by name on startup and mirrored into the legacy role column
        if (isBuiltIn(role.get()) && !role.get().getName().equals(dto.getName())) {
            return ResponseEntity.status(409).body("Built-in role " + role.get().getName() + " can't be renamed");
        }
        Optional<AppRole> sameName = appRoleService.findByName(dto.getName());
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            return ResponseEntity.status(409).body("Role already exists: " + dto.getName());
        }
        // Permission changes are a collection update, which the entity audit listener doesn't see
        EnumSet<Permission> before = role.get().getPermissions().isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(role.get().getPermissions());
        AppRole saved = appRoleService.save(dtoToEntity(dto, role.get()));
        if (!before.equals(saved.getPermissions())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("role", saved.getName());
            details.put("old", before);
            details.put("new", saved.getPermissions());
            auditLogService.record(AuditAction.ROLE_PERMISSIONS_CHANGE, "AppRole", saved.getId(), details);
        }
        return ResponseEntity.ok(entityToDto(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        Optional<AppRole> role = appRoleService.findById(id);
        if (role.isEmpty()) {
            return ResponseEntity.status(404).body("No role with id: " + id);
        }
        if (isBuiltIn(role.get())) {
            return ResponseEntity.status(409).body("Built-in role " + role.get().getName() + " can't be deleted");
        }
        long users = appRoleService.countUsersWithRole(role.get());
        if (users > 0) {
            return ResponseEntity.status(409).body("Role is still assigned to " + users + " users");
        }
        appRoleService.deleteById(id);
        return ResponseEntity.ok("Delete successful");
    }

    private static boolean isBuiltIn(AppRole role) {
        return Arrays.stream(Role.values()).anyMatch(r -> r.name().equals(role.getName()));
    }

    private AppRole dtoToEntity(AppRoleDto dto, AppRole role) {
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.getPermissions().clear();
        if (dto.getPermissions() != null) {
            role.getPermissions().addAll(dto.getPermissions());
        }
        return role;
    }

    private AppRoleDto entityToDto(AppRole role) {
        AppRoleDto dto = new AppRoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPermissions(role.getPermissions().isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(role.getPermissions()));
        dto.setUserCount(appRoleService.countUsersWithRole(role));
        return dto;
    }
}
