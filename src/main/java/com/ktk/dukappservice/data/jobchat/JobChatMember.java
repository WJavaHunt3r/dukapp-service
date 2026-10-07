package com.ktk.dukappservice.data.jobchat;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Someone added to the chat of a job by its organisers although they are not registered for the job. They can read
 * and write and get the chat's notifications, but hold no place, so registrations and hours are not affected.
 */
@Getter
@Setter
@Entity
@Table(name = "JOB_CHAT_MEMBERS", uniqueConstraints = {@UniqueConstraint(name = "UniqueChatMember", columnNames = {"JOB", "USERS"})})
public class JobChatMember extends BaseEntity<JobChatMember, Long> {

    @NotNull
    @JoinColumn(name = "JOB")
    @ManyToOne
    private Job job;

    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    @NotNull
    @JoinColumn(name = "ADDED_BY")
    @ManyToOne
    private User addedBy;

    @Column(name = "ADDED_DATE_TIME")
    private LocalDateTime addedDateTime;
}
