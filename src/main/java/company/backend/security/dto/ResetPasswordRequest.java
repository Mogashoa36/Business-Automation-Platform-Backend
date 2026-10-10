package company.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for redeeming a password reset token. */
public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{20,}$", message = "Invalid reset token") String token,
        @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String newPassword) {
}