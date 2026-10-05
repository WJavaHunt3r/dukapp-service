package com.ktk.dukappservice.data.jobchat;

import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.JobStatus;
import com.ktk.dukappservice.enums.Permission;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * The chat room of a job. Taking part: the registered users (not the waitlist), the responsible user and the creator.
 * People with {@link Permission#JOB_MANAGE_ALL} can read and write too. The chat is archived (read only) as soon as the
 * job is completed (hours submitted) or cancelled.
 */
@Service
public class JobChatService {
    public static final int MAX_LENGTH = 2000;

    private final JobChatMessageRepository messages;
    private final JobChatMuteRepository mutes;
    private final JobService jobService;

    public JobChatService(JobChatMessageRepository messages, JobChatMuteRepository mutes, JobService jobService) {
        this.messages = messages;
        this.mutes = mutes;
        this.jobService = jobService;
    }

    public static boolean isArchived(Job job) {
        return job.getStatus() != JobStatus.OPEN;
    }

    /** Ids of everyone who takes part in the chat of the job. */
    public Set<Long> participantIds(Job job) {
        Set<Long> ids = new LinkedHashSet<>();
        for (JobRegistration registration : jobService.findActiveRegistrations(job.getId())) {
            if (registration.getStatus() == JobRegistrationStatus.REGISTERED) {
                ids.add(registration.getUser().getId());
            }
        }
        ids.add(job.getResponsible().getId());
        ids.add(job.getCreateUser().getId());
        return ids;
    }

    public boolean canAccess(User user, Job job) {
        return user.hasPermission(Permission.JOB_MANAGE_ALL) || participantIds(job).contains(user.getId());
    }

    /** Newest messages in date order; {@code after} = only newer than that id, {@code before} = the page of older ones. */
    public List<JobChatMessage> getMessages(Job job, Long after, Long before) {
        List<JobChatMessage> result;
        if (after != null) {
            return messages.findTop200ByJobIdAndIdGreaterThanOrderByIdAsc(job.getId(), after);
        }
        result = before != null
                ? new ArrayList<>(messages.findTop50ByJobIdAndIdLessThanOrderByIdDesc(job.getId(), before))
                : new ArrayList<>(messages.findTop50ByJobIdOrderByIdDesc(job.getId()));
        Collections.reverse(result);
        return result;
    }

    @Transactional
    public JobChatMessage post(Job job, User sender, String text) {
        if (!canAccess(sender, job)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only people taking part in the job can use its chat.");
        }
        if (isArchived(job)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The chat is archived, the job is closed.");
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A message needs 1 to " + MAX_LENGTH + " characters.");
        }
        JobChatMessage message = new JobChatMessage();
        message.setJob(job);
        message.setSender(sender);
        message.setText(trimmed);
        message.setCreateDateTime(LocalDateTime.now());
        return messages.save(message);
    }

    public boolean isMuted(Long jobId, Long userId) {
        return mutes.findByJobIdAndUserId(jobId, userId).isPresent();
    }

    @Transactional
    public void setMuted(Job job, User user, boolean muted) {
        Optional<JobChatMute> existing = mutes.findByJobIdAndUserId(job.getId(), user.getId());
        if (muted && existing.isEmpty()) {
            JobChatMute mute = new JobChatMute();
            mute.setJob(job);
            mute.setUser(user);
            mutes.save(mute);
        } else if (!muted) {
            existing.ifPresent(mutes::delete);
        }
    }

    /** The users who should get a push for a new message: everyone taking part except the sender and who muted the chat. */
    public Set<Long> recipientIds(Job job, Long senderId) {
        Set<Long> ids = participantIds(job);
        ids.remove(senderId);
        if (ids.isEmpty()) {
            return ids;
        }
        mutes.findByJobIdAndUserIdIn(job.getId(), ids).forEach(m -> ids.remove(m.getUser().getId()));
        return ids;
    }
}
