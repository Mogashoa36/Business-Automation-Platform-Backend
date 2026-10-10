package company.backend.dashboard.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.dashboard.dto.DashboardResponse;
import company.backend.dashboard.service.DashboardService;
import company.backend.security.Permissions;

/** Landing page aggregation. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.DASHBOARD_VIEW + "')")
    public ApiResponse<DashboardResponse> build() {
        return ApiResponse.ok(dashboardService.build());
    }
}