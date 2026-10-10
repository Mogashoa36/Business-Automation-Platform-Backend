package company.backend.role.dto;

import java.time.Instant;
import java.util.List;

/** Read model for a role together with its granted permissions. */
public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean systemRole,
        boolean active,
        List<PermissionResponse> permissions,
        Instant createdAt,
        Instant updatedAt) {
}