package com.ktk.dukappservice.security;

import com.ktk.dukappservice.data.users.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        return userService.findByEmailOrUsername(username, username).map(u ->
                User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .authorities(authoritiesOf(u))
                        .build()
        ).orElseThrow(() -> new UsernameNotFoundException("User does not exist with the given username: " + username));
    }

    /**
     * Every permission becomes an authority of the same name (for {@code hasAuthority(...)}), and every role
     * becomes {@code ROLE_<name>} (for {@code hasRole(...)}).
     */
    public static List<GrantedAuthority> authoritiesOf(com.ktk.dukappservice.data.users.User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName())));
        user.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.name())));
        return authorities;
    }
}