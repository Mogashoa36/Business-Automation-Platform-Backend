package company.backend.department.dto;

import java.time.Instant;

/** Read model for a department. */
public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String description,
        Long parentId,
        String parentName,
        Long managerId,
        String managerName,
        boolean active,
        long memberCount,
        Instant createdAt,
        Instant updatedAt) {
}