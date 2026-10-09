package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.auditlog.AuditLogService;
import com.ktk.dukappservice.data.paceteam.PaceTeamService;
import com.ktk.dukappservice.data.paceteamround.PaceTeamRoundService;
import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.roles.AppRoleService;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.UserDto;
import com.ktk.dukappservice.enums.AuditAction;
import com.ktk.dukappservice.enums.Permission;
import com.ktk.dukappservice.mapper.UserMapper;
import com.ktk.dukappservice.service.UserFamilyImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final PaceTeamService paceTeamService;
    private final UserMapper userMapper;
    private final SeasonService seasonService;
    private final PaceTeamRoundService paceTeamRoundService;
    private final AppRoleService appRoleService;
    private final AuditLogService auditLogService;
    private final MicrosoftService microsoftService;

    /** Who is told when a user asks for their account to be deleted. */
    @Value("${app.accountDeletion.adminEmail:support@bcc-ktk.org}")
    private String accountDeletionAdminEmail;

    public UserController(UserService userService, PaceTeamService paceTeamService, UserMapper modelMapper, SeasonService seasonService, PaceTeamRoundService paceTeamRoundService, UserFamilyImportService userFamilyImportService, AppRoleService appRoleService, AuditLogService auditLogService, MicrosoftService microsoftService) {
        this.microsoftService = microsoftService;
        this.appRoleService = appRoleService;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.paceTeamService = paceTeamService;
        this.userMapper = modelMapper;
        this.seasonService = seasonService;
        this.paceTeamRoundService = paceTeamRoundService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<User> userById = userService.findById(id);
        if (userById.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userById.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userById = userService.findByUsername(userDetails.getUsername());
        if (userById.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userById.get()));
        }
        return ResponseEntity.status(404).body("User not found");

    }

    @GetMapping("/myShare/{myShareId}")
    public ResponseEntity<?> getUserByMYShare(@PathVariable Long myShareId) {
        Optional<User> userByMyShareId = userService.findByMyShareId(myShareId);
        if (userByMyShareId.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userByMyShareId.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Optional<User> userByMyShareId = userService.findByUsername(username);
        if (userByMyShareId.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userByMyShareId.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    /**
     * The signed-in user asks for their account and all data related to it to be deleted. An admin gets an e-mail and
     * does it by hand; nothing is deleted here.
     */
    @PostMapping("/me/deletion-request")
    public ResponseEntity<?> requestAccountDeletion(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        try {
            microsoftService.sendAccountDeletionRequest(user, accountDeletionAdminEmail);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("The request could not be sent. Please e-mail " + accountDeletionAdminEmail + " instead.");
        }
        auditLogService.record(AuditAction.ACCOUNT_DELETION_REQUEST, "User", user.getId(), null);
        return ResponseEntity.ok("Deletion request sent");
    }

    /** The secret of the user's calendar subscription link (created on first use); the client builds the link. */
    @GetMapping("/me/calendar-token")
    public ResponseEntity<?> getCalendarToken(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of("token", userService.getOrCreateCalendarToken(userService.getCurrentUser(userDetails))));
    }

    /** Replaces the secret: the old link stops working. */
    @PostMapping("/me/calendar-token/reset")
    public ResponseEntity<?> resetCalendarToken(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of("token", userService.resetCalendarToken(userService.getCurrentUser(userDetails))));
    }

    @GetMapping("/me/family")
    public ResponseEntity<?> getFamily(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isPresent()) {
            return ResponseEntity.status(200).body(userService.findFamily(user.get().getFamilyId(), user.get().getId()).stream().map(userMapper::entityToDto));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestParam(value = "teamId", required = false) Long teamId,
                                      @RequestParam(value = "familyId", required = false) Long familyId,
                                      @RequestParam(value = "churchId", required = false) Long churchId,
                                      @RequestParam(value = "spouseId", required = false) Long spouseId,
                                      @RequestParam(value = "keyword", required = false) String keyword, Pageable pageable) {
        return ResponseEntity.status(200).body(userService.fetchByQuery(familyId, spouseId, teamId, churchId, keyword, pageable).map(userMapper::entityToDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> putUser(@Valid @RequestBody UserDto userDto, @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        if (!id.equals(userDto.getId())) {
            return ResponseEntity.status(400).body("Invalid userId");
        }
        if (!currentUser.getId().equals(id) && !currentUser.hasPermission(Permission.USER_MANAGE)) {
            return ResponseEntity.status(403).body("Permission denied:");
        }
        Optional<User> user = userService.findById(id);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + id);
        }
        // Family links decide who may act for whom (e.g. registering children for jobs), so only user managers set them.
        User entity = userMapper.dtoToEntity(userDto, user.get(), currentUser.hasPermission(Permission.USER_MANAGE));
        // Legacy clients change the role through the single `role` field: replace all roles with the matching one.
        if (userDto.getRole() != null && userDto.getRole() != entity.getRole() && currentUser.hasPermission(Permission.ROLE_MANAGE)) {
            Optional<AppRole> role = appRoleService.findByName(userDto.getRole().name());
            if (role.isPresent()) {
                ResponseEntity<?> error = checkLastAdmin(entity, Set.of(role.get()));
                if (error != null) {
                    return error;
                }
                logRolesChange(entity, Set.of(role.get()));
                entity.setRoles(new HashSet<>(Set.of(role.get())));
            }
        }
        return ResponseEntity.status(200).body(userMapper.entityToDto(userService.save(entity)));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> putUserRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        Optional<User> user = userService.findById(id);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user with id:" + id);
        }
        Set<AppRole> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            Optional<AppRole> role = appRoleService.findById(roleId);
            if (role.isEmpty()) {
                return ResponseEntity.status(400).body("No role with id:" + roleId);
            }
            roles.add(role.get());
        }
        if (roles.isEmpty()) {
            roles.add(appRoleService.getDefaultRole());
        }
        ResponseEntity<?> error = checkLastAdmin(user.get(), roles);
        if (error != null) {
            return error;
        }
        logRolesChange(user.get(), roles);
        user.get().setRoles(roles);
        return ResponseEntity.status(200).body(userMapper.entityToDto(userService.save(user.get())));
    }

    private void logRolesChange(User user, Set<AppRole> newRoles) {
        List<String> before = user.getRoles().stream().map(AppRole::getName).sorted().toList();
        List<String> after = newRoles.stream().map(AppRole::getName).sorted().toList();
        if (!before.equals(after)) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("user", user.getUsername());
            details.put("old", before);
            details.put("new", after);
            auditLogService.record(AuditAction.ROLES_CHANGE, "User", user.getId(), details);
        }
    }

    private ResponseEntity<?> checkLastAdmin(User user, Set<AppRole> newRoles) {
        boolean wasAdmin = user.getRoles().stream().anyMatch(AppRole::isAdminRole);
        boolean staysAdmin = newRoles.stream().anyMatch(AppRole::isAdminRole);
        if (wasAdmin && !staysAdmin) {
            AppRole admin = appRoleService.findByName(AppRole.ADMIN).orElseThrow();
            if (appRoleService.countUsersWithRole(admin) <= 1) {
                return ResponseEntity.status(409).body("Can't remove the last " + AppRole.ADMIN);
            }
        }
        return null;
    }

    @GetMapping("/setPaceTeams")
    @PreAuthorize("hasAuthority('PACE_TEAM_MANAGE')")
    public ResponseEntity<?> setPaceTeams() {
        paceTeamRoundService.createTeamRounds();
        return ResponseEntity.status(200).body("Pace Teams set");
    }

}
