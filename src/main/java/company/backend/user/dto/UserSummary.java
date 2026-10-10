package company.backend.user.dto;

/** Compact reference used by pickers, task assignment dropdowns and approver lists. */
public record UserSummary(Long id, String username, String fullName, String email, String jobTitle, String avatarUrl) {
}