package com.ktk.dukappservice.enums;

/**
 * Fine-grained permissions checked by the backend. Roles (see {@code data/roles/AppRole}) bundle these,
 * and a user's effective permissions are the union over all of their roles.
 * Each permission is exposed to Spring Security as an authority with the same name,
 * so endpoints can use {@code @PreAuthorize("hasAuthority('CAMP_MANAGE')")}.
 */
public enum Permission {
    /** Edit any user's profile, not only your own. */
    USER_MANAGE,
    /** Create/edit/delete roles and assign roles to users. */
    ROLE_MANAGE,
    /** Reset another user's password. */
    PASSWORD_RESET,
    /** Create and delete transactions and transaction items. */
    TRANSACTION_MANAGE,
    /** Register activities in the app / in Teams (SharePoint). */
    ACTIVITY_REGISTER,
    /** Delete activities and activity items created by other users. */
    ACTIVITY_MANAGE_ALL,
    /** Create jobs, and edit or cancel the jobs you created. */
    JOB_CREATE,
    /** Edit, cancel and complete any job, and register/unregister any user on a job (also after the cancellation deadline). */
    JOB_MANAGE_ALL,
    /** Create, edit and delete goals. */
    GOAL_MANAGE,
    /** Create and delete mentor/mentee pairs. */
    MENTOR_MANAGE,
    /** Create, edit and delete camps, and register other users to camps. */
    CAMP_MANAGE,
    /** Create, edit and delete donations. */
    DONATION_MANAGE,
    /** Delete FraKare week entries and set "listened" on other users' entries. */
    FRAKARE_MANAGE,
    /** Trigger creation of pace team rounds. */
    PACE_TEAM_MANAGE,
    /** Create, edit and delete challenges. */
    CHALLENGE_MANAGE,
    /** Create/edit rounds, create FraKare weeks, and trigger point/status recalculations. */
    SEASON_MANAGE,
    /** Delete payments (reading, creating and updating stay public for the payment callbacks). */
    PAYMENT_MANAGE,
    /** Gets the ADMIN role in the external booking system. */
    BOOKING_ADMIN,
    /** Read the audit log (GET /api/auditLog). */
    AUDIT_LOG_VIEW
}
