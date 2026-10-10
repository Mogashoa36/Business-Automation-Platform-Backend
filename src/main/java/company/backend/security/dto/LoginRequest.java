package company.backend.security.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials submitted to {@code POST /api/auth/login}. */
public record LoginRequest(
        @NotBlank(message = "Username or email is required") String username,
        @NotBlank(message = "Password is required") String password) {
}