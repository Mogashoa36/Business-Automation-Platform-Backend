package company.backend.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for attaching an existing document to a business record. */
public record LinkDocumentRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Entity type must be upper case")
                String entityType,
        Long entityId) {
}