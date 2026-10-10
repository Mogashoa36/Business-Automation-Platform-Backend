package company.backend.role.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.audit.service.AuditService;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.role.dto.PermissionResponse;
import company.backend.role.dto.RoleRequest;
import company.backend.role.dto.RoleResponse;
import company.backend.role.entity.Permission;
import company.backend.role.entity.Role;
import company.backend.role.repository.PermissionRepository;
import company.backend.role.repository.RoleRepository;

/**
 * Role management.
 *
 * <p>Roles are data, not code: a client project can add, rename or extend them
 * without touching this class. The only protected concept is the system flag, which
 * stops a deployment from deleting the roles its own admin account depends on.
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public RoleService(
            RoleRepository roleRepository, PermissionRepository permissionRepository, AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(RoleService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return roleRepository
                .findWithPermissionsById(id)
                .map(RoleService::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));
    }

    @Audited(module = "role", action = AuditAction.CREATE, entityType = "Role")
    public RoleResponse create(RoleRequest request) {
        String code = request.code().toUpperCase();
        if (roleRepository.existsByCode(code)) {
            throw new BusinessException("A role with code '" + code + "' already exists");
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(request.name());
        role.setDescription(request.description());
        role.setActive(request.active() == null || request.active());
        role.setSystemRole(false);
        role.setPermissions(resolvePermissions(request.permissionIds()));

        return toResponse(roleRepository.save(role));
    }

    @Audited(module = "role", action = AuditAction.UPDATE, entityType = "Role")
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));

        RoleResponse before = toResponse(role);

        String code = request.code().toUpperCase();
        if (!code.equals(role.getCode()) && roleRepository.existsByCode(code)) {
            throw new BusinessException("A role with code '" + code + "' already exists");
        }
        role.setCode(code);
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.active() != null) {
            role.setActive(request.active());
        }
        role.setPermissions(resolvePermissions(request.permissionIds()));

        RoleResponse saved = toResponse(roleRepository.save(role));
        auditService.recordChange(
                AuditAction.UPDATE, "role", "Role", id, before, saved, "Role '" + saved.code() + "' updated");
        return saved;
    }

    /**
     * Removes a custom role.
     *
     * <p>Built-in roles are refused rather than cascaded, because deleting the admin
     * role would leave the deployment with no way to manage itself.
     */
    @Audited(module = "role", action = AuditAction.DELETE, entityType = "Role")
    public void delete(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Role", id));
        if (role.isSystemRole()) {
            throw new BusinessException("Built-in role '" + role.getCode() + "' cannot be deleted");
        }
        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != new LinkedHashSet<>(permissionIds).size()) {
            throw new BusinessException("One or more permissions do not exist");
        }
        return new LinkedHashSet<>(permissions);
    }

    static RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                role.isActive(),
                role.getPermissions().stream().map(RoleService::toPermissionResponse).toList(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }

    static PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.getModule());
    }
}