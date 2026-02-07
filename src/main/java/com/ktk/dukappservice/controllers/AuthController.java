package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.ChangePasswordDto;
import com.ktk.dukappservice.dto.LoginDto;
import com.ktk.dukappservice.dto.ResetPasswordDto;
import com.ktk.dukappservice.dto.SendNewPasswordDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.security.DukAppDetailsManager;
import com.ktk.dukappservice.security.JwtResponse;
import com.ktk.dukappservice.security.JwtUtils;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import com.microsoft.graph.models.odataerrors.ODataError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MicrosoftService microsoftService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    private final DukAppDetailsManager detailsManager;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, MicrosoftService microsoftService, JwtUtils jwtUtils, PasswordEncoder passwordEncoder, DukAppDetailsManager detailsManager) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.microsoftService = microsoftService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.detailsManager = detailsManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateToken(loginDto.getUsername());

        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // 5. Return the DTO
        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), roles));
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto dto) {
        User user = userService.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // ALWAYS use .matches(plainText, hashedStoredValue)
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body("Old password incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setChangedPassword(true);
        userService.save(user);

        return ResponseEntity.ok("Password updated to BCrypt format");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto) {

        User changer = userService.findById(dto.getChangerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

        if (changer.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: Admin role required");
        }

        User targetUser = userService.findById(dto.getUserId()).orElseThrow();

        // 1. Generate the new hash
        String newHash = passwordEncoder.encode(targetUser.getUsername());

        UserDetails userDetails = detailsManager.loadUserByUsername(targetUser.getUsername());
        detailsManager.updatePassword(userDetails, newHash);
        return ResponseEntity.ok("Password reset successfully");

    }

    @PostMapping("/sendNewPassword")
    public ResponseEntity<?> sendNewPassword(@RequestBody SendNewPasswordDto newPasswordDto) {

        String email = newPasswordDto.getUsername();
        Optional<User> user = userService.findByUsername(email).or(() -> userService.findByEmail(email));
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with email or username: " + email);
        }

        String encodedPassword = passwordEncoder.encode(user.get().getUsername());
        user.get().setPassword(encodedPassword);
        user.get().setChangedPassword(false);

        userService.save(user.get());
        try {
            microsoftService.sendNewPassword(user.get(), user.get().getUsername());
        } catch (Exception e) {
            if (e instanceof ODataError) {
                Objects.requireNonNull(((ODataError) e).getError()).getCode();
                ((ODataError) e).getError().getMessage();
            }
            return ResponseEntity.status(500).body(e.toString());
        }

        return ResponseEntity.status(200).body("Password reset successful");
    }

}
