package com.ktk.dukappservice.dto;

import java.util.List;

/** The chat of a job: messages (oldest first), whether the current user muted it, and whether it is read only. */
public record JobChatDto(List<JobChatMessageDto> messages, boolean muted, boolean archived, boolean canPost) {
}
