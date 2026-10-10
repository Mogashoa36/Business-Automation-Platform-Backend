package company.backend.user.dto;

import java.time.Instant;
import java.util.List;

import company.backend.user.entity.UserStatus;

/**
 * Full user representation returned to administrators.
 *
 * <p>There is intentionally no password field of any kind on this record.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String jobTitle,
        String avatarUrl,
        UserStatus status,
        Long departmentId,
        String departmentName,
        List<String> roles,
        List<String> permissions,
        boolean mustChangePassword,
        boolean locked,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt) {
}