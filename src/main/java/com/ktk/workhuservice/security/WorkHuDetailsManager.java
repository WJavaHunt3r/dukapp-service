package com.ktk.workhuservice.security;

import com.ktk.workhuservice.data.users.UserService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;

public class WorkHuDetailsManager implements UserDetailsManager, UserDetailsPasswordService {
    private UserService userService;

    public  WorkHuDetailsManager(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails updatePassword(UserDetails userDetails, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createUser(UserDetails userDetails) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateUser(UserDetails userDetails) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean userExists(String username) {
        return userService.findByUsername(username).isPresent();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userService.findByUsername(username).map(u -> {
            User.UserBuilder builder = User.builder();
            builder.username(u.getUsername());
            builder.password(u.getPassword());
            builder.roles(u.getRole().name());
            return builder.build();
        }).orElseThrow(() -> new UsernameNotFoundException("User does not exist with the given username: " + username));
    }
}