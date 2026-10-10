package company.backend.workflow.entity;

/** State of a single step within a running workflow instance. */
public enum WorkflowStepStatus {
    PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    SKIPPED
}