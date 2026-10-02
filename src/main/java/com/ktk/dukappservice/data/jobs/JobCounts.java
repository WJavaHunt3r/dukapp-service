package com.ktk.dukappservice.data.jobs;

/** Number of users holding a place / waiting for one. Cancelled registrations are not counted. */
public record JobCounts(long registered, long waitlisted) {
    public static final JobCounts NONE = new JobCounts(0, 0);
}
