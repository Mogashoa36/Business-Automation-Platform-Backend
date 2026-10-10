package company.backend.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for approving or rejecting the current step. */
public record WorkflowDecisionRequest(
        @NotNull Boolean approved,
        @Size(max = 2000) String comment) {
}