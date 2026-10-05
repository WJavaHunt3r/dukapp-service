package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobCounts;
import com.ktk.dukappservice.dto.JobDto;
import com.ktk.dukappservice.dto.JobRegistrationDto;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.JobStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Hand-written on purpose (no ModelMapper): the entity has server-managed fields (status, activity, creator, ...)
 * that must never be set from a request body.
 */
@Service
public class JobMapper implements BaseMapperInterface<Job, JobDto> {

    /** Without registration info: counts stay null, {@code registrationOpen} ignores capacity. */
    @Override
    public JobDto entityToDto(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setJobDateTime(job.getJobDateTime());
        dto.setJobEndDateTime(job.getJobEndDateTime());
        dto.setDescription(job.getDescription());
        dto.setComment(job.getComment());
        dto.setEmployerId(job.getEmployer().getId());
        dto.setEmployerName(job.getEmployer().getFullName());
        dto.setResponsibleId(job.getResponsible().getId());
        dto.setResponsibleName(job.getResponsible().getFullName());
        dto.setAccount(job.getAccount());
        dto.setTransactionType(job.getTransactionType());
        dto.setRegistrationOpensAt(job.getRegistrationOpensAt());
        dto.setSendNotification(job.isSendNotification());
        dto.setRegistrationDeadline(job.getRegistrationDeadline());
        dto.setCancellationDeadline(job.getCancellationDeadline());
        dto.setCancellationAllowed(job.isCancellationAllowed());
        dto.setRegistrationClosed(job.isRegistrationClosed());
        dto.setMaxParticipants(job.getMaxParticipants());
        dto.setWaitlistEnabled(job.isWaitlistEnabled());
        dto.setMinAge(job.getMinAge());
        dto.setMaxAge(job.getMaxAge());
        dto.setGenderRestriction(job.getGenderRestriction());
        dto.setCreateDateTime(job.getCreateDateTime());
        dto.setCreateUserId(job.getCreateUser().getId());
        dto.setCreateUserName(job.getCreateUser().getFullName());
        dto.setSeriesId(job.getSeriesId());
        dto.setStatus(job.getStatus());
        dto.setActivityId(job.getActivity() == null ? null : job.getActivity().getId());
        dto.setCompletedDateTime(job.getCompletedDateTime());

        LocalDateTime now = LocalDateTime.now();
        boolean open = job.getStatus() == JobStatus.OPEN;
        dto.setRegistrationOpen(open && !job.isRegistrationClosed()
                && (job.getRegistrationDeadline() == null || !now.isAfter(job.getRegistrationDeadline()))
                && (job.getRegistrationOpensAt() == null || !now.isBefore(job.getRegistrationOpensAt())));
        dto.setCancellationOpen(open && job.isCancellationAllowed()
                && (job.getCancellationDeadline() == null || !now.isAfter(job.getCancellationDeadline())));
        return dto;
    }

    public JobDto toDto(Job job, JobCounts counts, JobRegistrationStatus myStatus) {
        JobDto dto = entityToDto(job);
        boolean full = job.getMaxParticipants() != null && counts.registered() >= job.getMaxParticipants();
        dto.setRegisteredCount(counts.registered());
        dto.setWaitlistCount(counts.waitlisted());
        dto.setFull(full);
        dto.setRegistrationOpen(dto.isRegistrationOpen() && (!full || job.isWaitlistEnabled()));
        dto.setMyRegistrationStatus(myStatus);
        return dto;
    }

    /** Copies the editable fields only. Employer and responsible are resolved by the caller, like in ActivityMapper. */
    @Override
    public Job dtoToEntity(JobDto dto, Job job) {
        job.setJobDateTime(dto.getJobDateTime());
        job.setJobEndDateTime(dto.getJobEndDateTime());
        job.setDescription(dto.getDescription().trim());
        job.setComment(dto.getComment() == null || dto.getComment().isBlank() ? null : dto.getComment().trim());
        job.setAccount(dto.getAccount());
        job.setTransactionType(dto.getTransactionType());
        job.setRegistrationOpensAt(dto.getRegistrationOpensAt());
        job.setSendNotification(dto.isSendNotification());
        job.setRegistrationDeadline(dto.getRegistrationDeadline());
        job.setCancellationDeadline(dto.getCancellationDeadline());
        job.setCancellationAllowed(dto.isCancellationAllowed());
        job.setRegistrationClosed(dto.isRegistrationClosed());
        job.setMaxParticipants(dto.getMaxParticipants());
        job.setWaitlistEnabled(dto.isWaitlistEnabled());
        job.setMinAge(dto.getMinAge());
        job.setMaxAge(dto.getMaxAge());
        job.setGenderRestriction(dto.getGenderRestriction());
        return job;
    }

    public JobRegistrationDto registrationToDto(JobRegistration registration, boolean includeComment, Integer waitlistPosition) {
        JobRegistrationDto dto = new JobRegistrationDto();
        dto.setId(registration.getId());
        dto.setJobId(registration.getJob().getId());
        dto.setUserId(registration.getUser().getId());
        dto.setUserName(registration.getUser().getFullName());
        dto.setRegisteredById(registration.getRegisteredBy().getId());
        dto.setRegisteredByName(registration.getRegisteredBy().getFullName());
        dto.setComment(includeComment ? registration.getComment() : null);
        dto.setStatus(registration.getStatus());
        dto.setWaitlistPosition(waitlistPosition);
        dto.setRegisteredDateTime(registration.getRegisteredDateTime());
        dto.setCancelledDateTime(registration.getCancelledDateTime());
        dto.setHours(registration.getHours());
        return dto;
    }
}
