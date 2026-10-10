package company.backend.security.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload for exchanging a refresh token for a new access token. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}