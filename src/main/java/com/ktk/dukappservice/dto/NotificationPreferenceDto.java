package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.NotificationChannel;
import com.ktk.dukappservice.enums.NotificationType;

public record NotificationPreferenceDto(NotificationType type, NotificationChannel channel, boolean enabled) {
}
