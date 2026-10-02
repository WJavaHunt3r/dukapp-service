package com.ktk.dukappservice.data.roles;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements ApplicationRunner {
    private final AppRoleService appRoleService;

    public RoleInitializer(AppRoleService appRoleService) {
        this.appRoleService = appRoleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        appRoleService.initializeRoles();
    }
}
