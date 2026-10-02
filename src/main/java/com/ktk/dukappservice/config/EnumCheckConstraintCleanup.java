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

    /** table -> constraint, for enums new values get added to (Permission, AuditAction). */
    private static final List<String[]> CONSTRAINTS = List.of(
            new String[]{"role_permissions", "role_permissions_permission_check"},
            new String[]{"audit_log", "audit_log_action_check"});

    private final JdbcTemplate jdbcTemplate;

    public EnumCheckConstraintCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String[] constraint : CONSTRAINTS) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + constraint[0] + " DROP CONSTRAINT IF EXISTS " + constraint[1]);
            } catch (Exception e) {
                LOG.warn("Could not drop check constraint {} on {}", constraint[1], constraint[0], e);
            }
        }
    }
}
