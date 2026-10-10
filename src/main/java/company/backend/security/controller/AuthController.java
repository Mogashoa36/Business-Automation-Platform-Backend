package company.backend.security.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.security.dto.ForgotPasswordRequest;
import company.backend.security.dto.LoginRequest;
import company.backend.security.dto.LoginResponse;
import company.backend.security.dto.RefreshTokenRequest;
import company.backend.security.dto.ResetPasswordRequest;
import company.backend.security.service.AuthService;

import jakarta.validation.Valid;

/**
 * Authentication endpoints.
 *
 * <p>All of these are reachable without a token (see
 * {@code platform.security.public-endpoints}) except {@code /me} and
 * {@code /change-password}, which require an authenticated session.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Deliberately identical whether or not the address exists, so the endpoint
     * cannot be used to enumerate accounts.
     */
    private static final String GENERIC_RESET_MESSAGE =
            "If an account exists for that address, a password reset has been requested.";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Signed in", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Session refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.ok(GENERIC_RESET_MESSAGE);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password has been reset. Please sign in with your new password.");
    }
}