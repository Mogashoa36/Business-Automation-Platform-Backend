package company.backend.workflow.dto;

import java.time.Instant;
import java.util.List;

import company.backend.workflow.entity.WorkflowStepType;

/** Read model for a workflow template. */
public record WorkflowDefinitionResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active,
        int version,
        List<Step> steps,
        Instant createdAt,
        Instant updatedAt) {

    /** A step of the template. */
    public record Step(
            Long id,
            String name,
            WorkflowStepType stepType,
            int stepOrder,
            String assigneeRoleCode,
            boolean allowRejection,
            String instructions) {
    }
}