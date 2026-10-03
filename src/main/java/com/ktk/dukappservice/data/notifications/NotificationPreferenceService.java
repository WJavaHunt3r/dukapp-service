package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    /** Every notification type with the user's setting (enabled unless switched off). */
    public Map<NotificationType, Boolean> getPreferences(User user) {
        Map<NotificationType, NotificationPreference> stored = repository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(NotificationPreference::getType, Function.identity()));
        Map<NotificationType, Boolean> result = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            NotificationPreference preference = stored.get(type);
            result.put(type, preference == null || preference.isEnabled());
        }
        return result;
    }

    /** Updates the given types only; types missing from {@code changes} keep their setting. */
    @Transactional
    public Map<NotificationType, Boolean> updatePreferences(User user, Map<NotificationType, Boolean> changes) {
        Map<NotificationType, NotificationPreference> stored = repository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(NotificationPreference::getType, Function.identity()));
        changes.forEach((type, enabled) -> {
            if (type == null || enabled == null) {
                return;
            }
            NotificationPreference preference = stored.get(type);
            if (preference == null) {
                preference = new NotificationPreference();
                preference.setUser(user);
                preference.setType(type);
            } else if (preference.isEnabled() == enabled) {
                return;
            }
            preference.setEnabled(enabled);
            repository.save(preference);
        });
        return getPreferences(user);
    }

    public Set<Long> findDisabledUserIds(NotificationType type) {
        return repository.findDisabledUserIds(type);
    }
}
