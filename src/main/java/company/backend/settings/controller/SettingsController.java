package company.backend.settings.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.security.Permissions;
import company.backend.settings.entity.SystemSetting;
import company.backend.settings.service.SettingsService;

/** System settings. Readable by anyone signed in, writable by administrators. */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** Branding and runtime info; needed by the shell before any route resolves. */
    @GetMapping("/platform")
    public ApiResponse<Map<String, Object>> platformInfo() {
        return ApiResponse.ok(settingsService.platformInfo());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_VIEW + "')")
    public ApiResponse<List<SettingResponse>> findAll(
            @RequestParam(required = false) String group) {

        List<SystemSetting> settings =
                group == null || group.isBlank() ? settingsService.findAll() : settingsService.findByGroup(group);
        return ApiResponse.ok(settings.stream().map(SettingsController::toResponse).toList());
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_VIEW + "')")
    public ApiResponse<SettingResponse> findByKey(@PathVariable String key) {
        return ApiResponse.ok(toResponse(settingsService.findByKey(key)));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    public ApiResponse<SettingResponse> update(@PathVariable String key, @RequestBody UpdateSettingRequest request) {
        return ApiResponse.ok("Setting updated", toResponse(settingsService.update(key, request.value())));
    }

    private static SettingResponse toResponse(SystemSetting setting) {
        return new SettingResponse(
                setting.getKey(),
                setting.getValue(),
                setting.getValueType(),
                setting.getGroup(),
                setting.getDescription(),
                setting.isEditable(),
                setting.getUpdatedAt());
    }

    /** Payload for updating a single setting value. */
    public record UpdateSettingRequest(String value) {
    }

    /** Read model for a setting. */
    public record SettingResponse(
            String key,
            String value,
            String valueType,
            String group,
            String description,
            boolean editable,
            Instant updatedAt) {
    }
}