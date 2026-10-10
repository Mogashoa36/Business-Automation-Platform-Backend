package company.backend.approval.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.approval.dto.ApprovalActionResponse;
import company.backend.approval.dto.ApprovalDecisionRequest;
import company.backend.approval.dto.ApprovalResponse;
import company.backend.approval.dto.CreateApprovalRequest;
import company.backend.approval.entity.ApprovalStatus;
import company.backend.approval.service.ApprovalService;
import company.backend.common.ApiResponse;
import company.backend.common.PageResponse;
import company.backend.common.PageableFactory;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/** Approval requests and their decision history. */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "reference", "status", "createdAt", "amount");

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_VIEW + "')")
    public ApiResponse<PageResponse<ApprovalResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageableFactory.create(page, size, sort, direction, SORTABLE_FIELDS, "createdAt");
        return ApiResponse.ok(PageResponse.of(
                approvalService.search(search, status, category, requesterId, pageable)));
    }

    /** The caller's personal decision queue. */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_VIEW + "')")
    public ApiResponse<List<ApprovalResponse>> findPending() {
        return ApiResponse.ok(approvalService.findPendingForCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_VIEW + "')")
    public ApiResponse<ApprovalResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(approvalService.findById(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_VIEW + "')")
    public ApiResponse<List<ApprovalActionResponse>> findHistory(@PathVariable Long id) {
        return ApiResponse.ok(approvalService.findHistory(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_REQUEST + "')")
    public ApiResponse<ApprovalResponse> create(@Valid @RequestBody CreateApprovalRequest request) {
        return ApiResponse.created("Approval requested", approvalService.create(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_REQUEST + "')")
    public ApiResponse<ApprovalResponse> submit(@PathVariable Long id) {
        return ApiResponse.ok("Approval submitted", approvalService.submit(id));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_DECIDE + "')")
    public ApiResponse<ApprovalResponse> decide(
            @PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        String message = request.approved() ? "Approval granted" : "Approval rejected";
        return ApiResponse.ok(message, approvalService.decide(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_REQUEST + "')")
    public ApiResponse<ApprovalResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Approval cancelled", approvalService.cancel(id));
    }
}