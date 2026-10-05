package com.ktk.dukappservice.data.jobchat;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/** One message in the chat of a job. */
@Getter
@Setter
@Entity
@Table(name = "JOB_CHAT_MESSAGES", indexes = {@Index(name = "idx_job_chat_job", columnList = "JOB")})
@FieldNameConstants
public class JobChatMessage extends BaseEntity<JobChatMessage, Long> {

    @NotNull
    @JoinColumn(name = "JOB")
    @ManyToOne
    private Job job;

    @NotNull
    @JoinColumn(name = "SENDER")
    @ManyToOne
    private User sender;

    @NotNull
    @Size(max = 2000)
    @Column(name = "MESSAGE_TEXT", length = 2000)
    private String text;

    @Column(name = "CREATE_DATE_TIME")
    private LocalDateTime createDateTime;
}
