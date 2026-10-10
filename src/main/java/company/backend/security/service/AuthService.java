package company.backend.security.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import company.backend.audit.AuditAction;
import company.backend.audit.service.AuditService;
import company.backend.config.JwtProperties;
import company.backend.exception.BusinessException;
import company.backend.security.JwtService;
import company.backend.security.PlatformUserPrincipal;
import company.backend.security.SecurityUtils;
import company.backend.security.dto.ForgotPasswordRequest;
import company.backend.security.dto.LoginRequest;
import company.backend.security.dto.LoginResponse;
import company.backend.security.dto.RefreshTokenRequest;
import company.backend.security.dto.ResetPasswordRequest;
import company.backend.security.entity.PasswordResetToken;
import company.backend.security.entity.RefreshToken;
import company.backend.security.repository.PasswordResetTokenRepository;
import company.backend.security.repository.RefreshTokenRepository;
import company.backend.user.entity.User;
import company.backend.user.mapper.UserMapper;
import company.backend.user.repository.UserRepository;
import company.backend.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

/** Sign-in, sign-out, token rotation and the password reset foundation. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserRepository userRepository,
            UserService userService,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuditService auditService,
            LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.auditService = auditService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = resolveUser(request.username());

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailure(user, request.username());
            throw new BusinessException("Invalid username or password");
        }
        if (user.isLocked()) {
            throw new BusinessException("The account is temporarily locked after too many failed attempts");
        }
        if (!user.isActive()) {
            auditService.recordFailure(AuditAction.LOGIN_FAILED, "auth", "User", user.getId(), "Account inactive");
            throw new BusinessException("This account has been deactivated");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);
        auditService.record(AuditAction.LOGIN, "auth", "User", user.getId(), "Successful sign-in");

        return issueTokens(user);
    }

    /**
     * Issues a fresh access token and rotates the refresh token.
     *
     * <p>Rotation means a stolen refresh token is only usable once; presenting an
     * already-consumed token afterwards is treated as a compromise and revokes every
     * session belonging to that user.
     */
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(jwtService.hashToken(request.refreshToken()))
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), now);
            auditService.recordFailure(
                    AuditAction.LOGOUT, "auth", "User", stored.getUser().getId(), "Refresh token replay detected");
            throw new BusinessException("Invalid refresh token");
        }
        if (!stored.isUsable(now)) {
            throw new BusinessException("Refresh token is expired or has been revoked");
        }

        User user = stored.getUser();
        if (!user.isActive()) {
            throw new BusinessException("This account has been deactivated");
        }

        stored.revoke();
        refreshTokenRepository.save(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository
                .findByTokenHash(jwtService.hashToken(refreshToken))
                .ifPresent(stored -> {
                    stored.revoke();
                    refreshTokenRepository.save(stored);
                });
        auditService.record(AuditAction.LOGOUT, "auth", "User", SecurityUtils.getCurrentUserId(), "Signed out");
    }

    /**
     * Creates a password reset token.
     *
     * <p>Delivery is left to the deployment: the raw token is logged so a client
     * project can wire up e-mail, chat or SMS delivery without this template having
     * to depend on a mail provider. The endpoint always reports the same message so
     * it cannot be used to discover which addresses have accounts.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository
                .findByEmailIgnoreCase(request.email())
                .filter(User::isActive)
                .ifPresent(user -> {
                    Instant now = Instant.now();
                    passwordResetTokenRepository.invalidateAllForUser(user.getId(), now);

                    String rawToken = jwtService.generateOpaqueToken();
                    PasswordResetToken token = new PasswordResetToken();
                    token.setTokenHash(jwtService.hashToken(rawToken));
                    token.setUser(user);
                    token.setExpiresAt(now.plus(jwtProperties.getRefreshTokenExpiration()));
                    passwordResetTokenRepository.save(token);

                    auditService.record(
                            AuditAction.PASSWORD_RESET_REQUESTED,
                            "auth",
                            "User",
                            user.getId(),
                            "Password reset requested");
                    log.info("Password reset token issued for user {}: {}", user.getId(), rawToken);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(jwtService.hashToken(request.token()))
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        if (!token.isUsable(now)) {
            throw new BusinessException("Invalid or expired reset token");
        }
        User user = token.getUser();
        userService.applyNewPassword(user, request.newPassword());
        token.markUsed();
        passwordResetTokenRepository.save(token);
        // Existing sessions must not survive a credential change.
        refreshTokenRepository.revokeAllForUser(user.getId(), now);

        auditService.record(AuditAction.PASSWORD_RESET, "auth", "User", user.getId(), "Password reset completed");
    }

    private LoginResponse issueTokens(User user) {
        List<String> permissions = UserMapper.permissionsOf(List.copyOf(user.getRoles()));
        PlatformUserPrincipal principal = new PlatformUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.isActive(),
                permissions,
                user.getRoles().stream().map(company.backend.role.entity.Role::getCode).toList());

        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateOpaqueToken();

        RefreshToken stored = new RefreshToken();
        stored.setTokenHash(jwtService.hashToken(refreshToken));
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenExpiration()));
        stored.setUserAgent(currentUserAgent());
        refreshTokenRepository.save(stored);

        return LoginResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration(),
                UserMapper.toCurrentUser(user, permissions));
    }

    /**
     * Records a failed attempt.
     *
     * <p>Delegated to {@link LoginAttemptService} because a failed login throws,
     * which would roll back the counter stored in this transaction. Without a
     * separate committed transaction the lockout would never trigger.
     */
    private void registerFailure(User user, String identifier) {
        if (user == null) {
            auditService.recordFailure(AuditAction.LOGIN_FAILED, "auth", "User", null, "Unknown account: " + identifier);
            return;
        }
        loginAttemptService.recordFailure(user.getId());
        auditService.recordFailure(AuditAction.LOGIN_FAILED, "auth", "User", user.getId(), "Incorrect password");
    }

    /**
     * Finds the account behind a login identifier.
     *
     * <p>Accepts a numeric id, an email address or a username, in that order. The
     * username lookup is not optional: the field is labelled "username", and an
     * operator whose username differs from their email would otherwise be unable
     * to sign in at all.
     */
    private User resolveUser(String identifier) {
        String trimmed = identifier.trim();
        try {
            User byId = userRepository.findWithRolesById(Long.parseLong(trimmed)).orElse(null);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException notAnId) {
            // Expected for anything that is not a numeric id; fall through.
        }
        User byEmail = userRepository.findByEmailIgnoreCase(trimmed).orElse(null);
        if (byEmail != null) {
            return byEmail;
        }
        return userRepository.findByUsernameIgnoreCase(trimmed).orElse(null);
    }

    private String currentUserAgent() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            String agent = servlet.getRequest().getHeader("User-Agent");
            return agent == null ? null : agent.substring(0, Math.min(agent.length(), 300));
        }
        return null;
    }
}