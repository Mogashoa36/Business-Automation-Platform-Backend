package company.backend.approval.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import company.backend.approval.entity.ApprovalStatus;
import company.backend.approval.entity.ApprovalStepStatus;

/** Read model for an approval request, including its stage history. */
public record ApprovalResponse(
        Long id,
        String reference,
        String title,
        String description,
        ApprovalStatus status,
        Long requesterId,
        String requesterName,
        BigDecimal amount,
        String currency,
        String category,
        String sourceEntityType,
        Long sourceEntityId,
        int currentStepIndex,
        List<Step> steps,
        Instant submittedAt,
        Instant completedAt,
        Instant createdAt) {

    /** One stage of this request. */
    public record Step(
            Long id,
            String name,
            int stepOrder,
            ApprovalStepStatus status,
            Long approverId,
            String approverName,
            String approverRoleCode,
            String comment,
            Instant decidedAt,
            Long decidedById) {
    }
}