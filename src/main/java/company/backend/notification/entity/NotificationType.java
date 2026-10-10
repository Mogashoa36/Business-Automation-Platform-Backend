package company.backend.notification.entity;

/**
 * What a notification is about.
 *
 * <p>The first group is severity (used for styling) and the rest are business
 * events. Client projects add rows/values as their processes grow; the enum is a
 * convenience for the template's own flows, not a closed set - unknown values are
 * persisted as-is because the column is a plain string.
 */
public enum NotificationType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,

    TASK_ASSIGNED,
    TASK_DUE,
    APPROVAL_REQUESTED,
    APPROVAL_DECIDED,
    WORKFLOW_ASSIGNED,
    WORKFLOW_DECIDED,
    COMMENT_ADDED,
    MENTION,
    SYSTEM
}