package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.ChangePasswordDto;
import com.ktk.dukappservice.dto.LoginDto;
import com.ktk.dukappservice.dto.ResetPasswordDto;
import com.ktk.dukappservice.dto.SendNewPasswordDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.security.SecurityUtils;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import com.microsoft.graph.models.odataerrors.ODataError;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private UserService userService;
    private MicrosoftService microsoftService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, MicrosoftService microsoftService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.microsoftService = microsoftService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword()));

        return ResponseEntity.status(200).body(authentication.getPrincipal());
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto dto) {

        Optional<User> user = userService.findByUsername(dto.getUsername());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("Username not found");
        }

        if (!user.get().getPassword().equals(dto.getOldPassword())) {
            return ResponseEntity.status(400).body("Wrong password");
        }

        if (user.get().getPassword().equals(dto.getNewPassword())) {
            return ResponseEntity.status(400).body("New password cannot be same as old password");
        }

        user.get().setPassword(dto.getNewPassword());
        user.get().setChangedPassword(true);

        userService.save(user.get());

        return ResponseEntity.status(200).body("Password change successful");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto) {

        Optional<User> user = userService.findById(dto.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id: " + dto.getUserId());
        }

        Optional<User> changer = userService.findById(dto.getChangerId());
        if (changer.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id: " + dto.getChangerId());
        }
        if (changer.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(400).body("User is not admin");
        }

        user.get().setPassword(SecurityUtils.encryptSecret(user.get().getUsername()));
        user.get().setChangedPassword(false);

        userService.save(user.get());

        return ResponseEntity.status(200).body("Password reset successful");
    }

    @PostMapping("/sendNewPassword")
    public ResponseEntity<?> sendNewPassword(@RequestBody SendNewPasswordDto newPasswordDto) {

        String email = newPasswordDto.getUsername();
        Optional<User> user = userService.findByUsername(email).or(() -> userService.findByEmail(email));
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with email or username: " + email);
        }

        user.get().setPassword(SecurityUtils.encryptSecret(user.get().getUsername()));
        user.get().setChangedPassword(false);

        userService.save(user.get());
        try {
            microsoftService.sendNewPassword(user.get(), user.get().getUsername());
        } catch (Exception e) {
            if (e instanceof ODataError) {
                ((ODataError) e).getError().getCode();
                ((ODataError) e).getError().getMessage();
            }
            return ResponseEntity.status(500).body(e.toString());
        }

        return ResponseEntity.status(200).body("Password reset successful");
    }

}
