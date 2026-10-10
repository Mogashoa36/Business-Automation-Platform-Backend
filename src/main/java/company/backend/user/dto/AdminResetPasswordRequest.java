package company.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Administrative password reset.
 *
 * <p>{@code mustChangePassword} is a boxed Boolean so the service can tell "not
 * supplied" from "explicitly false" and apply the secure default of forcing a
 * change on the next sign-in.
 */
public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String newPassword,
        Boolean mustChangePassword) {

    /** Effective flag after applying the secure default. */
    public boolean requiresPasswordChange() {
        return mustChangePassword == null || mustChangePassword;
    }
}