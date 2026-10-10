package company.backend.role.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.role.dto.PermissionResponse;
import company.backend.role.entity.Permission;
import company.backend.role.entity.Role;
import company.backend.role.repository.PermissionRepository;
import company.backend.role.repository.RoleRepository;

/**
 * Permission catalogue.
 *
 * <p>The platform ships a fixed set of codes that the base modules enforce, but
 * client projects are expected to add their own rows here - for example
 * {@code INVOICE_APPROVE} - without any code change.
 */
@Service
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAllByOrderByModuleAscNameAsc().stream()
                .map(RoleService::toPermissionResponse)
                .toList();
    }

    /** Permissions grouped by owning module, which is how the UI renders the picker. */
    @Transactional(readOnly = true)
    public Map<String, List<PermissionResponse>> findGroupedByModule() {
        Map<String, List<PermissionResponse>> grouped = new LinkedHashMap<>();
        for (PermissionResponse permission : findAll()) {
            grouped.computeIfAbsent(permission.module(), key -> new java.util.ArrayList<>())
                    .add(permission);
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public List<String> findCodesIn(Set<String> codes) {
        return permissionRepository.findByCodeIn(List.copyOf(codes)).stream()
                .map(Permission::getCode)
                .toList();
    }

    @Audited(module = "permission", action = AuditAction.CREATE, entityType = "Permission")
    public PermissionResponse create(String code, String name, String description, String module) {
        String normalised = code == null ? null : code.trim().toUpperCase();
        if (normalised == null || normalised.isBlank()) {
            throw new BusinessException("Permission code is required");
        }
        if (permissionRepository.existsByCode(normalised)) {
            throw new BusinessException("A permission with code '" + normalised + "' already exists");
        }
        Permission permission = new Permission();
        permission.setCode(normalised);
        permission.setName(name == null || name.isBlank() ? normalised : name);
        permission.setDescription(description);
        permission.setModule(module == null || module.isBlank() ? "custom" : module);
        return RoleService.toPermissionResponse(permissionRepository.save(permission));
    }

    @Audited(module = "permission", action = AuditAction.DELETE, entityType = "Permission")
    public void delete(Long id) {
        Permission permission = permissionRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id));
        if (roleRepository.countBySystemRoleTrue() > 0 && isGrantedToSystemRole(id)) {
            throw new BusinessException(
                    "Permission '" + permission.getCode() + "' is used by a built-in role and cannot be removed");
        }
        permissionRepository.delete(permission);
    }

    private boolean isGrantedToSystemRole(Long permissionId) {
        return roleRepository.findAll().stream()
                .filter(Role::isSystemRole)
                .anyMatch(role -> role.getPermissions().stream()
                        .anyMatch(permission -> permission.getId().equals(permissionId)));
    }
}