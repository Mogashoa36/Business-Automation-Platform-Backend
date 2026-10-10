package company.backend.user.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import company.backend.security.Permissions;
import company.backend.user.dto.AdminResetPasswordRequest;
import company.backend.user.dto.CreateUserRequest;
import company.backend.user.dto.UpdateUserRequest;
import company.backend.user.dto.UserResponse;
import company.backend.user.dto.UserSummary;
import company.backend.user.entity.UserStatus;
import company.backend.user.service.UserService;

import jakarta.validation.Valid;

/**
 * User administration.
 *
 * <p>Every handler is guarded by a permission code. The matching Angular route and
 * control use the same code, so hiding a button and refusing the request can never
 * drift apart.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("username", "email", "firstName", "lastName", "status", "createdAt", "lastLoginAt");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_VIEW + "')")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageableFactory.create(page, size, sort, direction, SORTABLE_FIELDS, "username");
        return ApiResponse.ok(PageResponse.of(userService.search(search, status, departmentId, pageable)));
    }

    /** Bounded, lightweight list used by assignment dropdowns and approver pickers. */
    @GetMapping("/summaries")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserSummary>> summaries(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(userService.findSummaries(search, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_VIEW + "')")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.USER_CREATE + "')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.created("User created", userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + Permissions.USER_STATUS_CHANGE + "')")
    public ApiResponse<UserResponse> changeStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        return ApiResponse.ok("User status updated", userService.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_DELETE + "')")
    public ApiResponse<UserResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(
                "User deactivated. The account is retained so historical records keep a valid reference.",
                userService.deactivate(id));
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.USER_PASSWORD_RESET + "')")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.resetPassword(id, request);
    }
}