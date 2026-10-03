package com.ktk.dukappservice.security;

import java.util.List;

public record JwtResponse(
    String token,
    String refreshToken,
    String type, // Usually "Bearer"
    String username,
    List<String> roles,
    List<String> permissions
) {
    public JwtResponse(String token, String refreshToken, String username, List<String> roles, List<String> permissions) {
        this(token, refreshToken, "Bearer", username, roles, permissions);
    }
}