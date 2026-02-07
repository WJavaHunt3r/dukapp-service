package com.ktk.dukappservice.security;

import com.ktk.dukappservice.data.users.UserService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

@Service
public class DukAppDetailsManager implements UserDetailsManager, UserDetailsPasswordService {
    private final UserService userService;

    public DukAppDetailsManager(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newEncodedPassword) {
        userService.findByUsername(user.getUsername()).ifPresent(u -> {
            u.setPassword(newEncodedPassword);
            userService.save(u);
        });
        return user;
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
        return userService.findByUsername(username).map(u ->
                User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .build()
        ).orElseThrow(() -> new UsernameNotFoundException("User does not exist with the given username: " + username));
    }


}