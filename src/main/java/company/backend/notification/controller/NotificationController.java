package company.backend.notification.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.common.PageResponse;
import company.backend.common.PageableFactory;
import company.backend.notification.dto.NotificationPreferenceResponse;
import company.backend.notification.dto.NotificationResponse;
import company.backend.notification.service.NotificationService;
import company.backend.security.Permissions;

/** The signed-in user's own notifications. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> findAll(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageableFactory.create(page, size, "createdAt", "desc", Set.of("createdAt"), "createdAt");
        return ApiResponse.ok(PageResponse.of(notificationService.findForCurrentUser(unreadOnly, pageable)));
    }

    @GetMapping("/recent")
    public ApiResponse<List<NotificationResponse>> findRecent() {
        return ApiResponse.ok(notificationService.findRecentUnread());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(notificationService.countUnread());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        return ApiResponse.ok("Notification marked as read", notificationService.markRead(id));
    }

    @PostMapping("/read-all")
    public ApiResponse<Long> markAllRead() {
        return ApiResponse.ok("All notifications marked as read", notificationService.markAllRead());
    }

    @GetMapping("/preferences")
    public ApiResponse<List<NotificationPreferenceResponse>> findPreferences() {
        return ApiResponse.ok(notificationService.findPreferences());
    }

    @PutMapping("/preferences")
    public ApiResponse<NotificationPreferenceResponse> updatePreference(
            @RequestBody NotificationPreferenceRequest request) {
        return ApiResponse.ok(
                "Notification preferences updated",
                notificationService.updatePreference(
                        request.type(), request.inApp(), request.email(), request.sms(), request.push()));
    }

    /** Payload for toggling a single notification type's delivery channels. */
    public record NotificationPreferenceRequest(
            @jakarta.validation.constraints.NotBlank String type,
            boolean inApp,
            boolean email,
            boolean sms,
            boolean push) {
    }
}