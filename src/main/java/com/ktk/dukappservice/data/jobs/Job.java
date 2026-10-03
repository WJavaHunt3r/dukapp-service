package com.ktk.dukappservice.data.jobs;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.Gender;
import com.ktk.dukappservice.enums.JobStatus;
import com.ktk.dukappservice.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * A job people can register for before it happens. Once it is over the responsible user submits the hours of each
 * registered user ({@code JobService#complete}), which creates a regular {@link Activity} with its items.
 */
@Getter
@Setter
@Entity
@Table(name = "JOBS")
@FieldNameConstants
public class Job extends BaseEntity<Job, Long> {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "CREATE_DATE_TIME")
    private LocalDateTime createDateTime;

    @JoinColumn(name = "CREATE_USER")
    @ManyToOne
    @NotNull
    private User createUser;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "JOB_DATE_TIME")
    @NotNull
    private LocalDateTime jobDateTime;

    /** Optional; jobs created before this field existed have none. Must be after {@link #jobDateTime}. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "JOB_END_DATE_TIME")
    private LocalDateTime jobEndDateTime;

    @Size(max = 200)
    @Column(name = "DESCRIPTION", length = 200)
    @NotNull
    @NotEmpty
    private String description;

    @JoinColumn(name = "EMPLOYER")
    @ManyToOne
    @NotNull
    private User employer;

    @JoinColumn(name = "RESPONSIBLE")
    @ManyToOne
    @NotNull
    private User responsible;

    @Column(name = "ACCOUNT")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Account account;

    @Column(name = "TRANSACTION_TYPE")
    @Enumerated(EnumType.STRING)
    @NotNull
    private TransactionType transactionType;

    /** Null = no deadline. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "REGISTRATION_DEADLINE")
    private LocalDateTime registrationDeadline;

    /** Until then registered users (or their parents) can cancel on their own. Null = no deadline. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "CANCELLATION_DEADLINE")
    private LocalDateTime cancellationDeadline;

    /** False: registered users can't cancel on their own at all (only {@code JOB_MANAGE_ALL}). */
    @Column(name = "CANCELLATION_ALLOWED", columnDefinition = "boolean default true")
    private boolean cancellationAllowed = true;

    /** True: nobody can register until the job is opened again, whatever the opening time and deadline say. */
    @Column(name = "REGISTRATION_CLOSED", columnDefinition = "boolean default false")
    private boolean registrationClosed;

    /** Registration opens at this time; null means it is open from the moment the job is created. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "REGISTRATION_OPENS_AT")
    private LocalDateTime registrationOpensAt;

    /**
     * Whether users are told about the job with a push notification, sent when registration opens (right away
     * without {@link #registrationOpensAt}). Rows from before this existed are false, so they are never announced.
     */
    @Column(name = "SEND_NOTIFICATION", columnDefinition = "boolean default false")
    private boolean sendNotification;

    /** When the "new job" notification went out; null while it is still due. */
    @Column(name = "ANNOUNCED_DATE_TIME")
    private LocalDateTime announcedDateTime;

    /** Number of places; null means unlimited. */
    @Column(name = "MAX_PARTICIPANTS")
    private Integer maxParticipants;

    /** When the job is full, further registrations are waitlisted instead of rejected. */
    @Column(name = "WAITLIST_ENABLED", columnDefinition = "boolean default false")
    private boolean waitlistEnabled;

    /** Inclusive, measured on the day of the job. Null means no limit. */
    @Column(name = "MIN_AGE")
    private Integer minAge;

    /** Inclusive, measured on the day of the job. Null means no limit. */
    @Column(name = "MAX_AGE")
    private Integer maxAge;

    /** Null means open to everyone. */
    @Column(name = "GENDER_RESTRICTION", length = 10)
    @Enumerated(EnumType.STRING)
    private Gender genderRestriction;

    /** Shared by the occurrences of a repeating job; null for a single job. */
    @Column(name = "SERIES_ID", length = 36)
    private String seriesId;

    @Column(name = "STATUS", length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private JobStatus status = JobStatus.OPEN;

    /** The activity created when the job was completed. */
    @JoinColumn(name = "ACTIVITY")
    @ManyToOne
    private Activity activity;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "COMPLETED_DATE_TIME")
    private LocalDateTime completedDateTime;
}
