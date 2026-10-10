package company.backend.workflow.entity;

/**
 * Generic workflow lifecycle.
 *
 * <p>Deliberately industry neutral. A client project shapes its process by
 * defining steps, not by adding new states here.
 */
public enum WorkflowStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == COMPLETED || this == CANCELLED;
    }

    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }
}