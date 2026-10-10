package company.backend.approval.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for approving or rejecting the current stage. */
public record ApprovalDecisionRequest(
        @NotNull Boolean approved,
        @Size(max = 2000) String comment) {
}