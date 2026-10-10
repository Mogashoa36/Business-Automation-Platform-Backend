package company.backend.user.dto;

import java.util.List;

import company.backend.user.entity.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for editing a user.
 *
 * <p>Email is immutable here on purpose - changing a login identifier should be an
 * explicit, audited re-issue rather than a side effect of editing a phone number.
 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 150) String jobTitle,
        UserStatus status,
        Long departmentId,
        List<String> roleCodes) {
}