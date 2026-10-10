package company.backend.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Forgot-password request.
 *
 * <p>The API always answers with the same generic message so it cannot be used to
 * enumerate which addresses have accounts.
 */
public record ForgotPasswordRequest(@NotBlank @Email String email) {
}