package company.backend.workflow.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating a workflow template. */
public record WorkflowDefinitionRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        Boolean active,
        @NotEmpty @Size(max = 50) List<@Valid WorkflowStepRequest> steps) {

    /** One ordered step of the process. */
    public record WorkflowStepRequest(
            @NotBlank @Size(max = 150) String name,
            String stepType,
            String assigneeRoleCode,
            Boolean allowRejection,
            @Size(max = 1000) String instructions) {
    }
}