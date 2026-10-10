package company.backend.department.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.common.PageResponse;
import company.backend.common.PageableFactory;
import company.backend.department.dto.DepartmentRequest;
import company.backend.department.dto.DepartmentResponse;
import company.backend.department.service.DepartmentService;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/** Department administration. */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "code", "createdAt", "active");

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.DEPARTMENT_VIEW + "')")
    public ApiResponse<PageResponse<DepartmentResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageableFactory.create(page, size, sort, direction, SORTABLE_FIELDS, "name");
        return ApiResponse.ok(PageResponse.of(departmentService.search(search, active, pageable)));
    }

    /** Full list without pagination, for selects and tree building. */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('" + Permissions.DEPARTMENT_VIEW + "')")
    public ApiResponse<List<DepartmentResponse>> findAll() {
        return ApiResponse.ok(departmentService.findAll());
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DepartmentResponse>> findActive() {
        return ApiResponse.ok(departmentService.findActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.DEPARTMENT_VIEW + "')")
    public ApiResponse<DepartmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(departmentService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.DEPARTMENT_CREATE + "')")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.created("Department created", departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.DEPARTMENT_UPDATE + "')")
    public ApiResponse<DepartmentResponse> update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok("Department updated", departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.DEPARTMENT_DELETE + "')")
    public ApiResponse<DepartmentResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok("Department deactivated", departmentService.deactivate(id));
    }
}