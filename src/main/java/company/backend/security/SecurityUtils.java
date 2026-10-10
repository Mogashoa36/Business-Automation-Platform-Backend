package company.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience access to the authenticated principal for services and controllers. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** @return the current user id, or {@code null} for anonymous / background execution */
    public static Long getCurrentUserId() {
        return principal().map(PlatformUserPrincipal::getId).orElse(null);
    }

    public static java.util.Optional<PlatformUserPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof PlatformUserPrincipal platformUser
                ? java.util.Optional.of(platformUser)
                : java.util.Optional.empty();
    }

    public static Long getCurrentUserIdOrThrow() {
        return getCurrentUserId();
    }
}