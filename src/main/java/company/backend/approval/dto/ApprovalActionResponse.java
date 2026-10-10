package company.backend.approval.dto;

import java.time.Instant;

/** Read model for a recorded decision. */
public record ApprovalActionResponse(
        Long id,
        Long stepId,
        String stepName,
        Long actorId,
        String actorName,
        String action,
        String comment,
        Instant createdAt) {
}