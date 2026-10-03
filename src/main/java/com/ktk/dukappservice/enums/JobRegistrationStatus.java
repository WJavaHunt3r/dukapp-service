package com.ktk.dukappservice.enums;

public enum JobRegistrationStatus {
    /** Holds one of the job's places. */
    REGISTERED,
    /** Job was full; promoted to REGISTERED (oldest first) when a place frees up. */
    WAITLISTED,
    /** Withdrawn. The row is kept for history and reused if the user registers again. */
    CANCELLED
}
