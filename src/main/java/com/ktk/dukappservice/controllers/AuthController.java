package com.ktk.dukappservice.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ktk.dukappservice.data.auditlog.AuditLogService;
import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.roles.AppRoleService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.*;
import com.ktk.dukappservice.enums.AuditAction;
import com.ktk.dukappservice.security.*;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import com.microsoft.graph.models.odataerrors.ODataError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final AppRoleService appRoleService;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          MicrosoftService microsoftService,
                          JwtUtils jwtUtils,
                          BookingJwtUtils bookingJwtUtils, PasswordEncoder passwordEncoder, DukAppDetailsManager detailsManager, RefreshTokenService refreshTokenService,
                          @Value("${google.app.clientIds}") List<String> googleClientIds, AppRoleService appRoleService, AuditLogService auditLogService) {
        this.appRoleService = appRoleService;
        this.auditLogService = auditLogService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.microsoftService = microsoftService;
        this.jwtUtils = jwtUtils;
        this.bookingJwtUtils = bookingJwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.detailsManager = detailsManager;
        this.refreshTokenService = refreshTokenService;
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(googleClientIds)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
            );
        } catch (AuthenticationException e) {
            auditLogService.recordAs(loginDto.getUsername(), AuditAction.LOGIN_FAILED, null, null,
                    Map.of("method", "password", "reason", e.getClass().getSimpleName()));
            throw e;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Fetch the actual username from the principal (in case they logged in with email)
        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        String jwt = jwtUtils.generateToken(userDetails.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        List<String> roles = authorities.stream().filter(a -> a.startsWith("ROLE_")).toList();
        List<String> permissions = authorities.stream().filter(a -> !a.startsWith("ROLE_")).toList();

        auditLogService.recordAs(userDetails.getUsername(), AuditAction.LOGIN, null, null, Map.of("method", "password"));
        return ResponseEntity.ok(new JwtResponse(jwt, refreshToken.getToken(), userDetails.getUsername(), roles, permissions));
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
        user.setRoles(new HashSet<>(Set.of(appRoleService.getDefaultRole())));
        user.setChangedPassword(true);
        user.setFirstname(registerDto.getFirstname());
        user.setLastname(registerDto.getLastname());

        userService.save(user);
        auditLogService.recordAs(user.getUsername(), AuditAction.REGISTER, "User", user.getId(), Map.of("method", "password"));

        // 3. Optional: Return a JWT immediately so they don't have to log in right after registering
        String jwt = jwtUtils.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
        return ResponseEntity.ok(jwtResponse(jwt, refreshToken, user));
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
        auditLogService.record(AuditAction.PASSWORD_CHANGE, "User", user.getId(), null);

        return ResponseEntity.ok("Password updated successfully");
    }

    @PostMapping("/resetPassword")
    @PreAuthorize("hasAuthority('PASSWORD_RESET')")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto) {
        User targetUser = userService.findById(dto.getUserId()).orElseThrow();
        String newHash = passwordEncoder.encode(targetUser.getUsername());
        UserDetails userDetails = detailsManager.loadUserByUsername(targetUser.getUsername());
        detailsManager.updatePassword(userDetails, newHash);
        auditLogService.record(AuditAction.PASSWORD_RESET, "User", targetUser.getId(), Map.of("user", targetUser.getUsername()));
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
        auditLogService.record(AuditAction.PASSWORD_SEND, "User", user.get().getId(), Map.of("user", user.get().getUsername()));

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

        try {
            // 1. Verify the token
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();

                // Get user info from Google payload
                String email = googlePayload.getEmail();
                ResolvedName name = resolveGoogleName((String) googlePayload.get("given_name"),
                        (String) googlePayload.get("family_name"), (String) googlePayload.get("name"));

                // 2. Find or Create the user in your database
                User user = userService.findByEmail(email).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstname(name.firstname());
                    newUser.setLastname(name.lastname());
                    newUser.setMyShareID(null);
                    // Generate username using your normalization logic; never from a placeholder name
                    String baseUsername = name.complete()
                            ? normalizeUsername(name.lastname(), name.firstname())
                            : normalizeUsername(email.substring(0, Math.max(email.indexOf('@'), 0)), "");
                    if (baseUsername.isEmpty()) {
                        baseUsername = "user";
                    }
                    baseUsername = baseUsername.substring(0, Math.min(baseUsername.length(), 25)); // USERNAME is max 30
                    String finalUsername = baseUsername;
                    int counter = 1;
                    while (userService.findByUsername(finalUsername).isPresent()) {
                        finalUsername = baseUsername + counter;
                        counter++;
                    }
                    newUser.setUsername(finalUsername);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Random pass
                    newUser.setRoles(new HashSet<>(Set.of(appRoleService.getDefaultRole())));
                    User saved = userService.save(newUser);
                    auditLogService.recordAs(saved.getUsername(), AuditAction.REGISTER, "User", saved.getId(), Map.of("method", "google"));
                    return saved;
                });
                auditLogService.recordAs(user.getUsername(), AuditAction.LOGIN, null, null, Map.of("method", "google"));

                // 3. Generate YOUR app's JWT
                String jwt = jwtUtils.generateToken(user.getUsername());
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
                return ResponseEntity.ok(jwtResponse(jwt, refreshToken, user));
            }
        } catch (Exception e) {
            auditLogService.recordAs(null, AuditAction.LOGIN_FAILED, null, null,
                    Map.of("method", "google", "reason", e.getClass().getSimpleName()));
            return ResponseEntity.status(401).body("Invalid Google Token");
        }
        auditLogService.recordAs(null, AuditAction.LOGIN_FAILED, null, null, Map.of("method", "google", "reason", "token rejected"));
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

    private JwtResponse jwtResponse(String jwt, RefreshToken refreshToken, User user) {
        return new JwtResponse(jwt, refreshToken.getToken(), user.getUsername(),
                user.getRoles().stream().map(AppRole::getName).toList(),
                user.getPermissions().stream().map(Enum::name).toList());
    }

    @GetMapping("/isAlive")
    public ResponseEntity<?> isAlive() {
        return ResponseEntity.ok("OK");
    }

    record ResolvedName(String firstname, String lastname) {
        boolean complete() {
            return !User.NAME_PLACEHOLDER.equals(firstname) && !User.NAME_PLACEHOLDER.equals(lastname);
        }
    }

    /**
     * Google doesn't always send given_name / family_name. Falls back to the full display name ("name"): the part
     * that isn't the given name is the family name, which works for both "Anna Kis" and Hungarian "Kis Anna".
     * Whatever is still missing becomes {@link User#NAME_PLACEHOLDER}, which marks the profile as incomplete so the
     * frontend asks the user to fill it in.
     */
    static ResolvedName resolveGoogleName(String givenName, String familyName, String fullName) {
        String given = blankToNull(givenName);
        String family = blankToNull(familyName);
        String full = blankToNull(fullName);

        if (family == null && full != null) {
            if (given == null) {
                int split = full.lastIndexOf(' ');
                given = split > 0 ? full.substring(0, split).trim() : full;
                family = split > 0 ? full.substring(split + 1).trim() : null;
            } else if (full.contains(given)) {
                family = blankToNull(full.replaceFirst(Pattern.quote(given), "").replaceAll("\\s+", " "));
            }
        }
        return new ResolvedName(nameOrPlaceholder(given), nameOrPlaceholder(family));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nameOrPlaceholder(String name) {
        return name == null ? User.NAME_PLACEHOLDER : name.substring(0, Math.min(name.length(), 50));
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
            auditLogService.recordAs(userDetails.getUsername(), AuditAction.LOGOUT, null, null, null);
        }
        return ResponseEntity.ok("Logged out successfully.");
    }
}