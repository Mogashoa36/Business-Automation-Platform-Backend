package company.backend.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.security.Permissions;
import company.backend.user.dto.ChangePasswordRequest;
import company.backend.user.dto.CurrentUserResponse;
import company.backend.user.service.UserService;

import jakarta.validation.Valid;

/**
 * Self-service endpoints for the signed-in user.
 *
 * <p>Separated from {@link UserController} so that the administration permission
 * codes are never required for ordinary profile work.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.DASHBOARD_VIEW + "') or isAuthenticated()")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(userService.getCurrentUser());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(userService.getCurrentUser().id(), request);
    }
}