package company.backend.audit;

/** Canonical action names written to {@code audit_logs.action}. */
public final class AuditAction {

    private AuditAction() {
    }

    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String STATUS_CHANGE = "STATUS_CHANGE";
    public static final String ASSIGN = "ASSIGN";
    public static final String APPROVE = "APPROVE";
    public static final String REJECT = "REJECT";
    public static final String SUBMIT = "SUBMIT";
    public static final String COMPLETE = "COMPLETE";
    public static final String CANCEL = "CANCEL";
    public static final String UPLOAD = "UPLOAD";
    public static final String VIEW = "VIEW";
}