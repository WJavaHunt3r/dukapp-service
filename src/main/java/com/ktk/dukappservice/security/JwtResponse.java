package com.ktk.dukappservice.security;

import java.util.List;

public record JwtResponse(
    String token,
    String refreshToken,
    String type, // Usually "Bearer"
    String username,
    List<String> roles
) {
    public JwtResponse(String token,String refreshToken, String username, List<String> roles) {
        this(token, refreshToken, "Bearer", username, roles);
    }
}