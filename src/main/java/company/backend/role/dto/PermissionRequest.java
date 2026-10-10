package company.backend.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for registering a client specific permission. */
public record PermissionRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be upper case letters, digits and underscores")
                String code,
        @Size(max = 150) String name,
        @Size(max = 500) String description,
        @Size(max = 50) String module) {
}