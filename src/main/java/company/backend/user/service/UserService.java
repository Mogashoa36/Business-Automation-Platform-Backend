package company.backend.user.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.audit.service.AuditService;
import company.backend.department.entity.Department;
import company.backend.department.repository.DepartmentRepository;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.role.entity.Role;
import company.backend.role.repository.RoleRepository;
import company.backend.security.SecurityUtils;
import company.backend.security.SystemRoles;
import company.backend.user.dto.AdminResetPasswordRequest;
import company.backend.user.dto.ChangePasswordRequest;
import company.backend.user.dto.CreateUserRequest;
import company.backend.user.dto.CurrentUserResponse;
import company.backend.user.dto.UpdateUserRequest;
import company.backend.user.dto.UserResponse;
import company.backend.user.dto.UserSummary;
import company.backend.user.entity.User;
import company.backend.user.entity.UserStatus;
import company.backend.user.mapper.UserMapper;
import company.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;

/** User lifecycle management. */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String search, UserStatus status, Long departmentId, Pageable pageable) {
        return userRepository
                .findAll(buildSpecification(search, status, departmentId), pageable)
                .map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserMapper.toResponse(requireUser(id));
    }

    /** Lightweight list for assignment pickers; bounded to keep the payload small. */
    @Transactional(readOnly = true)
    public List<UserSummary> findSummaries(String search, int limit) {
        Specification<User> spec =
                (root, query, builder) -> builder.and(
                        builder.equal(root.get("status"), UserStatus.ACTIVE),
                        matchesSearch(search, root, builder));
        int bounded = Math.min(Math.max(limit, 1), 200);
        return userRepository.findAll(spec, PageRequest.of(0, bounded)).getContent().stream()
                .map(UserMapper::toSummary)
                .toList();
    }

    @Audited(module = "user", action = AuditAction.CREATE, entityType = "User")
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        String username = request.username().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("A user with email '" + email + "' already exists");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("A user with username '" + username + "' already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setJobTitle(request.jobTitle());
        user.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        user.setDepartment(resolveDepartment(request.departmentId()));
        user.setRoles(resolveRoles(request.roleCodes()));
        user.setPasswordChangedAt(Instant.now());
        // An administrator choosing the initial password cannot be expected to keep
        // it secret, so the user is forced to set their own on first sign-in.
        user.setMustChangePassword(true);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Audited(module = "user", action = AuditAction.UPDATE, entityType = "User")
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = requireUser(id);
        UserResponse before = UserMapper.toResponse(user);

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().toLowerCase(Locale.ROOT);
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
                throw new BusinessException("A user with email '" + email + "' already exists");
            }
            user.setEmail(email);
        }
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setJobTitle(request.jobTitle());
        if (request.departmentId() != null) {
            user.setDepartment(resolveDepartment(request.departmentId()));
        }
        if (request.roleCodes() != null) {
            Set<Role> roles = resolveRoles(request.roleCodes());
            assertNotRemovingLastAdministrator(user, roles);
            user.setRoles(roles);
        }
        if (request.status() != null && request.status() != user.getStatus()) {
            assertCanChangeStatus(user, request.status());
            user.setStatus(request.status());
        }

        UserResponse saved = UserMapper.toResponse(userRepository.save(user));
        auditService.recordChange(
                AuditAction.UPDATE, "user", "User", id, before, saved, "User '" + saved.username() + "' updated");
        return saved;
    }

    @Audited(module = "user", action = AuditAction.STATUS_CHANGE, entityType = "User")
    public UserResponse changeStatus(Long id, UserStatus target) {
        User user = requireUser(id);
        assertCanChangeStatus(user, target);
        user.setStatus(target);
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Deactivates rather than destroys an account.
     *
     * <p>Users are referenced by tasks, approvals, comments and audit entries, so
     * erasing a row would silently rewrite history. Deactivation preserves the
     * record while removing every form of access.
     */
    @Audited(module = "user", action = AuditAction.DELETE, entityType = "User")
    public UserResponse deactivate(Long id) {
        User user = requireUser(id);
        assertCanChangeStatus(user, UserStatus.INACTIVE);
        user.setStatus(UserStatus.INACTIVE);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Audited(module = "user", action = AuditAction.PASSWORD_RESET, entityType = "User")
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        User user = requireUser(id);
        applyNewPassword(user, request.newPassword());
        user.setMustChangePassword(request.requiresPasswordChange());
        userRepository.save(user);
    }

    /** Self-service password change; verifies the current password first. */
    public void changeOwnPassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            auditService.recordFailure(
                    AuditAction.PASSWORD_CHANGED, "user", "User", userId, "Current password was incorrect");
            throw new BusinessException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }
        applyNewPassword(user, request.newPassword());
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("No authenticated user");
        }
        User user = requireUser(userId);
        return UserMapper.toCurrentUser(user, UserMapper.permissionsOf(List.copyOf(user.getRoles())));
    }

    @Transactional(readOnly = true)
    public User requireUser(Long id) {
        return userRepository.findWithRolesById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    /**
     * Applies a new credential to a user.
     *
     * <p>Exposed to the security module so that password reset and the admin reset
     * share one implementation of "set password, clear lockout".
     */
    public void applyNewPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    private static Specification<User> buildSpecification(String search, UserStatus status, Long departmentId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(matchesSearch(search, root, builder));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (departmentId != null) {
                predicates.add(builder.equal(root.get("department").get("id"), departmentId));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate matchesSearch(String search, Root<User> root, CriteriaBuilder builder) {
        if (search == null || search.isBlank()) {
            return builder.conjunction();
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return builder.or(
                builder.like(builder.lower(root.get("username")), pattern),
                builder.like(builder.lower(root.get("email")), pattern),
                builder.like(builder.lower(root.get("firstName")), pattern),
                builder.like(builder.lower(root.get("lastName")), pattern),
                builder.like(builder.lower(root.get("jobTitle")), pattern));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new BusinessException("Department " + departmentId + " does not exist"));
    }

    private Set<Role> resolveRoles(List<String> roleCodes) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleCodes == null) {
            return roles;
        }
        for (String code : roleCodes) {
            roles.add(roleRepository
                    .findByCode(code.toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new BusinessException("Role '" + code + "' does not exist")));
        }
        return roles;
    }

    /** Refuses to remove the last active administrator, which would lock the deployment out. */
    private void assertNotRemovingLastAdministrator(User user, Set<Role> newRoles) {
        if (!user.isActive() || !user.hasRole(SystemRoles.ADMIN)) {
            return;
        }
        boolean stillAdmin = newRoles.stream().anyMatch(role -> SystemRoles.ADMIN.equals(role.getCode()));
        if (!stillAdmin && userRepository.countActiveByRoleCode(SystemRoles.ADMIN) <= 1) {
            throw new BusinessException("At least one active administrator must remain");
        }
    }

    private void assertCanChangeStatus(User user, UserStatus target) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (user.getId().equals(currentUserId) && target == UserStatus.INACTIVE) {
            throw new BusinessException("You cannot deactivate your own account");
        }
        if (user.hasRole(SystemRoles.ADMIN)
                && target == UserStatus.INACTIVE
                && userRepository.countActiveByRoleCode(SystemRoles.ADMIN) <= 1) {
            throw new BusinessException("At least one active administrator must remain");
        }
    }
}