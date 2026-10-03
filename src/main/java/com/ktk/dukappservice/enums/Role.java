package com.ktk.dukappservice.enums;

/**
 * Legacy single role per user (column USERS.ROLE). Kept in sync with the user's {@code AppRole}s for older clients,
 * the booking system and system-user lookups. Authorization uses {@link Permission}s instead.
 */
public enum Role {
    ADMIN, USER, TEAM_LEADER, HELPER
}
