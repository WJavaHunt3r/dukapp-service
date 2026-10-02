package com.ktk.dukappservice.data.auditlog;

import com.ktk.dukappservice.data.donation.Donation;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.AuditAction;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditEntityListenerTest {

    @Mock AuditLogService auditLogService;
    @Mock EntityManagerFactory entityManagerFactory;

    private AuditEntityListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuditEntityListener(auditLogService, entityManagerFactory);
    }

    @Test
    void derivedStatisticsAndBookkeepingAreNotAudited() {
        assertThat(listener.requiresPostCommitHandling(persister(UserStatus.class))).isFalse();
        assertThat(listener.requiresPostCommitHandling(persister(AuditLog.class))).isFalse();
        assertThat(listener.requiresPostCommitHandling(persister(Donation.class))).isTrue();

        listener.onPostInsert(new PostInsertEvent(new UserStatus(), 1L, new Object[0], persister(UserStatus.class), null));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void insertLogsNonNullFieldsWithEntityReferencesAsIds() {
        Team team = new Team();
        team.setId(3L);
        EntityPersister persister = persister(User.class, "firstname", "team", "email", "password");

        listener.onPostInsert(new PostInsertEvent(new User(), 7L, new Object[]{"Anna", team, null, "{bcrypt}hash"}, persister, null));

        assertThat(recorded(AuditAction.CREATE, "User", 7L))
                .containsExactly(Map.entry("firstname", "Anna"), Map.entry("team", 3L), Map.entry("password", "***"));
    }

    @Test
    void updateLogsOnlyChangedFieldsWithOldAndNewValues() {
        EntityPersister persister = persister(User.class, "firstname", "lastname", "points", "password");
        Object[] oldState = {"Anna", "Kis", 10.0, "old"};
        Object[] newState = {"Anna", "Nagy", 12.0, "new"};

        listener.onPostUpdate(new PostUpdateEvent(new User(), 7L, newState, oldState, new int[]{1, 2, 3}, persister, null));

        assertThat(recorded(AuditAction.UPDATE, "User", 7L)).containsExactly(
                Map.entry("lastname", Map.of("old", "Kis", "new", "Nagy")),
                Map.entry("password", Map.of("old", "***", "new", "***")));
    }

    @Test
    void updateOfOnlyRecalculatedFieldsIsSkipped() {
        EntityPersister persister = persister(User.class, "firstname", "points", "currentMyShareCredit");

        listener.onPostUpdate(new PostUpdateEvent(new User(), 7L, new Object[]{"Anna", 12.0, 5}, new Object[]{"Anna", 10.0, 4}, new int[]{1, 2}, persister, null));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateWithoutDirtyInfoComparesStates() {
        EntityPersister persister = persister(Donation.class, "description", "goal");

        listener.onPostUpdate(new PostUpdateEvent(new Donation(), 2L, new Object[]{"Camp", 500}, new Object[]{"Camp", 400}, null, persister, null));

        assertThat(recorded(AuditAction.UPDATE, "Donation", 2L)).containsOnlyKeys("goal");
    }

    @Test
    void deleteLogsDeletedStateAndCollections() {
        EntityPersister persister = persister(Donation.class, "description", "tags");

        listener.onPostDelete(new PostDeleteEvent(new Donation(), 2L, new Object[]{"Camp", List.of("a", "b")}, persister, null));

        assertThat(recorded(AuditAction.DELETE, "Donation", 2L))
                .containsExactly(Map.entry("description", "Camp"), Map.entry("tags", List.of("a", "b")));
    }

    @Test
    void auditFailuresDoNotPropagate() {
        doThrow(new RuntimeException("boom")).when(auditLogService).record(any(), any(), any(), any());

        listener.onPostDelete(new PostDeleteEvent(new Donation(), 2L, new Object[]{"Camp"}, persister(Donation.class, "description"), null));
    }

    private static EntityPersister persister(Class<?> type, String... properties) {
        EntityPersister persister = mock(EntityPersister.class);
        doReturn(type).when(persister).getMappedClass();
        when(persister.getPropertyNames()).thenReturn(properties);
        return persister;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> recorded(AuditAction action, String entityType, Object id) {
        ArgumentCaptor<Map<String, ?>> details = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(action), eq(entityType), eq(id), details.capture());
        return (Map<String, Object>) details.getValue();
    }
}
