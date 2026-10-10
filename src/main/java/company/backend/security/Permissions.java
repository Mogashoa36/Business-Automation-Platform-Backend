package company.backend.security;

/**
 * Platform permission codes.
 *
 * <p>These are the generic capabilities every deployment needs. They are seeded into
 * the {@code permissions} table at startup and referenced from {@code @PreAuthorize}
 * expressions, so a typo becomes a compile error rather than an endpoint that is
 * silently locked down.
 *
 * <p>Client specific permissions are added as ordinary rows - no code change needed.
 */
public final class Permissions {

    private Permissions() {
    }

    // Dashboard
    public static final String DASHBOARD_VIEW = "DASHBOARD_VIEW";

    // User management
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";

    // Role and permission management
    public static final String ROLE_VIEW = "ROLE_VIEW";
    public static final String ROLE_CREATE = "ROLE_CREATE";
    public static final String ROLE_UPDATE = "ROLE_UPDATE";
    public static final String ROLE_DELETE = "ROLE_DELETE";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String PERMISSION_VIEW = "PERMISSION_VIEW";
    public static final String PERMISSION_MANAGE = "PERMISSION_MANAGE";

    // Organization and departments
    public static final String ORGANIZATION_VIEW = "ORGANIZATION_VIEW";
    public static final String ORGANIZATION_MANAGE = "ORGANIZATION_MANAGE";
    public static final String DEPARTMENT_VIEW = "DEPARTMENT_VIEW";
    public static final String DEPARTMENT_CREATE = "DEPARTMENT_CREATE";
    public static final String DEPARTMENT_UPDATE = "DEPARTMENT_UPDATE";
    public static final String DEPARTMENT_DELETE = "DEPARTMENT_DELETE";

    // Tasks
    public static final String TASK_VIEW = "TASK_VIEW";
    public static final String TASK_CREATE = "TASK_CREATE";
    public static final String TASK_UPDATE = "TASK_UPDATE";
    public static final String TASK_DELETE = "TASK_DELETE";
    public static final String TASK_ASSIGN = "TASK_ASSIGN";

    // Workflows
    public static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";
    public static final String WORKFLOW_MANAGE_DEFINITION = "WORKFLOW_MANAGE_DEFINITION";
    public static final String WORKFLOW_INSTANTIATE = "WORKFLOW_INSTANTIATE";
    public static final String WORKFLOW_CANCEL = "WORKFLOW_CANCEL";
    public static final String WORKFLOW_VIEW_ALL = "WORKFLOW_VIEW_ALL";

    // Approvals
    public static final String APPROVAL_VIEW = "APPROVAL_VIEW";
    public static final String APPROVAL_VIEW_ALL = "APPROVAL_VIEW_ALL";
    public static final String APPROVAL_REQUEST = "APPROVAL_REQUEST";
    public static final String APPROVAL_DECIDE = "APPROVAL_DECIDE";

    // Documents
    public static final String DOCUMENT_VIEW = "DOCUMENT_VIEW";
    public static final String DOCUMENT_UPLOAD = "DOCUMENT_UPLOAD";
    public static final String DOCUMENT_DELETE = "DOCUMENT_DELETE";

    // Notifications and comments
    public static final String NOTIFICATION_VIEW = "NOTIFICATION_VIEW";
    public static final String COMMENT_VIEW = "COMMENT_VIEW";
    public static final String COMMENT_CREATE = "COMMENT_CREATE";
    public static final String COMMENT_DELETE = "COMMENT_DELETE";

    // Audit and administration
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String SETTINGS_VIEW = "SETTINGS_VIEW";
    public static final String SETTINGS_MANAGE = "SETTINGS_MANAGE";
    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";
    public static final String MASTER_DATA_VIEW = "MASTER_DATA_VIEW";
    public static final String MASTER_DATA_MANAGE = "MASTER_DATA_MANAGE";
}