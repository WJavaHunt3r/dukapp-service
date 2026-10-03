package com.ktk.dukappservice.data.roles;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserRepository;
import com.ktk.dukappservice.enums.Permission;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AppRoleService extends BaseService<AppRole, Long> {
    private static final Logger LOG = LoggerFactory.getLogger(AppRoleService.class);

    private final AppRoleRepository appRoleRepository;
    private final UserRepository userRepository;

    public AppRoleService(AppRoleRepository appRoleRepository, UserRepository userRepository) {
        this.appRoleRepository = appRoleRepository;
        this.userRepository = userRepository;
    }

    public Optional<AppRole> findByName(String name) {
        return appRoleRepository.findByName(name);
    }

    public AppRole getDefaultRole() {
        return appRoleRepository.findByName(AppRole.DEFAULT)
                .orElseThrow(() -> new IllegalStateException("Default role " + AppRole.DEFAULT + " missing"));
    }

    public long countUsersWithRole(AppRole role) {
        return appRoleRepository.countUsersWithRole(role);
    }

    /**
     * Creates the built-in roles if they don't exist yet (existing roles are never overwritten, so admin edits survive
     * restarts), makes sure ADMIN holds every permission, and gives users that only have the legacy single
     * {@link Role} column the matching new role.
     */
    @Transactional
    public void initializeRoles() {
        Map<String, AppRole> defaults = new LinkedHashMap<>();
        defaults.put(Role.ADMIN.name(), AppRole.of(Role.ADMIN.name(), "Full access", EnumSet.allOf(Permission.class)));
        defaults.put(Role.TEAM_LEADER.name(), AppRole.of(Role.TEAM_LEADER.name(), "Team leader",
                EnumSet.of(Permission.TRANSACTION_MANAGE, Permission.ACTIVITY_REGISTER, Permission.FRAKARE_MANAGE, Permission.BOOKING_ADMIN)));
        defaults.put(Role.HELPER.name(), AppRole.of(Role.HELPER.name(), "Helper",
                EnumSet.of(Permission.TRANSACTION_MANAGE, Permission.ACTIVITY_REGISTER, Permission.BOOKING_ADMIN)));
        defaults.put(Role.USER.name(), AppRole.of(Role.USER.name(), "Regular user", EnumSet.noneOf(Permission.class)));

        Map<String, AppRole> roles = new HashMap<>();
        defaults.forEach((name, role) -> roles.put(name, appRoleRepository.findByName(name).orElseGet(() -> {
            LOG.info("Creating role {}", name);
            return appRoleRepository.save(role);
        })));

        AppRole admin = roles.get(AppRole.ADMIN);
        if (!admin.getPermissions().containsAll(EnumSet.allOf(Permission.class))) {
            admin.getPermissions().addAll(EnumSet.allOf(Permission.class));
            appRoleRepository.save(admin);
        }

        int migrated = 0;
        for (User user : userRepository.findAll()) {
            if (user.getRoles().isEmpty()) {
                Role legacy = user.getRole() == null ? Role.USER : user.getRole();
                user.setRoles(new HashSet<>(Set.of(roles.get(legacy.name()))));
                userRepository.save(user);
                migrated++;
            }
        }
        if (migrated > 0) {
            LOG.info("Assigned roles to {} users based on their legacy role", migrated);
        }
    }

    @Override
    protected JpaRepository<AppRole, Long> getRepository() {
        return appRoleRepository;
    }

    @Override
    public Class<AppRole> getEntityClass() {
        return AppRole.class;
    }

    @Override
    public AppRole createEntity() {
        return new AppRole();
    }
}
