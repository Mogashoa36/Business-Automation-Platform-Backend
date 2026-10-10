package company.backend.security.dto;

import java.time.Instant;

import company.backend.user.dto.CurrentUserResponse;

/** Result of a successful sign-in or token refresh. */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt,
        CurrentUserResponse user) {

    public static LoginResponse of(String accessToken, String refreshToken, Instant expiresAt, CurrentUserResponse user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresAt, user);
    }
}