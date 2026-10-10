package company.backend.role.dto;

/** Read model for a permission. */
public record PermissionResponse(
        Long id, String code, String name, String description, String module) {
}