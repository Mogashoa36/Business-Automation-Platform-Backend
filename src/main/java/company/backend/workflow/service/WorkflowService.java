package company.backend.workflow.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.audit.service.AuditService;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.notification.entity.NotificationType;
import company.backend.notification.service.NotificationService;
import company.backend.security.Permissions;
import company.backend.role.entity.Role;
import company.backend.role.repository.RoleRepository;
import company.backend.security.SecurityUtils;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;
import company.backend.workflow.dto.StartWorkflowRequest;
import company.backend.workflow.dto.WorkflowDecisionRequest;
import company.backend.workflow.dto.WorkflowDefinitionRequest;
import company.backend.workflow.dto.WorkflowDefinitionResponse;
import company.backend.workflow.dto.WorkflowInstanceResponse;
import company.backend.workflow.entity.WorkflowDefinition;
import company.backend.workflow.entity.WorkflowInstance;
import company.backend.workflow.entity.WorkflowInstanceStep;
import company.backend.workflow.entity.WorkflowStatus;
import company.backend.workflow.entity.WorkflowStepDefinition;
import company.backend.workflow.entity.WorkflowStepStatus;
import company.backend.workflow.entity.WorkflowStepType;
import company.backend.workflow.repository.WorkflowDefinitionRepository;
import company.backend.workflow.repository.WorkflowInstanceRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

/**
 * Generic workflow engine.
 *
 * <p>The engine knows about ordered steps and decisions - nothing about the
 * business. A client project defines its process by creating a template through
 * the API or the UI, which is what keeps this class free of industry concepts.
 *
 * <p>Steps are resolved against a <em>role</em>, not a person, so replacing the
 * person who holds a role never invalidates a running process.
 */
@Service
@Transactional
public class WorkflowService {

