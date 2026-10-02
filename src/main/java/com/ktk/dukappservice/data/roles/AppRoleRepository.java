package com.ktk.dukappservice.data.roles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Not exported via Spring Data REST: role changes must go through RoleController's permission checks.
@Repository
@RepositoryRestResource(exported = false)
public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByName(String name);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = ?1")
    long countUsersWithRole(AppRole role);
}
