package company.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings.
 *
 * <p>{@code secret} must be supplied through the {@code PLATFORM_SECURITY_JWT_SECRET}
 * environment variable. A development-only default lives in
 * {@code application-dev.properties} so that production can never silently fall
 * back to a well-known key.
 */
@ConfigurationProperties(prefix = "platform.security.jwt")
public class JwtProperties {

    /** Base64 encoded secret; must be at least 256 bits for HS256. */
    private String secret;

    private Duration accessTokenExpiration = Duration.ofMinutes(30);

    private Duration refreshTokenExpiration = Duration.ofDays(7);

    private String issuer = "business-automation-platform";

    /** Allows a client to invalidate a refresh token before it expires. */
    private boolean tokenRevocationEnabled = true;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(Duration accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(Duration refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isTokenRevocationEnabled() {
        return tokenRevocationEnabled;
    }

    public void setTokenRevocationEnabled(boolean tokenRevocationEnabled) {
        this.tokenRevocationEnabled = tokenRevocationEnabled;
    }
}