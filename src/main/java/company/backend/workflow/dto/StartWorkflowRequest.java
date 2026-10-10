package company.backend.workflow.dto;

import java.time.Instant;
import java.util.List;

import company.backend.workflow.entity.WorkflowStatus;
import company.backend.workflow.entity.WorkflowStepStatus;
import company.backend.workflow.entity.WorkflowStepType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for starting a workflow instance. */
public record StartWorkflowRequest(
        @NotNull Long definitionId,
        @NotBlank @Size(max = 250) String title,
        @Size(max = 4000) String description,
        @Size(max = 4000) String payload,
        /** When true the instance is submitted immediately instead of saved as a draft. */
        Boolean submitImmediately) {

    public boolean shouldSubmit() {
        return Boolean.TRUE.equals(submitImmediately);
    }
}