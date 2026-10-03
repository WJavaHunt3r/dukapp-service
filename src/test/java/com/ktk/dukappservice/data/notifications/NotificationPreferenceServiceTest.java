package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationPreferenceServiceTest {

    private final NotificationPreferenceRepository repository = mock(NotificationPreferenceRepository.class);
    private final NotificationPreferenceService service = new NotificationPreferenceService(repository);
    private final List<NotificationPreference> store = new ArrayList<>();
    private final User user = new User();

    NotificationPreferenceServiceTest() {
        user.setId(1L);
        when(repository.findByUserId(1L)).thenAnswer(i -> List.copyOf(store));
        when(repository.save(any())).thenAnswer(i -> {
            NotificationPreference p = i.getArgument(0);
            if (!store.contains(p)) {
                store.add(p);
            }
            return p;
        });
    }

    @Test
    void everythingIsEnabledByDefault() {
        assertThat(service.getPreferences(user)).hasSize(NotificationType.values().length).doesNotContainValue(false);
    }

    @Test
    void updatesOnlyGivenTypesAndSkipsUnchanged() {
        service.updatePreferences(user, Map.of(NotificationType.JOB_NEW, false, NotificationType.ON_TRACK_EMAIL, false));
        Map<NotificationType, Boolean> result = service.updatePreferences(user, Map.of(NotificationType.JOB_NEW, true, NotificationType.ON_TRACK_EMAIL, false));

        assertThat(result).containsEntry(NotificationType.JOB_NEW, true)
                .containsEntry(NotificationType.ON_TRACK_EMAIL, false)
                .containsEntry(NotificationType.GENERAL, true);
        assertThat(store).hasSize(2);
        // 2 creations + 1 change; the unchanged ON_TRACK_EMAIL isn't saved again
        verify(repository, times(3)).save(any());
    }
}
