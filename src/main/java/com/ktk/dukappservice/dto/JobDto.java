package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request and response body for jobs. On create/update only the "input" fields are read; id, status, activity and
 * everything under "read-only" is ignored.
 */
@NoArgsConstructor
@Getter
@Setter
public class JobDto {
    private Long id;

    // ---- input
    @NotNull
    private LocalDateTime jobDateTime;

    /** When the job ends; optional, must be after the start. */
    private LocalDateTime jobEndDateTime;

    @NotBlank
    @Size(max = 200)
    private String description;

    @NotNull
    private Long employerId;

    @NotNull
    private Long responsibleId;

    @NotNull
    private Account account;

    @NotNull
    private TransactionType transactionType;

    @NotNull
    private LocalDateTime registrationDeadline;

    @NotNull
    private LocalDateTime cancellationDeadline;

    /** Number of places, null for unlimited. */
    @Min(1)
    private Integer maxParticipants;

    private boolean waitlistEnabled;

    @Min(0)
    private Integer minAge;

    @Min(0)
    private Integer maxAge;

    /** Null means open to everyone. */
    private Gender genderRestriction;

    // ---- read-only
    private LocalDateTime createDateTime;
    private Long createUserId;
    private String createUserName;
    private String employerName;
    private String responsibleName;
    private JobStatus status;
    private Long activityId;
    private LocalDateTime completedDateTime;

    /** Null in responses that don't carry registration info. */
    private Long registeredCount;
    private Long waitlistCount;
    private Boolean full;

    /** Open for new registrations (or the waitlist) right now. */
    private boolean registrationOpen;
    /** Registered users can still cancel on their own. */
    private boolean cancellationOpen;
    /** The requesting user's own registration, null if none. Children are listed via the registrations endpoint. */
    private JobRegistrationStatus myRegistrationStatus;
}
