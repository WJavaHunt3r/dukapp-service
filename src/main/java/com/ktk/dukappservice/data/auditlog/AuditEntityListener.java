package com.ktk.dukappservice.data.auditlog;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.paceteamround.PaceTeamRound;
import com.ktk.dukappservice.data.paceuserround.PaceUserRound;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.teamrounds.TeamRound;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.data.userrounds.UserRound;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.enums.AuditAction;
import com.ktk.dukappservice.security.RefreshToken;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Writes a CREATE / UPDATE / DELETE audit entry for every committed change of a JPA entity.
 * Uses Hibernate's post-commit events, so rolled-back changes are not logged.
 * <p>
 * Not covered: bulk JPQL updates/deletes ({@code @Modifying @Query}) and changes that only touch a collection
 * (e.g. a user's roles) — those are logged explicitly where they happen.
 */
@Component
public class AuditEntityListener implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(AuditEntityListener.class);

    /** Bookkeeping and derived statistics that are recalculated constantly. */
    private static final Set<Class<?>> EXCLUDED_ENTITIES = Set.of(
            AuditLog.class, RefreshToken.class, UserStatus.class, UserRound.class,
            PaceUserRound.class, PaceTeamRound.class, TeamRound.class);

    /** Recalculated fields: an update that only changes these is not logged. */
    private static final Map<Class<?>, Set<String>> DERIVED_FIELDS = Map.of(
            User.class, Set.of(User.Fields.points, User.Fields.currentMyShareCredit),
            Team.class, Set.of(Team.Fields.points),
            Round.class, Set.of(Round.Fields.samvirkChurchStatus));

    private static final int MAX_VALUE_LENGTH = 500;
    private static final String MASK = "***";

    private final AuditLogService auditLogService;
    private final EntityManagerFactory entityManagerFactory;

    public AuditEntityListener(AuditLogService auditLogService, EntityManagerFactory entityManagerFactory) {
        this.auditLogService = auditLogService;
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getEventListenerRegistry();
        registry.appendListeners(EventType.POST_COMMIT_INSERT, this);
        registry.appendListeners(EventType.POST_COMMIT_UPDATE, this);
        registry.appendListeners(EventType.POST_COMMIT_DELETE, this);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return isAudited(persister);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (isAudited(event.getPersister())) {
            safely(() -> auditLogService.record(AuditAction.CREATE, entityType(event.getPersister()), event.getId(),
                    snapshot(event.getPersister(), event.getState())));
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (isAudited(event.getPersister())) {
            safely(() -> {
                Map<String, Object> changes = changes(event);
                if (!changes.isEmpty()) {
                    auditLogService.record(AuditAction.UPDATE, entityType(event.getPersister()), event.getId(), changes);
                }
            });
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (isAudited(event.getPersister())) {
            safely(() -> auditLogService.record(AuditAction.DELETE, entityType(event.getPersister()), event.getId(),
                    snapshot(event.getPersister(), event.getDeletedState())));
        }
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    }

    private static boolean isAudited(EntityPersister persister) {
        return !EXCLUDED_ENTITIES.contains(persister.getMappedClass());
    }

    private static String entityType(EntityPersister persister) {
        return persister.getMappedClass().getSimpleName();
    }

    private static Map<String, Object> snapshot(EntityPersister persister, Object[] state) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (state == null) {
            return values;
        }
        String[] names = persister.getPropertyNames();
        for (int i = 0; i < names.length; i++) {
            Object value = state[i];
            if (value == null || (value instanceof Collection<?> c && !Hibernate.isInitialized(c))) {
                continue;
            }
            values.put(names[i], isSensitive(names[i]) ? MASK : format(value));
        }
        return values;
    }

    private static Map<String, Object> changes(PostUpdateEvent event) {
        Map<String, Object> changes = new LinkedHashMap<>();
        Object[] oldState = event.getOldState();
        Object[] newState = event.getState();
        String[] names = event.getPersister().getPropertyNames();
        Set<String> derived = DERIVED_FIELDS.getOrDefault(event.getPersister().getMappedClass(), Set.of());

        int[] dirty = event.getDirtyProperties();
        if (dirty == null) {
            dirty = java.util.stream.IntStream.range(0, names.length)
                    .filter(i -> oldState == null || !Objects.equals(oldState[i], newState[i]))
                    .toArray();
        }
        for (int i : dirty) {
            if (derived.contains(names[i])) {
                continue;
            }
            Map<String, Object> change = new LinkedHashMap<>();
            boolean sensitive = isSensitive(names[i]);
            change.put("old", oldState == null ? null : sensitive ? MASK : format(oldState[i]));
            change.put("new", sensitive ? MASK : format(newState[i]));
            changes.put(names[i], change);
        }
        return changes;
    }

    private static boolean isSensitive(String property) {
        String name = property.toLowerCase(Locale.ROOT);
        return name.contains("password") || name.contains("secret") || name.contains("token");
    }

    /** Entities become their id, collections lists, everything else a (truncated) string. */
    private static Object format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getIdentifier();
        }
        if (value instanceof BaseEntity<?, ?> entity) {
            return entity.getId();
        }
        if (value instanceof Collection<?> collection) {
            return Hibernate.isInitialized(collection) ? collection.stream().map(AuditEntityListener::format).toList() : "(not loaded)";
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        String text = value.toString();
        return text.length() > MAX_VALUE_LENGTH ? text.substring(0, MAX_VALUE_LENGTH) + "…" : text;
    }

    /** Auditing must never break the request that made the change. */
    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            LOG.error("Failed to create audit log entry", e);
        }
    }
}
