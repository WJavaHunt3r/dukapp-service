package com.ktk.dukappservice.dto;

import java.time.LocalDateTime;

public record JobChatMessageDto(Long id, Long userId, String userName, String text, LocalDateTime createDateTime) {
}
