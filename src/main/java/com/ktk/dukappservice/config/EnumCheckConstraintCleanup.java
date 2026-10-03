package com.ktk.dukappservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate creates a CHECK constraint listing the enum values when it creates a table with an enum column, and
 * {@code ddl-auto=update} never updates it. For enums that grow over time this makes inserts of new values fail,
 * so these constraints are dropped on every startup (before roles are seeded).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnumCheckConstraintCleanup implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(EnumCheckConstraintCleanup.class);

    /** table -> constraint, for enums new values get added to (Permission, AuditAction, NotificationType). */
    private static final List<String[]> CONSTRAINTS = List.of(
            new String[]{"role_permissions", "role_permissions_permission_check"},
            new String[]{"audit_log", "audit_log_action_check"},
            new String[]{"notification_preferences", "notification_preferences_type_check"},
            new String[]{"notification_schedules", "notification_schedules_type_check"});

    private final JdbcTemplate jdbcTemplate;

    public EnumCheckConstraintCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** table.column pairs that became optional; {@code ddl-auto=update} never drops a NOT NULL. */
    private static final List<String[]> NOW_NULLABLE = List.of(
            new String[]{"jobs", "registration_deadline"},
            new String[]{"jobs", "cancellation_deadline"});

    @Override
    public void run(ApplicationArguments args) {
        for (String[] column : NOW_NULLABLE) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + column[0] + " ALTER COLUMN " + column[1] + " DROP NOT NULL");
            } catch (Exception e) {
                // The table doesn't exist yet on a fresh database; Hibernate creates the column nullable then.
                LOG.debug("Could not make {}.{} nullable", column[0], column[1], e);
            }
        }
        for (String[] constraint : CONSTRAINTS) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + constraint[0] + " DROP CONSTRAINT IF EXISTS " + constraint[1]);
            } catch (Exception e) {
                LOG.warn("Could not drop check constraint {} on {}", constraint[1], constraint[0], e);
            }
        }
    }
}
