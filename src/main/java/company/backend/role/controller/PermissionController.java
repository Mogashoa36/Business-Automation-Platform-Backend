package company.backend.role.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.role.dto.PermissionRequest;
import company.backend.role.dto.PermissionResponse;
import company.backend.role.service.PermissionService;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/**
 * Permission catalogue.
 *
 * <p>Separate from {@link RoleController} because the two have different lifecycles:
 * permissions are referenced by code from {@code @PreAuthorize} expressions, while
 * roles are edited far more often.
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_VIEW + "')")
    public ApiResponse<List<PermissionResponse>> findAll() {
        return ApiResponse.ok(permissionService.findAll());
    }

    /** Grouped by module, which is the shape the role editor renders. */
    @GetMapping("/grouped")
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_VIEW + "')")
    public ApiResponse<Map<String, List<PermissionResponse>>> findGrouped() {
        return ApiResponse.ok(permissionService.findGroupedByModule());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_MANAGE + "')")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return ApiResponse.created(
                "Permission created",
                permissionService.create(
                        request.code(), request.name(), request.description(), request.module()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_MANAGE + "')")
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
    }
}