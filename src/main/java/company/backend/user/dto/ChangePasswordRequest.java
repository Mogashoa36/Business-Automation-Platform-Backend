package company.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service password change; requires the current password to be supplied. */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank @Size(min = 8, max = 100, message = "New password must be at least 8 characters")
                String newPassword) {
}