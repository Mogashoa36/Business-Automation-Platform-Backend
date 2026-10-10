package company.backend.organization.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.organization.dto.OrganizationRequest;
import company.backend.organization.dto.OrganizationResponse;
import company.backend.organization.service.OrganizationService;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/** Company profile. Readable by anyone signed in, editable by administrators. */
@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OrganizationResponse> get() {
        return ApiResponse.ok(organizationService.get());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + Permissions.ORGANIZATION_MANAGE + "')")
    public ApiResponse<OrganizationResponse> save(@Valid @RequestBody OrganizationRequest request) {
        return ApiResponse.ok("Company profile updated", organizationService.save(request));
    }
}