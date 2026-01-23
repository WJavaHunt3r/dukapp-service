package com.ktk.dukappservice.enums;

public enum Role {
    ADMIN, USER, TEAM_LEADER, HELPER;

    public static boolean hasRestrictions(Role role) {
        return USER.equals(role) || TEAM_LEADER.equals(role);
    }
}