package com.ktk.dukappservice.data.jobregistrations;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "JOB_REGISTRATIONS", uniqueConstraints =
        {@UniqueConstraint(name = "UniqueJobAndUser", columnNames = {"JOB", "USERS"})})
@FieldNameConstants
public class JobRegistration extends BaseEntity<JobRegistration, Long> {

    @NotNull
    @JoinColumn(name = "JOB")
    @ManyToOne
    private Job job;

    /** The person who will do the job. */
    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    /** Who made the registration: the user themselves, a parent, or an admin. */
    @NotNull
    @JoinColumn(name = "REGISTERED_BY")
    @ManyToOne
    private User registeredBy;

    @Size(max = 500)
    @Column(name = "REGISTRATION_COMMENT", length = 500)
    private String comment;

    @Column(name = "STATUS", length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private JobRegistrationStatus status;

    /** Waitlist order is by this timestamp (then id), so re-registering after a cancel goes to the back. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "REGISTERED_DATE_TIME")
    private LocalDateTime registeredDateTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "CANCELLED_DATE_TIME")
    private LocalDateTime cancelledDateTime;

    /** Hours submitted when the job was completed. */
    @Column(name = "HOURS", columnDefinition = "float8 default 0")
    private double hours;
}
