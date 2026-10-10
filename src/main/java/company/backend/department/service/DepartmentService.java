package company.backend.department.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.department.dto.DepartmentRequest;
import company.backend.department.dto.DepartmentResponse;
import company.backend.department.entity.Department;
import company.backend.department.repository.DepartmentRepository;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

/** Department structure and membership. Departments are plain data, never hard coded. */
@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> search(String search, Boolean active, Pageable pageable) {
        Specification<Department> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("code")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return departmentRepository.findAll(spec, pageable).map(department -> toResponse(department));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(department -> toResponse(department))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findActive() {
        return departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(department -> toResponse(department))
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return toResponse(requireDepartment(id));
    }

    @Audited(module = "department", action = AuditAction.CREATE, entityType = "Department")
    public DepartmentResponse create(DepartmentRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("A department with code '" + code + "' already exists");
        }
        Department department = new Department();
        apply(department, request, code);
        return toResponse(departmentRepository.save(department));
    }

    @Audited(module = "department", action = AuditAction.UPDATE, entityType = "Department")
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = requireDepartment(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException("A department with code '" + code + "' already exists");
        }
        apply(department, request, code);
        return toResponse(departmentRepository.save(department));
    }

    /**
     * Deactivates a department.
     *
     * <p>Deactivation is preferred over deletion so that historical assignments
     * keep a resolvable reference; a department that still has child departments
     * cannot be removed.
     */
    @Audited(module = "department", action = AuditAction.DELETE, entityType = "Department")
    public DepartmentResponse deactivate(Long id) {
        Department department = requireDepartment(id);
        if (hasChildren(id)) {
            throw new BusinessException("Move or remove the child departments first");
        }
        department.setActive(false);
        return toResponse(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public Department requireDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    private boolean hasChildren(Long id) {
        return departmentRepository.findAll().stream()
                .anyMatch(department ->
                        department.getParent() != null && department.getParent().getId().equals(id));
    }

    private void apply(Department department, DepartmentRequest request, String code) {
        department.setCode(code);
        department.setName(request.name().trim());
        department.setDescription(request.description());
        if (request.active() != null) {
            department.setActive(request.active());
        }
        department.setManagerId(validateManager(request.managerId()));
        if (request.parentId() != null) {
            if (request.parentId().equals(department.getId())) {
                throw new BusinessException("A department cannot be its own parent");
            }
            department.setParent(requireDepartment(request.parentId()));
        } else {
            department.setParent(null);
        }
    }

    private Long validateManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        if (!userRepository.existsById(managerId)) {
            throw new BusinessException("Manager " + managerId + " does not exist");
        }
        return managerId;
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.getParent() == null ? null : department.getParent().getId(),
                department.getParent() == null ? null : department.getParent().getName(),
                department.getManagerId(),
                managerName(department.getManagerId()),
                department.isActive(),
                department.getMembers() == null ? 0 : department.getMembers().size(),
                department.getCreatedAt(),
                department.getUpdatedAt());
    }

    private String managerName(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userRepository.findWithRolesById(managerId).map(user -> user.getFullName()).orElse(null);
    }
}