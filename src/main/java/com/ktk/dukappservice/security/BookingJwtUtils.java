package com.ktk.dukappservice.security;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class BookingJwtUtils {
    @Value("${app.booking.jwt.secret}")
    private String jwtSecret;

    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiresAt = now.plus(5, ChronoUnit.MINUTES);

        // JWT felépítése JJWT builder-rel
        return Jwts.builder()
                .claim("userId", user.getId())
                .claim("name", user.getFullName())
                .claim("email", user.getEmail())
                .claim("phone", user.getPhoneNumber())
                .claim("role", user.getRole() == Role.USER ? Role.USER.name() : Role.ADMIN.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key) // Digitális aláírás a megosztott kulccsal
                .compact();
    }

}