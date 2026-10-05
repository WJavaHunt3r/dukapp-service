package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.jobchat.JobChatMessage;
import com.ktk.dukappservice.data.jobchat.JobChatService;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.JobChatDto;
import com.ktk.dukappservice.dto.JobChatMessageDto;
import com.ktk.dukappservice.dto.JobChatMuteDto;
import com.ktk.dukappservice.dto.JobChatSendDto;
import com.ktk.dukappservice.service.notifications.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** The chat room of a job; see {@link JobChatService} for who takes part. Clients poll with {@code after}. */
@RestController
@RequestMapping("/api/job/{jobId}/chat")
public class JobChatController {

    private final JobChatService chatService;
    private final JobService jobService;
    private final UserService userService;
    private final PushNotificationService pushNotificationService;

    public JobChatController(JobChatService chatService, JobService jobService, UserService userService,
                             PushNotificationService pushNotificationService) {
        this.chatService = chatService;
        this.jobService = jobService;
        this.userService = userService;
        this.pushNotificationService = pushNotificationService;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    /** No parameters: the newest 50 messages. {@code after}: only newer ones. {@code before}: the page of older ones. */
    @GetMapping
    public ResponseEntity<?> getChat(@PathVariable Long jobId,
                                     @RequestParam(value = "after", required = false) Long after,
                                     @RequestParam(value = "before", required = false) Long before,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        Job job = findJob(jobId);
        requireAccess(user, job);
        List<JobChatMessageDto> messages = chatService.getMessages(job, after, before).stream().map(this::toDto).toList();
        boolean archived = JobChatService.isArchived(job);
        return ResponseEntity.ok(new JobChatDto(messages, chatService.isMuted(jobId, user.getId()), archived, !archived));
    }

    @PostMapping
    public ResponseEntity<?> send(@PathVariable Long jobId, @Valid @RequestBody JobChatSendDto body,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        Job job = findJob(jobId);
        JobChatMessage message = chatService.post(job, user, body.getText());
        pushNotificationService.chatMessage(jobId, user, message.getText());
        return ResponseEntity.ok(toDto(message));
    }

    @PutMapping("/mute")
    public ResponseEntity<?> mute(@PathVariable Long jobId, @RequestBody JobChatMuteDto body,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails);
        Job job = findJob(jobId);
        requireAccess(user, job);
        chatService.setMuted(job, user, body.isMuted());
        return ResponseEntity.ok(body.isMuted());
    }

    private void requireAccess(User user, Job job) {
        if (!chatService.canAccess(user, job)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only people taking part in the job can use its chat.");
        }
    }

    private Job findJob(Long id) {
        return jobService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id: " + id));
    }

    private JobChatMessageDto toDto(JobChatMessage m) {
        return new JobChatMessageDto(m.getId(), m.getSender().getId(), m.getSender().getFullName(), m.getText(), m.getCreateDateTime());
    }
}