    private static final String REFERENCE_PREFIX = "WF-";

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public WorkflowService(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowInstanceRepository instanceRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService) {
        this.definitionRepository = definitionRepository;
        this.instanceRepository = instanceRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- definitions

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> findAllDefinitions() {
        return definitionRepository.findAllByOrderByNameAsc().stream()
                .map(WorkflowService::toDefinitionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse findDefinition(Long id) {
        return toDefinitionResponse(requireDefinition(id));
    }

    @Audited(module = "workflow", action = AuditAction.CREATE, entityType = "WorkflowDefinition")
    public WorkflowDefinitionResponse createDefinition(WorkflowDefinitionRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (definitionRepository.existsByCode(code)) {
            throw new BusinessException("A workflow with code '" + code + "' already exists");
        }
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setCode(code);
        definition.setName(request.name().trim());
        definition.setDescription(request.description());
        definition.setActive(request.active() == null || request.active());
        applySteps(definition, request);
        return toDefinitionResponse(definitionRepository.save(definition));
    }

    @Audited(module = "workflow", action = AuditAction.UPDATE, entityType = "WorkflowDefinition")
    public WorkflowDefinitionResponse updateDefinition(Long id, WorkflowDefinitionRequest request) {
        WorkflowDefinition definition = requireDefinition(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.equals(definition.getCode()) && definitionRepository.existsByCode(code)) {
            throw new BusinessException("A workflow with code '" + code + "' already exists");
        }
        assertNoRunningInstances(id);

        definition.setCode(code);
        definition.setName(request.name().trim());
        definition.setDescription(request.description());
        if (request.active() != null) {
            definition.setActive(request.active());
        }
        // A version bump makes the change visible to everyone auditing a running instance.
        definition.setVersion(definition.getVersion() + 1);
        applySteps(definition, request);
        return toDefinitionResponse(definitionRepository.save(definition));
    }

    @Audited(module = "workflow", action = AuditAction.DELETE, entityType = "WorkflowDefinition")
    public void deleteDefinition(Long id) {
        WorkflowDefinition definition = requireDefinition(id);
        assertNoRunningInstances(id);
        definitionRepository.delete(definition);
    }

    // ------------------------------------------------------------------ instances

    /** Starts a run of a template, copying its steps so history is immutable. */
    @Audited(module = "workflow", action = AuditAction.CREATE, entityType = "WorkflowInstance")
    public WorkflowInstanceResponse start(StartWorkflowRequest request) {
        WorkflowDefinition definition = requireDefinition(request.definitionId());
        if (!definition.isActive()) {
            throw new BusinessException("Workflow '" + definition.getCode() + "' is not active");
        }
        if (definition.getSteps().isEmpty()) {
            throw new BusinessException("Workflow '" + definition.getCode() + "' has no steps configured");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setDefinition(definition);
        instance.setDefinitionVersion(definition.getVersion());
        instance.setTitle(request.title().trim());
        instance.setDescription(request.description());
        instance.setPayload(request.payload());
        instance.setRequester(currentUser());
        instance.setStatus(WorkflowStatus.DRAFT);

        for (WorkflowStepDefinition template : definition.getSteps()) {
            WorkflowInstanceStep step = new WorkflowInstanceStep();
            step.setStepName(template.getName());
            step.setStepType(template.getStepType());
            step.setStepOrder(template.getStepOrder());
            step.setAssigneeRole(template.getAssigneeRole());
            step.setStatus(WorkflowStepStatus.PENDING);
            instance.addStep(step);
        }

        WorkflowInstance saved = instanceRepository.save(instance);
        saved.setReference(REFERENCE_PREFIX + String.format("%06d", saved.getId()));
        saved = instanceRepository.save(saved);

        if (request.shouldSubmit()) {
            submit(saved.getId());
        }
        return findById(saved.getId());
    }

    @Audited(module = "workflow", action = AuditAction.SUBMIT, entityType = "WorkflowInstance")
    public WorkflowInstanceResponse submit(Long instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (!instance.getStatus().isEditable()) {
            throw new BusinessException("Only a draft or rejected workflow can be submitted");
        }
        assertOwnedByRequester(instance);

        instance.setStatus(WorkflowStatus.SUBMITTED);
        instance.setSubmittedAt(Instant.now());
        instanceRepository.save(instance);
        advance(instance);
        return findById(instanceId);
    }

    /**
     * Records a decision on the step that is currently active.
     *
     * <p>Approving moves the process to the next step, or closes it when this was
     * the last one. Rejecting ends it immediately - there is no partial approval
     * state, which keeps the meaning of {@code REJECTED} unambiguous.
     */
    @Audited(module = "workflow", action = AuditAction.UPDATE, entityType = "WorkflowInstance")
    public WorkflowInstanceResponse decide(Long instanceId, WorkflowDecisionRequest request) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (instance.getStatus().isTerminal()) {
            throw new BusinessException("This workflow has already finished");
        }
        if (instance.getStatus() != WorkflowStatus.UNDER_REVIEW) {
            throw new BusinessException("This workflow is not awaiting a decision");
        }

        WorkflowInstanceStep step = currentStep(instance);
        if (step == null) {
            throw new BusinessException("No active step was found for this workflow");
        }
        assertCanDecide(instance, step);

        Instant now = Instant.now();
        step.setDecisionComment(request.comment());
        step.setCompletedAt(now);
        step.setStatus(request.approved() ? WorkflowStepStatus.APPROVED : WorkflowStepStatus.REJECTED);
        instanceRepository.save(instance);

        if (!request.approved()) {
            instance.setStatus(WorkflowStatus.REJECTED);
            instance.setCompletedAt(now);
            instance.setCurrentAssignee(null);
            instance.setCurrentStepIndex(-1);
            instanceRepository.save(instance);
            notifyRequester(instance, "Workflow rejected", instance.getReference() + " was rejected at '"
                    + step.getStepName() + "'.");
            return findById(instanceId);
        }

        advance(instance);
        return findById(instanceId);
    }

    /** Closes an approved workflow, or asks a requester to withdraw a draft. */
    @Audited(module = "workflow", action = AuditAction.UPDATE, entityType = "WorkflowInstance")
    public WorkflowInstanceResponse complete(Long instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (instance.getStatus() != WorkflowStatus.APPROVED) {
            throw new BusinessException("Only an approved workflow can be completed");
        }
        instance.setStatus(WorkflowStatus.COMPLETED);
        instance.setCompletedAt(Instant.now());
        return toInstanceResponse(instanceRepository.save(instance));
    }

    @Audited(module = "workflow", action = AuditAction.CANCEL, entityType = "WorkflowInstance")
    public WorkflowInstanceResponse cancel(Long instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (instance.getStatus().isTerminal()) {
            throw new BusinessException("This workflow has already finished");
        }
        assertOwnedByRequester(instance);

        instance.getSteps().stream()
                .filter(WorkflowInstanceStep::isPending)
                .forEach(step -> step.setStatus(WorkflowStepStatus.SKIPPED));
        instance.setStatus(WorkflowStatus.CANCELLED);
        instance.setCompletedAt(Instant.now());
        instance.setCurrentAssignee(null);
        instance.setCurrentStepIndex(-1);
        return toInstanceResponse(instanceRepository.save(instance));
    }

    // -------------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public Page<WorkflowInstanceResponse> search(
            String search, WorkflowStatus status, Long definitionId, Long assignedToId, boolean mineOnly,
            Pageable pageable) {

        Specification<WorkflowInstance> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("reference")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (definitionId != null) {
                predicates.add(builder.equal(root.get("definition").get("id"), definitionId));
            }
            if (assignedToId != null) {
                predicates.add(builder.equal(root.get("currentAssignee").get("id"), assignedToId));
            }
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (mineOnly && currentUserId != null) {
                predicates.add(builder.or(
                        builder.equal(root.get("requester").get("id"), currentUserId),
                        builder.equal(root.get("currentAssignee").get("id"), currentUserId)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return instanceRepository.findAll(spec, pageable).map(WorkflowService::toInstanceResponse);
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse findById(Long id) {
        return toInstanceResponse(requireInstance(id));
    }

    @Transactional(readOnly = true)
    public long countInProgress() {
        return instanceRepository.countInProgress();
    }

    @Transactional(readOnly = true)
    public long countInProgressForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userId == null ? 0 : instanceRepository.countInProgressByRequester(userId);
    }

    // ------------------------------------------------------------------- helpers

    /**
     * Moves the process to the next pending step, or closes it when none is left.
     *
     * <p>Steps that do not need a decision (task or notification steps) are
     * completed automatically so a template author does not have to model a
     * no-op approval.
     */
    private void advance(WorkflowInstance instance) {
        Instant now = Instant.now();
        List<WorkflowInstanceStep> ordered = instance.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(WorkflowInstanceStep::getStepOrder))
                .toList();

        Optional<WorkflowInstanceStep> next = ordered.stream()
                .filter(step -> step.getStatus() == WorkflowStepStatus.PENDING)
                .findFirst();

        if (next.isEmpty()) {
            instance.setStatus(WorkflowStatus.APPROVED);
            instance.setCompletedAt(now);
            instance.setCurrentAssignee(null);
            instance.setCurrentStepIndex(-1);
            instanceRepository.save(instance);
            notifyRequester(instance, "Workflow approved", instance.getReference() + " completed all steps.");
            return;
        }

        WorkflowInstanceStep step = next.get();
        step.setStatus(WorkflowStepStatus.IN_PROGRESS);
        step.setStartedAt(now);
        step.setAssignee(resolveAssignee(step));

        instance.setStatus(WorkflowStatus.UNDER_REVIEW);
        instance.setCurrentStepIndex(ordered.indexOf(step));
        instance.setCurrentAssignee(step.getAssignee());
        instanceRepository.save(instance);

        if (step.getAssignee() != null) {
            notificationService.notify(
                    step.getAssignee().getId(),
                    NotificationType.WORKFLOW_ASSIGNED.name(),
                    "Workflow step assigned: " + step.getStepName(),
                    instance.getReference() + " - " + instance.getTitle(),
                    "/workflows/" + instance.getId());
        }
    }

    /**
     * Picks the person who will act on a step.
     *
     * <p>An explicitly assigned user wins. Otherwise the first active holder of the
     * step's role is used. Returning {@code null} for a role with no members is
     * deliberate: the workflow waits, and an administrator can reassign it, instead
     * of silently skipping a required approval.
     */
    private User resolveAssignee(WorkflowInstanceStep step) {
        if (step.getAssignee() != null) {
            return step.getAssignee();
        }
        Role role = step.getAssigneeRole();
        if (role == null) {
            return null;
        }
        List<User> holders = userRepository.findActiveUsersByRoleCode(role.getCode());
        return holders.isEmpty() ? null : holders.get(0);
    }

    private static WorkflowInstanceStep currentStep(WorkflowInstance instance) {
        return instance.getSteps().stream()
                .filter(WorkflowInstanceStep::isPending)
                .min(java.util.Comparator.comparingInt(WorkflowInstanceStep::getStepOrder))
                .orElse(null);
    }

    /**
     * Only the assigned person may decide, unless they hold the explicit
     * {@code WORKFLOW_VIEW_ALL} permission, which covers administrators and
     * delegates. Keeping this here rather than only in {@code @PreAuthorize}
     * matters because the check depends on the instance's current step.
     */
    private void assertCanDecide(WorkflowInstance instance, WorkflowInstanceStep step) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAssignee = step.getAssignee() != null && step.getAssignee().getId().equals(currentUserId);
        if (isAssignee || hasPermission(Permissions.WORKFLOW_VIEW_ALL)) {
            return;
        }
        if (step.getAssignee() == null) {
            throw new BusinessException("This step has no assignee; an administrator must reassign it first");
        }
        throw new BusinessException("This step is assigned to someone else");
    }

    private void assertOwnedByRequester(WorkflowInstance instance) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isRequester = instance.getRequester() != null
                && instance.getRequester().getId().equals(currentUserId);
        if (!isRequester && !hasPermission(Permissions.WORKFLOW_VIEW_ALL)) {
            throw new BusinessException("Only the requester can perform this action");
        }
    }

    private static boolean hasPermission(String permission) {
        return SecurityUtils.principal()
                .map(principal -> principal.getAuthorities().stream()
                        .anyMatch(authority -> permission.equals(authority.getAuthority())))
                .orElse(false);
    }

    /** A running instance would otherwise silently change shape mid-flight. */
    private void assertNoRunningInstances(Long definitionId) {
        long running = instanceRepository
                .findAll(buildRunningInstanceSpec(definitionId), PageRequest.of(0, 1))
                .getTotalElements();
        if (running > 0) {
            throw new BusinessException(
                    running + " workflow instance(s) of this template are still running; wait for them to finish");
        }
    }

    private static Specification<WorkflowInstance> buildRunningInstanceSpec(Long definitionId) {
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("definition").get("id"), definitionId),
                root.get("status").in(List.of(
                        WorkflowStatus.SUBMITTED,
                        WorkflowStatus.UNDER_REVIEW,
                        WorkflowStatus.APPROVED)));
    }

