package com.ktk.dukappservice.data.jobchat;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** A user who muted the chat of a job: they can still read it, but get no push notifications from it. */
@Getter
@Setter
@Entity
@Table(name = "JOB_CHAT_MUTES", uniqueConstraints = {@UniqueConstraint(name = "UniqueChatMute", columnNames = {"JOB", "USERS"})})
public class JobChatMute extends BaseEntity<JobChatMute, Long> {

    @NotNull
    @JoinColumn(name = "JOB")
    @ManyToOne
    private Job job;

    @NotNull
    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;
}
