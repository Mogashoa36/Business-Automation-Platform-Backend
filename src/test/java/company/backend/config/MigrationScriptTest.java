package company.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Executes the real Flyway migration scripts against H2 in PostgreSQL mode.
 *
 * <p>The normal test profile builds its schema from the JPA entities, which is
 * fast but says nothing about the SQL production actually runs. This test closes
 * that gap: a migration with a typo, a bad constraint or a non-portable statement
 * fails here instead of on a client's first boot.
 *
 * <p>Deliberately a plain JUnit test with its own DataSource rather than a
 * {@code @SpringBootTest}. What is under test is the SQL; involving the whole
 * application context would only add profile-ordering surprises.
 */
class MigrationScriptTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void migrateFreshDatabase() {
        // DB_CLOSE_DELAY=-1 is essential: without it H2 drops an in-memory database
        // as soon as the last connection closes, which Flyway does right after
        // migrating - leaving the JdbcTemplate to open a brand new empty database.
        // A unique name per test keeps them independent of execution order.
        String url = "jdbc:h2:mem:migration-" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("the baseline migration applies cleanly and creates every table")
    void baselineMigrationIsValid() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tables).contains(
                "permissions", "roles", "role_permissions", "organizations", "departments",
                "users", "user_roles", "refresh_tokens", "password_reset_tokens", "audit_logs",
                "system_settings", "master_data_items", "tasks", "documents", "document_links",
                "comments", "workflow_definitions", "workflow_step_definitions", "workflow_instances",
                "workflow_instance_steps", "approvals", "approval_steps", "approval_actions",
                "notifications", "notification_preferences");
    }

    @Test
    @DisplayName("identity columns generate keys without a separate sequence")
    void identityColumnsWork() {
        insertPermission("IDENTITY_TEST");
        Long roleId = insertRole("IDENTITY_TEST");

        assertThat(jdbcTemplate.queryForObject("SELECT id FROM permissions WHERE code = 'IDENTITY_TEST'", Long.class))
                .isPositive();
        assertThat(roleId).isPositive();
    }

    @Test
    @DisplayName("a department's parent is cleared instead of blocking the delete")
    void departmentParentIsClearedOnDelete() {
        jdbcTemplate.update(
                "INSERT INTO departments (code, name, active, created_at, updated_at) "
                        + "VALUES ('PARENT_DEPT', 'Parent', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        Long parentId = jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE code = 'PARENT_DEPT'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO departments (code, name, parent_id, active, created_at, updated_at) "
                        + "VALUES ('CHILD_DEPT', 'Child', ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                parentId);
        Long childId = jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE code = 'CHILD_DEPT'", Long.class);

        // ON DELETE SET NULL, not CASCADE: the child survives as a root department.
        jdbcTemplate.update("DELETE FROM departments WHERE id = ?", parentId);

        assertThat(jdbcTemplate.queryForObject("SELECT parent_id FROM departments WHERE id = ?", Long.class, childId))
                .isNull();
    }

    @Test
    @DisplayName("removing a role cascades to its permission grants")
    void rolePermissionGrantsAreCascaded() {
        Long roleId = insertRole("CASCADE_ROLE");
        Long permissionId = insertPermission("CASCADE_PERMISSION");

        jdbcTemplate.update(
                "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, permissionId);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", Long.class, roleId))
                .isEqualTo(1L);

        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", roleId);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", Long.class, roleId))
                .isZero();
    }

    @Test
    @DisplayName("unique constraints reject duplicate business keys")
    void uniqueConstraintsAreEnforced() {
        insertPermission("UNIQUE_TEST");
        assertThrows(DataIntegrityViolationException.class, () -> insertPermission("UNIQUE_TEST"));
    }

    @Test
    @DisplayName("not-null constraints are enforced")
    void notNullConstraintsAreEnforced() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO permissions (code, module, created_at, updated_at) "
                                + "VALUES ('NO_NAME', 'TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"));
    }

    @Test
    @DisplayName("the enum check constraints are present in the schema")
    void enumCheckConstraintsAreDefined() {
        // Existence only - enforcement is deliberately NOT asserted here. H2's
        // PostgreSQL compatibility mode stores a column reference inside a CHECK as
        // a case-sensitive quoted identifier ("status"), which no longer matches the
        // lower-cased column, so it rejects every value including valid ones. The
        // constraints are standard SQL and correct on PostgreSQL, the production
        // engine; they are asserted as present here and enforced by the JPA enums
        // and bean validation in the application layer. See docs/database.md.
        List<String> checks = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.check_constraints "
                        + "WHERE constraint_name LIKE 'ck_%' ORDER BY constraint_name",
                String.class);

        assertThat(checks).contains(
                "ck_users_status",
                "ck_tasks_status",
                "ck_tasks_priority",
                "ck_workflow_instances_status",
                "ck_approvals_status");
    }

    @Test
    @DisplayName("the departments.manager_id column exists, matching the plain-column mapping")
    void managerColumnExists() {
        // Checked by column presence rather than by index name: index naming is
        // engine-specific, the column and the migration applying cleanly are not.
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'departments' AND column_name = 'manager_id'",
                String.class);

        assertThat(columns).containsExactly("manager_id");
    }

    // ------------------------------------------------------------------ helpers
    // Each test uses its own code so ordering between them cannot matter.

    private Long insertPermission(String code) {
        jdbcTemplate.update(
                "INSERT INTO permissions (code, name, module, created_at, updated_at) "
                        + "VALUES (?, ?, 'TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                code,
                "Test permission");
        return jdbcTemplate.queryForObject("SELECT id FROM permissions WHERE code = ?", Long.class, code);
    }

    private Long insertRole(String code) {
        jdbcTemplate.update(
                "INSERT INTO roles (code, name, active, system_role, created_at, updated_at) "
                        + "VALUES (?, ?, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                code,
                "Test role");
        return jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = ?", Long.class, code);
    }
}