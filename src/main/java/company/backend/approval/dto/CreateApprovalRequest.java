package company.backend.approval.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for creating an approval request with its ordered stages. */
public record CreateApprovalRequest(
        @NotBlank @Size(max = 250) String title,
        @Size(max = 4000) String description,
        @Size(max = 50) String category,
        @DecimalMin(value = "0.0", message = "Amount cannot be negative") BigDecimal amount,
        @Size(max = 10) String currency,
        @NotEmpty @Size(max = 20) List<@Valid StagePayload> stages,
        /** When true the request is sent for approval straight away. */
        Boolean submitImmediately) {

    /** One stage of the chain. */
    public record StagePayload(
            @NotBlank @Size(max = 150) String name,
            /** Role code whose members may act on this stage. */
            @Size(max = 50) String approverRoleCode,
            /** A specific person, when the stage is not delegated to a role. */
            Long approverId) {
    }

    public boolean shouldSubmit() {
        return Boolean.TRUE.equals(submitImmediately);
    }
}