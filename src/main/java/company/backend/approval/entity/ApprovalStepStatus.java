package company.backend.approval.entity;

/** State of one stage of a multi-stage approval. */
public enum ApprovalStepStatus {
    PENDING,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    SKIPPED
}