    private void applySteps(WorkflowDefinition definition, WorkflowDefinitionRequest request) {
        definition.getSteps().clear();
        int order = 0;
        for (WorkflowDefinitionRequest.WorkflowStepRequest stepRequest : request.steps()) {
            WorkflowStepDefinition step = new WorkflowStepDefinition();
            step.setName(stepRequest.name().trim());
            step.setStepType(parseStepType(stepRequest.stepType()));
            step.setStepOrder(order++);
            step.setAllowRejection(stepRequest.allowRejection() == null || stepRequest.allowRejection());
            step.setInstructions(stepRequest.instructions());
            step.setAssigneeRole(resolveRole(stepRequest.assigneeRoleCode()));
            definition.addStep(step);
        }
    }

    private Role resolveRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return roleRepository
                .findByCode(roleCode.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException("Role '" + roleCode + "' does not exist"));
    }

    private static WorkflowStepType parseStepType(String value) {
        if (value == null || value.isBlank()) {
            return WorkflowStepType.APPROVAL;
        }
        try {
            return WorkflowStepType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unknown workflow step type '" + value + "'");
        }
    }

    private void notifyRequester(WorkflowInstance instance, String title, String message) {
        if (instance.getRequester() != null) {
            notificationService.notify(
                    instance.getRequester().getId(),
                    NotificationType.WORKFLOW_DECIDED.name(),
                    title,
                    message,
                    "/workflows/" + instance.getId());
        }
    }

    private User currentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("The current user no longer exists"));
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition requireDefinition(Long id) {
        return definitionRepository
                .findWithStepsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Workflow definition", id));
    }

    @Transactional(readOnly = true)
    public WorkflowInstance requireInstance(Long id) {
        return instanceRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Workflow instance", id));
    }

    // ------------------------------------------------------------------- mappers

    static WorkflowDefinitionResponse toDefinitionResponse(WorkflowDefinition definition) {
        List<WorkflowDefinitionResponse.Step> steps = definition.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(WorkflowStepDefinition::getStepOrder))
                .map(step -> new WorkflowDefinitionResponse.Step(
                        step.getId(),
                        step.getName(),
                        step.getStepType(),
                        step.getStepOrder(),
                        step.getAssigneeRole() == null ? null : step.getAssigneeRole().getCode(),
                        step.isAllowRejection(),
                        step.getInstructions()))
                .toList();
        return new WorkflowDefinitionResponse(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getDescription(),
                definition.isActive(),
                definition.getVersion(),
                steps,
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    static WorkflowInstanceResponse toInstanceResponse(WorkflowInstance instance) {
        WorkflowDefinition definition = instance.getDefinition();
        User requester = instance.getRequester();
        User assignee = instance.getCurrentAssignee();

        List<WorkflowInstanceResponse.Step> steps = instance.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(WorkflowInstanceStep::getStepOrder))
                .map(step -> new WorkflowInstanceResponse.Step(
                        step.getId(),
                        step.getStepName(),
                        step.getStepType(),
                        step.getStepOrder(),
                        step.getStatus(),
                        step.getAssignee() == null ? null : step.getAssignee().getId(),
                        step.getAssignee() == null ? null : step.getAssignee().getFullName(),
                        step.getDecisionComment(),
                        step.getStartedAt(),
                        step.getCompletedAt()))
                .toList();

        return new WorkflowInstanceResponse(
                instance.getId(),
                instance.getReference(),
                definition == null ? null : definition.getId(),
                definition == null ? null : definition.getCode(),
                definition == null ? null : definition.getName(),
                instance.getDefinitionVersion(),
                instance.getTitle(),
                instance.getDescription(),
                instance.getStatus(),
                requester == null ? null : requester.getId(),
                requester == null ? null : requester.getFullName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                instance.getCurrentStepIndex(),
                instance.getPayload(),
                steps,
                instance.getSubmittedAt(),
                instance.getCompletedAt(),
                instance.getCreatedAt());
    }
}