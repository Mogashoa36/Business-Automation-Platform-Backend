package company.backend.workflow.controller;

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
import company.backend.security.Permissions;
import company.backend.workflow.dto.StartWorkflowRequest;
import company.backend.workflow.dto.WorkflowDecisionRequest;
import company.backend.workflow.dto.WorkflowDefinitionRequest;
import company.backend.workflow.dto.WorkflowDefinitionResponse;
import company.backend.workflow.dto.WorkflowInstanceResponse;
import company.backend.workflow.entity.WorkflowStatus;
import company.backend.workflow.service.WorkflowService;

import jakarta.validation.Valid;

/**
 * Workflow templates and instances.
 *
 * <p>Definitions and instances live under separate paths because they are genuinely
 * different resources with different lifecycles and permissions.
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "reference", "status", "createdAt", "submittedAt");

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // ----------------------------------------------------------------- definitions

    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_VIEW + "')")
    public ApiResponse<List<WorkflowDefinitionResponse>> findDefinitions() {
        return ApiResponse.ok(workflowService.findAllDefinitions());
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_VIEW + "')")
    public ApiResponse<WorkflowDefinitionResponse> findDefinition(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.findDefinition(id));
    }

    @PostMapping("/definitions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_MANAGE_DEFINITION + "')")
    public ApiResponse<WorkflowDefinitionResponse> createDefinition(
            @Valid @RequestBody WorkflowDefinitionRequest request) {
        return ApiResponse.created("Workflow created", workflowService.createDefinition(request));
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_MANAGE_DEFINITION + "')")
    public ApiResponse<WorkflowDefinitionResponse> updateDefinition(
            @PathVariable Long id, @Valid @RequestBody WorkflowDefinitionRequest request) {
        return ApiResponse.ok("Workflow updated", workflowService.updateDefinition(id, request));
    }

    @DeleteMapping("/definitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_MANAGE_DEFINITION + "')")
    public void deleteDefinition(@PathVariable Long id) {
        workflowService.deleteDefinition(id);
    }

    // ------------------------------------------------------------------ instances

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_VIEW + "')")
    public ApiResponse<PageResponse<WorkflowInstanceResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) Long definitionId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false, defaultValue = "false") boolean mineOnly,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageableFactory.create(page, size, sort, direction, SORTABLE_FIELDS, "createdAt");
        return ApiResponse.ok(PageResponse.of(
                workflowService.search(search, status, definitionId, assignedToId, mineOnly, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_VIEW + "')")
    public ApiResponse<WorkflowInstanceResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.findById(id));
    }

    @PostMapping("/instances")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_INSTANTIATE + "')")
    public ApiResponse<WorkflowInstanceResponse> start(@Valid @RequestBody StartWorkflowRequest request) {
        return ApiResponse.created("Workflow started", workflowService.start(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_INSTANTIATE + "')")
    public ApiResponse<WorkflowInstanceResponse> submit(@PathVariable Long id) {
        return ApiResponse.ok("Workflow submitted", workflowService.submit(id));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('" + Permissions.APPROVAL_DECIDE + "')")
    public ApiResponse<WorkflowInstanceResponse> decide(
            @PathVariable Long id, @Valid @RequestBody WorkflowDecisionRequest request) {
        String message = request.approved() ? "Step approved" : "Step rejected";
        return ApiResponse.ok(message, workflowService.decide(id, request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_INSTANTIATE + "')")
    public ApiResponse<WorkflowInstanceResponse> complete(@PathVariable Long id) {
        return ApiResponse.ok("Workflow completed", workflowService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_CANCEL + "')")
    public ApiResponse<WorkflowInstanceResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Workflow cancelled", workflowService.cancel(id));
    }
}