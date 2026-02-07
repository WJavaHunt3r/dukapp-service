package com.ktk.dukappservice.security;

import java.util.List;

public record JwtResponse(
    String token,
    String type, // Usually "Bearer"
    String username,
    List<String> roles
) {
    public JwtResponse(String token, String username, List<String> roles) {
        this(token, "Bearer", username, roles);
    }
}