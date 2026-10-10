package company.backend.user.dto;

import java.time.Instant;
import java.util.List;

/**
 * Identity of the signed-in user.
 *
 * <p>The permission list drives both the Angular route guards and the visibility of
 * individual UI controls, so it is the single source of truth for what a user can
 * reach. Backed by the same permission codes enforced server side.
 */
public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String jobTitle,
        String avatarUrl,
        Long departmentId,
        String departmentName,
        List<String> roles,
        List<String> permissions,
        boolean mustChangePassword,
        Instant lastLoginAt) {
}