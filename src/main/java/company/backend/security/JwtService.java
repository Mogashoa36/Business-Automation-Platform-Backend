package company.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import company.backend.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates JSON Web Tokens.
 *
 * <p>Access tokens are self-contained and short lived. Refresh tokens are opaque
 * random strings that are persisted (hashed) so they can be revoked - this is what
 * makes "log out everywhere" possible without server side sessions.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String TYPE_ACCESS = "access";

    /** HS256 requires a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private static final java.util.regex.Pattern BASE64_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9+/]{4,}={0,2}$");

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(decodeSecret(properties.getSecret()));
    }

    /**
     * Accepts either a Base64 encoded secret or a raw passphrase.
     *
     * <p>The string is tested against the Base64 alphabet before decoding rather
     * than catching the decoder's exception, because that exception is a
     * {@code JwtException} and not an {@code IllegalArgumentException} - a subtle
     * mismatch that would otherwise let a short, malformed key through.
     *
     * <p>Failing fast is deliberate: a silently weak default is far more dangerous
     * than a startup error.
     */
    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "platform.security.jwt.secret is not configured. Set the "
                            + "PLATFORM_SECURITY_JWT_SECRET environment variable.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (BASE64_PATTERN.matcher(secret).matches()) {
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (RuntimeException notActuallyBase64) {
                // Fall through and use the raw bytes.
            }
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "platform.security.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256, but was " + keyBytes.length + " bytes.");
        }
        return keyBytes;
    }

    public String generateAccessToken(PlatformUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenExpiration());
        return Jwts.builder()
                .subject(principal.getUsername())
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, principal.getEmail())
                .claim(CLAIM_NAME, principal.getFullName())
                .signWith(signingKey)
                .compact();
    }

    public Instant getAccessTokenExpiration() {
        return Instant.now().plus(properties.getAccessTokenExpiration());
    }

    /**
     * Validates the signature, issuer and expiry of an access token.
     *
     * @return the parsed claims, or {@code null} when the token is invalid for any reason
     */
    public Claims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class)) ? claims : null;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected token: {}", ex.getMessage());
            return null;
        }
    }

    /** @return the user id encoded in the token, or {@code null} if it cannot be trusted */
    public Long extractUserId(String token) {
        Claims claims = parseAccessToken(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Cryptographically random, URL safe token used for refresh and password reset. */
    public String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex digest; only this is persisted, never the token itself. */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }
}