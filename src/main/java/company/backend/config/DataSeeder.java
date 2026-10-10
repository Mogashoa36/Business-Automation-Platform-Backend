package company.backend.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import company.backend.department.entity.Department;
import company.backend.department.repository.DepartmentRepository;
import company.backend.masterdata.entity.MasterDataItem;
import company.backend.masterdata.repository.MasterDataItemRepository;
import company.backend.organization.entity.Organization;
import company.backend.organization.repository.OrganizationRepository;
import company.backend.role.entity.Permission;
import company.backend.role.entity.Role;
import company.backend.role.repository.PermissionRepository;
import company.backend.role.repository.RoleRepository;
import company.backend.security.Permissions;
import company.backend.security.SystemRoles;
import company.backend.settings.entity.SystemSetting;
import company.backend.settings.repository.SystemSettingRepository;
import company.backend.user.entity.User;
import company.backend.user.entity.UserStatus;
import company.backend.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the minimum data a new deployment needs to be usable.
 *
 * <p>Every step checks before it writes, so restarting the application never
 * duplicates a row or resets a customised role. The four default roles are marked
 * as system roles, which prevents an administrator from deleting the role that
 * gives them access in the first place.
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SeedProperties properties;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final SystemSettingRepository settingRepository;
    private final MasterDataItemRepository masterDataRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            SeedProperties properties,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            OrganizationRepository organizationRepository,
            SystemSettingRepository settingRepository,
            MasterDataItemRepository masterDataRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.settingRepository = settingRepository;
        this.masterDataRepository = masterDataRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Data seeding is disabled (platform.seed.enabled=false)");
            return;
        }

        seedPermissions();
        seedRoles();
        seedDepartments();
        seedOrganization();
        seedSettings();
        seedMasterData();
        seedAdministrator();
    }

    /** Registers the platform's permission codes, keeping any local additions. */
    private void seedPermissions() {
        for (PermissionDefinition definition : permissionDefinitions()) {
            if (permissionRepository.existsByCode(definition.code())) {
                continue;
            }
            Permission permission = new Permission();
            permission.setCode(definition.code());
            permission.setName(definition.name());
            permission.setDescription("Grants permission to " + definition.module().toLowerCase(java.util.Locale.ROOT) + " actions");
            permission.setModule(definition.module());
            permissionRepository.save(permission);
        }
        log.info("Permission catalogue verified ({} codes)", permissionDefinitions().size());
    }

    /** Default role names; only ADMIN is granted everything, and the rest stay editable. */
    private void seedRoles() {
        seedRole(SystemRoles.ADMIN, "Administrator", "Full access to every module.");
        seedRole(SystemRoles.MANAGER, "Manager", "Supervises work and approves requests.");
        seedRole(SystemRoles.EMPLOYEE, "Employee", "Handles day-to-day work.");
        seedRole(SystemRoles.VIEWER, "Viewer", "Read-only access.");
    }

    /**
     * Creates the role if absent, and tops up the administrator's permissions.
     *
     * <p>The top-up runs on every start so a permission added by a newer version
     * of the template reaches an existing deployment automatically.
     */
    private void seedRole(String code, String name, String description) {
        boolean isAdmin = SystemRoles.ADMIN.equals(code);

        roleRepository.findByCode(code).ifPresentOrElse(existing -> {
            if (isAdmin && existing.getPermissions().size() < permissionRepository.count()) {
                existing.setPermissions(new java.util.LinkedHashSet<>(permissionRepository.findAll()));
                roleRepository.save(existing);
            }
        }, () -> {
            Role created = new Role();
            created.setCode(code);
            created.setName(name);
            created.setDescription(description);
            created.setSystemRole(true);
            created.setActive(true);
            if (isAdmin) {
                created.setPermissions(new java.util.LinkedHashSet<>(permissionRepository.findAll()));
            }
            roleRepository.save(created);
        });
    }

    private void seedDepartments() {
        for (String code : properties.getDepartmentCodes()) {
            if (departmentRepository.existsByCodeIgnoreCase(code)) {
                continue;
            }
            Department department = new Department();
            department.setCode(code.toUpperCase(java.util.Locale.ROOT));
            department.setName(humanize(department.getCode()));
            department.setDescription("Seeded department; rename or remove it for your organisation");
            department.setActive(true);
            departmentRepository.save(department);
        }
    }

    private void seedOrganization() {
        if (organizationRepository.findFirstByOrderByIdAsc().isPresent()) {
            return;
        }
        Organization organization = new Organization();
        organization.setName(properties.getOrganizationName());
        organization.setTimezone(java.time.ZoneId.systemDefault().getId());
        organization.setCurrency("EUR");
        organization.setDateFormat("yyyy-MM-dd");
        organizationRepository.save(organization);
        log.info("Created a default company profile; edit it under Organization");
    }

    private void seedSettings() {
        seedSetting("platform.default.pageSize", "20", "NUMBER", "GENERAL", "Rows shown per page by default", false);
        seedSetting("platform.date.format", "yyyy-MM-dd", "STRING", "GENERAL", "Date display format", true);
        seedSetting("platform.date.timezone", "UTC", "STRING", "GENERAL", "Timezone used for reporting", false);
        seedSetting("security.password.minLength", "8", "NUMBER", "SECURITY", "Minimum accepted password length", true);
        seedSetting("security.session.timeoutMinutes", "30", "NUMBER", "SECURITY", "Access token lifetime", true);
        seedSetting("notification.email.enabled", "false", "BOOLEAN", "NOTIFICATIONS", "Send e-mail notifications", true);
        seedSetting("storage.maxFileSizeMb", "10", "NUMBER", "STORAGE", "Maximum upload size in megabytes", true);
    }

    private void seedSetting(
            String key, String value, String type, String group, String description, boolean editable) {
        if (settingRepository.findByKey(key).isPresent()) {
            return;
        }
        SystemSetting setting = new SystemSetting();
        setting.setKey(key);
        setting.setValue(value);
        setting.setValueType(type);
        setting.setGroup(group);
        setting.setDescription(description);
        setting.setEditable(editable);
        settingRepository.save(setting);
    }

    private void seedMasterData() {
        String[] categories = {"CONTRACT", "INVOICE", "REPORT", "POLICY", "OTHER"};
        for (int index = 0; index < categories.length; index++) {
            if (masterDataRepository.existsByGroupAndCode("DOCUMENT_CATEGORY", categories[index])) {
                continue;
            }
            MasterDataItem item = new MasterDataItem();
            item.setGroup("DOCUMENT_CATEGORY");
            item.setCode(categories[index]);
            item.setLabel(humanize(categories[index]));
            item.setSortOrder(index);
            item.setActive(true);
            item.setSystemManaged(false);
            masterDataRepository.save(item);
        }
    }

    /**
     * Creates the first administrator, but only when a password was supplied.
     *
     * <p>Refusing to invent a credential is the whole point: a template that ships a
     * default {@code admin/admin} password is a template that gets deployed that
     * way.
     */
    private void seedAdministrator() {
        if (userRepository.count() > 0) {
            return;
        }
        if (properties.getAdminPassword() == null || properties.getAdminPassword().isBlank()) {
            log.warn("""

                    No administrator account was created because platform.seed.admin-password is not set.
                    Set PLATFORM_SEED_ADMIN_PASSWORD to create the first account. Without it the
                    application starts with no way to sign in, which is deliberate: shipping a
                    well-known default password would be worse.
                    """);
            return;
        }

        Role adminRole = roleRepository
                .findByCode(SystemRoles.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role was not seeded"));

        User admin = new User();
        admin.setUsername(properties.getAdminUsername());
        admin.setEmail(properties.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(properties.getAdminPassword()));
        admin.setFirstName(properties.getAdminFirstName());
        admin.setLastName(properties.getAdminLastName());
        admin.setJobTitle("Administrator");
        admin.setStatus(UserStatus.ACTIVE);
        // A seeded credential must be rotated on first sign-in.
        admin.setMustChangePassword(true);
        admin.setPasswordChangedAt(java.time.Instant.now());
        admin.setRoles(new java.util.LinkedHashSet<>(java.util.List.of(adminRole)));
        userRepository.save(admin);

        log.info("Created administrator account '{}'. Change the password on first sign-in.", admin.getUsername());
    }

    private static String humanize(String code) {
        String lower = code.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * One entry of the platform permission catalogue.
     *
     * <p>The display name is derived from the code, so adding a constant to
     * {@link Permissions} is enough to register it here - there is no second
     * place to forget to update.
     */
    private record PermissionDefinition(String code, String name, String module) {

        private static PermissionDefinition of(String code, String module) {
            return new PermissionDefinition(code, humanize(code), module);
        }
    }

    private static List<PermissionDefinition> permissionDefinitions() {
        return List.of(
                PermissionDefinition.of(Permissions.DASHBOARD_VIEW, "DASHBOARD"),
                PermissionDefinition.of(Permissions.USER_VIEW, "USER"),
                PermissionDefinition.of(Permissions.USER_CREATE, "USER"),
                PermissionDefinition.of(Permissions.USER_UPDATE, "USER"),
                PermissionDefinition.of(Permissions.USER_DELETE, "USER"),
                PermissionDefinition.of(Permissions.USER_STATUS_CHANGE, "USER"),
                PermissionDefinition.of(Permissions.USER_PASSWORD_RESET, "USER"),
                PermissionDefinition.of(Permissions.ROLE_VIEW, "ROLE"),
                PermissionDefinition.of(Permissions.ROLE_CREATE, "ROLE"),
                PermissionDefinition.of(Permissions.ROLE_UPDATE, "ROLE"),
                PermissionDefinition.of(Permissions.ROLE_DELETE, "ROLE"),
                PermissionDefinition.of(Permissions.ROLE_ASSIGN, "ROLE"),
                PermissionDefinition.of(Permissions.PERMISSION_VIEW, "ROLE"),
                PermissionDefinition.of(Permissions.PERMISSION_MANAGE, "ROLE"),
                PermissionDefinition.of(Permissions.ORGANIZATION_VIEW, "ORGANIZATION"),
                PermissionDefinition.of(Permissions.ORGANIZATION_MANAGE, "ORGANIZATION"),
                PermissionDefinition.of(Permissions.DEPARTMENT_VIEW, "DEPARTMENT"),
                PermissionDefinition.of(Permissions.DEPARTMENT_CREATE, "DEPARTMENT"),
                PermissionDefinition.of(Permissions.DEPARTMENT_UPDATE, "DEPARTMENT"),
                PermissionDefinition.of(Permissions.DEPARTMENT_DELETE, "DEPARTMENT"),
                PermissionDefinition.of(Permissions.TASK_VIEW, "TASK"),
                PermissionDefinition.of(Permissions.TASK_CREATE, "TASK"),
                PermissionDefinition.of(Permissions.TASK_UPDATE, "TASK"),
                PermissionDefinition.of(Permissions.TASK_DELETE, "TASK"),
                PermissionDefinition.of(Permissions.TASK_ASSIGN, "TASK"),
                PermissionDefinition.of(Permissions.WORKFLOW_VIEW, "WORKFLOW"),
                PermissionDefinition.of(Permissions.WORKFLOW_MANAGE_DEFINITION, "WORKFLOW"),
                PermissionDefinition.of(Permissions.WORKFLOW_INSTANTIATE, "WORKFLOW"),
                PermissionDefinition.of(Permissions.WORKFLOW_CANCEL, "WORKFLOW"),
                PermissionDefinition.of(Permissions.WORKFLOW_VIEW_ALL, "WORKFLOW"),
                PermissionDefinition.of(Permissions.APPROVAL_VIEW, "APPROVAL"),
                PermissionDefinition.of(Permissions.APPROVAL_VIEW_ALL, "APPROVAL"),
                PermissionDefinition.of(Permissions.APPROVAL_REQUEST, "APPROVAL"),
                PermissionDefinition.of(Permissions.APPROVAL_DECIDE, "APPROVAL"),
                PermissionDefinition.of(Permissions.DOCUMENT_VIEW, "DOCUMENT"),
                PermissionDefinition.of(Permissions.DOCUMENT_UPLOAD, "DOCUMENT"),
                PermissionDefinition.of(Permissions.DOCUMENT_DELETE, "DOCUMENT"),
                PermissionDefinition.of(Permissions.NOTIFICATION_VIEW, "NOTIFICATION"),
                PermissionDefinition.of(Permissions.COMMENT_VIEW, "COMMENT"),
                PermissionDefinition.of(Permissions.COMMENT_CREATE, "COMMENT"),
                PermissionDefinition.of(Permissions.COMMENT_DELETE, "COMMENT"),
                PermissionDefinition.of(Permissions.AUDIT_VIEW, "AUDIT"),
                PermissionDefinition.of(Permissions.SETTINGS_VIEW, "SETTINGS"),
                PermissionDefinition.of(Permissions.SETTINGS_MANAGE, "SETTINGS"),
                PermissionDefinition.of(Permissions.REPORT_VIEW, "REPORT"),
                PermissionDefinition.of(Permissions.REPORT_EXPORT, "REPORT"),
                PermissionDefinition.of(Permissions.MASTER_DATA_VIEW, "MASTER_DATA"),
                PermissionDefinition.of(Permissions.MASTER_DATA_MANAGE, "MASTER_DATA"));
    }
}