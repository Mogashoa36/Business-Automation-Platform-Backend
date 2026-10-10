package company.backend.user.dto;

import java.util.List;

import company.backend.user.entity.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating a user. */
public record CreateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 50) String phone,
        @Size(max = 150) String jobTitle,
        UserStatus status,
        Long departmentId,
        List<String> roleCodes) {
}