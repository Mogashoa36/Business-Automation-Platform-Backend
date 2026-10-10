package company.backend.role.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating a role. */
public record RoleRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be upper case letters, digits and underscores")
                String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        Boolean active,
        List<Long> permissionIds) {
}