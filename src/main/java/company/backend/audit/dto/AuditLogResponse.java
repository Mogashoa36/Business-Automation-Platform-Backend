package company.backend.audit.dto;

import java.time.Instant;

/** Read model for an audit entry. */
public record AuditLogResponse(
        Long id,
        Long userId,
        String username,
        String action,
        String module,
        String entityType,
        String entityId,
        String description,
        String ipAddress,
        boolean success,
        Instant createdAt) {
}