package com.ktk.dukappservice.enums;

public enum AuditAction {
    /** Entity inserted (any JPA entity that isn't excluded in {@code AuditEntityListener}). */
    CREATE,
    /** Entity updated; details hold the changed fields with old and new values. */
    UPDATE,
    /** Entity deleted; details hold the deleted state. */
    DELETE,
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    REGISTER,
    PASSWORD_CHANGE,
    PASSWORD_RESET,
    /** A new random password was e-mailed to the user. */
    PASSWORD_SEND,
    /** The roles assigned to a user changed. */
    ROLES_CHANGE,
    /** The permissions of a role changed. */
    ROLE_PERMISSIONS_CHANGE,
    /** An authenticated user was refused by a permission check. */
    ACCESS_DENIED
}
