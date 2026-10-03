package com.ktk.dukappservice.enums;

/**
 * Everything a user can be notified about. Every type can be switched off per user in their notification
 * preferences (enabled by default).
 */
public enum NotificationType {
    /** A new job was published that the user is eligible for. */
    JOB_NEW(NotificationChannel.PUSH),
    /** A job the user is registered or waitlisted for was cancelled. */
    JOB_CANCELLED(NotificationChannel.PUSH),
    /** Someone else (a parent, an organizer) registered the user for a job. */
    JOB_REGISTERED_BY_OTHER(NotificationChannel.PUSH),
    /** A transaction was manually created for the user. */
    TRANSACTION_CREATED(NotificationChannel.PUSH),
    /** Recurring weekly notification defined by an admin. */
    WEEKLY(NotificationChannel.PUSH),
    /** One-off notification sent by an admin. */
    GENERAL(NotificationChannel.PUSH),
    /** The weekly "on track" status e-mail. */
    ON_TRACK_EMAIL(NotificationChannel.EMAIL);

    private final NotificationChannel channel;

    NotificationType(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationChannel getChannel() {
        return channel;
    }
}
