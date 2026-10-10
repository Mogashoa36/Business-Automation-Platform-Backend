package company.backend.workflow.dto;

import java.time.Instant;
import java.util.List;

import company.backend.workflow.entity.WorkflowStatus;
import company.backend.workflow.entity.WorkflowStepStatus;
import company.backend.workflow.entity.WorkflowStepType;

/** Read model for a running workflow. */
public record WorkflowInstanceResponse(
        Long id,
        String reference,
        Long definitionId,
        String definitionCode,
        String definitionName,
        int definitionVersion,
        String title,
        String description,
        WorkflowStatus status,
        Long requesterId,
        String requesterName,
        Long currentAssigneeId,
        String currentAssigneeName,
        int currentStepIndex,
        String payload,
        List<Step> steps,
        Instant submittedAt,
        Instant completedAt,
        Instant createdAt) {

    /** A step of this particular run, including its decision history fields. */
    public record Step(
            Long id,
            String name,
            WorkflowStepType stepType,
            int stepOrder,
            WorkflowStepStatus status,
            Long assigneeId,
            String assigneeName,
            String decisionComment,
            Instant startedAt,
            Instant completedAt) {
    }
}