package com.ktk.dukappservice.enums;

public enum JobStatus {
    /** Accepting registrations (until the registration deadline) and cancellations (until the cancellation deadline). */
    OPEN,
    /** Hours were submitted and an Activity was created from the job. */
    COMPLETED,
    /** Called off before it took place. */
    CANCELLED
}
