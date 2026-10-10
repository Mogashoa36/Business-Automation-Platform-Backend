package company.backend.user.mapper;

import java.util.Comparator;
import java.util.List;

import company.backend.role.entity.Permission;
import company.backend.role.entity.Role;
import company.backend.user.dto.CurrentUserResponse;
import company.backend.user.dto.UserResponse;
import company.backend.user.dto.UserSummary;
import company.backend.user.entity.User;

/**
 * Entity to DTO conversion.
 *
 * <p>Hand written on purpose: the mappings are few, stable and security relevant,
 * which makes an explicit whitelist noticeably safer than a reflective mapper that
 * could one day copy the password hash across by accident.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        List<Role> roles = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getCode))
                .toList();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getPhone(),
                user.getJobTitle(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                roles.stream().map(Role::getCode).toList(),
                permissionsOf(roles),
                user.isMustChangePassword(),
                user.isLocked(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static CurrentUserResponse toCurrentUser(User user, List<String> permissions) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getJobTitle(),
                user.getAvatarUrl(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                permissions,
                user.isMustChangePassword(),
                user.getLastLoginAt());
    }

    public static UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getJobTitle(),
                user.getAvatarUrl());
    }

    /** Flat, de-duplicated permission codes granted through the user's roles. */
    public static List<String> permissionsOf(List<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
    }
}