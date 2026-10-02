package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.JobRegistrationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
public class JobRegistrationDto {
    private Long id;
    private Long jobId;
    private Long userId;
    private String userName;
    private Long registeredById;
    private String registeredByName;
    /** Null unless the requester is the registrant, their parent, the registrar or runs the job. */
    private String comment;
    private JobRegistrationStatus status;
    /** 1 = next to be promoted. Only set for WAITLISTED. */
    private Integer waitlistPosition;
    private LocalDateTime registeredDateTime;
    private LocalDateTime cancelledDateTime;
    private double hours;
}
