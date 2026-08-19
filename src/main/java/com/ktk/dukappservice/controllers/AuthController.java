package com.ktk.dukappservice.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.*;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.security.*;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import com.microsoft.graph.models.odataerrors.ODataError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MicrosoftService microsoftService;
    private final JwtUtils jwtUtils;
    private final BookingJwtUtils bookingJwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final DukAppDetailsManager detailsManager;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          MicrosoftService microsoftService,
                          JwtUtils jwtUtils,
                          BookingJwtUtils bookingJwtUtils, PasswordEncoder passwordEncoder, DukAppDetailsManager detailsManager, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.microsoftService = microsoftService;
        this.jwtUtils = jwtUtils;
        this.bookingJwtUtils = bookingJwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.detailsManager = detailsManager;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        // Note: loginDto.getUsername() here can be either email or username
        // Spring Security will call your loadUserByUsername, which we updated earlier
        // to check both fields in the database.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Fetch the actual username from the principal (in case they logged in with email)
        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        String jwt = jwtUtils.generateToken(userDetails.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(new JwtResponse(jwt, refreshToken.getToken(), userDetails.getUsername(), roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDto registerDto) {
        // 1. Validation logic
        if (userService.findByEmail(registerDto.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already in use!");
        }

        // 2. Create and Save User
        User user = new User();
        String baseUsername = normalizeUsername(registerDto.getLastname(), registerDto.getFirstname());
        String finalUsername = baseUsername;
        int counter = 1;
        while (userService.findByUsername(finalUsername).isPresent()) {
            finalUsername = baseUsername + counter;
            counter++;
        }
        user.setUsername(finalUsername);
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setRole(Role.USER); // Default role
        user.setChangedPassword(true);
        user.setFirstname(registerDto.getFirstname());
        user.setLastname(registerDto.getLastname());

        userService.save(user);

        // 3. Optional: Return a JWT immediately so they don't have to log in right after registering
        String jwt = jwtUtils.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
        return ResponseEntity.ok(new JwtResponse(jwt, refreshToken.getToken(), user.getUsername(), List.of(Role.USER.name())));
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody ChangePasswordDto dto) {
        // Lookup by username or email
        Optional<User> userById = userService.findByUsername(userDetails.getUsername());
        if (userById.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        User user = userById.get();
//        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Old password incorrect");
//        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setChangedPassword(true);
        userService.save(user);

        return ResponseEntity.ok("Password updated successfully");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto) {
        User changer = userService.findById(dto.getChangerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (changer.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: Admin role required");
        }

        User targetUser = userService.findById(dto.getUserId()).orElseThrow();
        String newHash = passwordEncoder.encode(targetUser.getUsername());
        UserDetails userDetails = detailsManager.loadUserByUsername(targetUser.getUsername());
        detailsManager.updatePassword(userDetails, newHash);
        return ResponseEntity.ok("Password reset successfully");
    }

    @GetMapping("/bookingToken")
    public ResponseEntity<?> resetPassword(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userById = userService.findByUsername(userDetails.getUsername());
        if (userById.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        String token = bookingJwtUtils.generateToken(userById.get());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/sendNewPassword")
    public ResponseEntity<?> sendNewPassword(@RequestBody SendNewPasswordDto newPasswordDto) {

        String email = newPasswordDto.getUsername();
        Optional<User> user = userService.findByUsername(email).or(() -> userService.findByEmail(email));
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with email or username: " + email);
        }
        String newPassword = PasswordUtils.generateRandomPassword(10);
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.get().setPassword(encodedPassword);
        user.get().setChangedPassword(false);
        userService.save(user.get());

        try {
            microsoftService.sendNewPassword(user.get(), newPassword);
        } catch (Exception e) {
            if (e instanceof ODataError) {
                Objects.requireNonNull(((ODataError) e).getError()).getCode();
                ((ODataError) e).getError().getMessage();
            }
            return ResponseEntity.status(500).body(e.toString());
        }
        return ResponseEntity.status(200).body("Password reset successful");
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        String idTokenString = payload.get("idToken");

        // 1. Setup Verifier
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList("470140408680-vvsu3rjroghr7suq603r4eek5lec5bds.apps.googleusercontent.com"))
                .build();

        try {
            // 2. Verify the token
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();

                // Get user info from Google payload
                String email = googlePayload.getEmail();
                String firstName = (String) googlePayload.get("given_name");
                String lastName = (String) googlePayload.get("family_name");

                // 3. Find or Create the user in your database
                User user = userService.findByEmail(email).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstname(firstName);
                    newUser.setLastname(lastName);
                    newUser.setMyShareID(null);
                    // Generate username using your normalization logic
                    String baseUsername = normalizeUsername(lastName, firstName);
                    String finalUsername = baseUsername;
                    int counter = 1;
                    while (userService.findByUsername(finalUsername).isPresent()) {
                        finalUsername = baseUsername + counter;
                        counter++;
                    }
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Random pass
                    newUser.setRole(Role.USER);
                    return userService.save(newUser);
                });

                // 4. Generate YOUR app's JWT
                String jwt = jwtUtils.generateToken(user.getUsername());
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
                return ResponseEntity.ok(new JwtResponse(jwt, refreshToken.getToken(), user.getUsername(), List.of(user.getRole().name())));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Google Token");
        }
        return ResponseEntity.status(401).body("Google Authentication Failed");
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.refreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateToken(user.getUsername());
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token is not in database!"));
    }

    @GetMapping("/isAlive")
    public ResponseEntity<?> isAlive() {
        return ResponseEntity.ok("OK");
    }

    public String normalizeUsername(String firstName, String lastName) {
        String input = (lastName + firstName).toLowerCase();

        // 1. Normalize Unicode (separates 'é' into 'e' + '´')
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 2. Remove all non-ASCII characters (the accents we just separated)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");

        // 3. Optional: Remove anything else that isn't a-z or 0-9
        // (like spaces, hyphens, or special symbols)
        return result.replaceAll("[^a-z0-9]", "");
    }

    public record TokenRefreshRequest(String refreshToken) {
    }

    public record TokenRefreshResponse(String accessToken, String refreshToken, String tokenType) {
        public TokenRefreshResponse(String accessToken, String refreshToken) {
            this(accessToken, refreshToken, "Bearer");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestParam(name = "refreshToken", required = true) String refreshToken, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            refreshTokenService.logoutDevice(refreshToken);
        }
        return ResponseEntity.ok("Logged out successfully.");
    }
}