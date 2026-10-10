package company.backend.approval.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.approval.dto.ApprovalActionResponse;
import company.backend.approval.dto.ApprovalDecisionRequest;
import company.backend.approval.dto.ApprovalResponse;
import company.backend.approval.dto.CreateApprovalRequest;
import company.backend.approval.entity.ApprovalAction;
import company.backend.approval.entity.ApprovalRequest;
import company.backend.approval.entity.ApprovalStatus;
import company.backend.approval.entity.ApprovalStep;
import company.backend.approval.entity.ApprovalStepStatus;
import company.backend.approval.repository.ApprovalActionRepository;
import company.backend.approval.repository.ApprovalRequestRepository;
import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.notification.entity.NotificationType;
import company.backend.notification.service.NotificationService;
import company.backend.role.entity.Role;
import company.backend.role.repository.RoleRepository;
import company.backend.security.Permissions;
import company.backend.security.SecurityUtils;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

/**
 * Generic multi-stage approval.
 *
 * <p>Stages are supplied by the caller rather than hard coded, so a client project
 * can model "manager then finance" or any other chain purely through configuration.
 * Each stage is delegated to a role or a named person, and stages run in order.
 */
@Service
@Transactional
public class ApprovalService {

    private static final String REFERENCE_PREFIX = "APR-";

    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalActionRepository actionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ApprovalService(
            ApprovalRequestRepository approvalRepository,
            ApprovalActionRepository actionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.approvalRepository = approvalRepository;
        this.actionRepository = actionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** Creates a request with its stages and, optionally, submits it immediately. */
    @Audited(module = "approval", action = AuditAction.CREATE, entityType = "ApprovalRequest")
    public ApprovalResponse create(CreateApprovalRequest request) {
        ApprovalRequest approval = new ApprovalRequest();
        approval.setTitle(request.title().trim());
        approval.setDescription(request.description());
        approval.setCategory(request.category());
        approval.setAmount(request.amount());
        approval.setCurrency(request.currency());
        approval.setStatus(ApprovalStatus.DRAFT);
        approval.setRequester(currentUser());

        int order = 0;
        for (CreateApprovalRequest.StagePayload payload : request.stages()) {
            ApprovalStep step = new ApprovalStep();
            step.setName(payload.name().trim());
            step.setStepOrder(order++);
            step.setStatus(ApprovalStepStatus.PENDING);
            step.setApproverRole(resolveRole(payload.approverRoleCode()));
            step.setApprover(resolveApprover(payload.approverId()));
            approval.addStep(step);
        }

        ApprovalRequest saved = approvalRepository.save(approval);
        saved.setReference(REFERENCE_PREFIX + String.format("%06d", saved.getId()));
        saved = approvalRepository.save(saved);

        if (request.shouldSubmit()) {
            return submit(saved.getId());
        }
        return findById(saved.getId());
    }

    @Audited(module = "approval", action = AuditAction.SUBMIT, entityType = "ApprovalRequest")
    public ApprovalResponse submit(Long id) {
        ApprovalRequest approval = requireApproval(id);
        if (!approval.getStatus().isTerminal() && approval.getStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("This approval has already been submitted");
        }
        assertOwnedByRequester(approval);

        approval.setStatus(ApprovalStatus.SUBMITTED);
        approval.setSubmittedAt(Instant.now());
        approvalRepository.save(approval);
        advance(approval);
        return findById(id);
    }

    /**
     * Records a decision on the active stage.
     *
     * <p>Approving opens the next stage, or approves the whole request when this
     * was the last. Rejecting ends the chain immediately, because a partially
     * approved request has no useful meaning.
     */
    @Audited(module = "approval", action = AuditAction.APPROVE, entityType = "ApprovalRequest")
    public ApprovalResponse decide(Long id, ApprovalDecisionRequest request) {
        ApprovalRequest approval = requireApproval(id);
        if (approval.getStatus().isTerminal()) {
            throw new BusinessException("This approval has already been decided");
        }
        if (approval.getStatus() != ApprovalStatus.IN_REVIEW) {
            throw new BusinessException("This approval is not awaiting a decision");
        }

        ApprovalStep step = currentStep(approval);
        if (step == null) {
            throw new BusinessException("No active stage was found for this approval");
        }
        assertCanDecide(step);

        Instant now = Instant.now();
        User actor = currentUser();
        step.setStatus(request.approved() ? ApprovalStepStatus.APPROVED : ApprovalStepStatus.REJECTED);
        step.setComment(request.comment());
        step.setDecidedAt(now);
        step.setDecidedById(actor == null ? null : actor.getId());
        approvalRepository.save(approval);

        recordAction(approval, step, actor, request.approved() ? "APPROVED" : "REJECTED", request.comment());

        if (!request.approved()) {
            approval.setStatus(ApprovalStatus.REJECTED);
            approval.setCompletedAt(now);
            approval.setCurrentStepIndex(-1);
            approvalRepository.save(approval);
            notifyRequester(approval, "Approval rejected", approval.getReference() + " was rejected at '"
                    + step.getName() + "'.");
            return findById(id);
        }

        advance(approval);
        return findById(id);
    }

    @Audited(module = "approval", action = AuditAction.CANCEL, entityType = "ApprovalRequest")
    public ApprovalResponse cancel(Long id) {
        ApprovalRequest approval = requireApproval(id);
        if (approval.getStatus().isTerminal()) {
            throw new BusinessException("This approval has already been decided");
        }
        assertOwnedByRequester(approval);

        approval.getSteps().stream()
                .filter(ApprovalStep::isPending)
                .forEach(step -> step.setStatus(ApprovalStepStatus.SKIPPED));
        approval.setStatus(ApprovalStatus.CANCELLED);
        approval.setCompletedAt(Instant.now());
        approval.setCurrentStepIndex(-1);
        return toResponse(approvalRepository.save(approval));
    }

    // -------------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> search(
            String search, ApprovalStatus status, String category, Long requesterId, Pageable pageable) {

        Specification<ApprovalRequest> spec = (root, query, builder) -> {
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
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(root.get("category"), category));
            }
            if (requesterId != null) {
                predicates.add(builder.equal(root.get("requester").get("id"), requesterId));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return approvalRepository.findAll(spec, pageable).map(ApprovalService::toResponse);
    }

    @Transactional(readOnly = true)
    public ApprovalResponse findById(Long id) {
        return toResponse(requireApproval(id));
    }

    /** Approvals waiting on the current user; the "my approvals" queue. */
    @Transactional(readOnly = true)
    public List<ApprovalResponse> findPendingForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return approvalRepository.findPendingStepsForUser(userId).stream()
                .map(ApprovalStep::getApproval)
                .distinct()
                .map(ApprovalService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalActionResponse> findHistory(Long approvalId) {
        requireApproval(approvalId);
        return actionRepository.findByApprovalIdOrderByCreatedAtAsc(approvalId).stream()
                .map(ApprovalService::toActionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return approvalRepository.countOpen();
    }

    @Transactional(readOnly = true)
    public long countPendingForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userId == null ? 0 : approvalRepository.findPendingStepsForUser(userId).size();
    }

    // ------------------------------------------------------------------- helpers

    /** Opens the next stage, or approves the whole request when none is left. */
    private void advance(ApprovalRequest approval) {
        List<ApprovalStep> ordered = approval.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(ApprovalStep::getStepOrder))
                .toList();

        ApprovalStep next = ordered.stream()
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (next == null) {
            approval.setStatus(ApprovalStatus.APPROVED);
            approval.setCompletedAt(Instant.now());
            approval.setCurrentStepIndex(-1);
            approvalRepository.save(approval);
            notifyRequester(approval, "Approval granted", approval.getReference() + " completed all stages.");
            return;
        }

        next.setStatus(ApprovalStepStatus.IN_REVIEW);
        approval.setStatus(ApprovalStatus.IN_REVIEW);
        approval.setCurrentStepIndex(ordered.indexOf(next));
        approvalRepository.save(approval);

        if (next.getApprover() != null) {
            notificationService.notify(
                    next.getApprover().getId(),
                    NotificationType.APPROVAL_REQUESTED.name(),
                    "Approval requested: " + next.getName(),
                    approval.getReference() + " - " + approval.getTitle(),
                    "/approvals/" + approval.getId());
        }
    }

    private static ApprovalStep currentStep(ApprovalRequest approval) {
        return approval.getSteps().stream()
                .filter(ApprovalStep::isPending)
                .min(java.util.Comparator.comparingInt(ApprovalStep::getStepOrder))
                .orElse(null);
    }

    /**
     * A named approver wins; otherwise any active holder of the stage's role may
     * act. Administrators holding {@code APPROVAL_VIEW_ALL} can always act, which
     * is how a client project implements delegation and absence cover.
     */
    private void assertCanDecide(ApprovalStep step) {
        if (hasPermission(Permissions.APPROVAL_VIEW_ALL)) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (step.getApprover() != null && step.getApprover().getId().equals(userId)) {
            return;
        }
        Role role = step.getApproverRole();
        if (role != null && userId != null) {
            boolean member = userRepository.findActiveUsersByRoleCode(role.getCode()).stream()
                    .anyMatch(user -> user.getId().equals(userId));
            if (member) {
                return;
            }
        }
        if (step.getApprover() == null) {
            throw new BusinessException("This stage has no approver; an administrator must assign one");
        }
        throw new BusinessException("This approval is assigned to someone else");
    }

    private void assertOwnedByRequester(ApprovalRequest approval) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isRequester = approval.getRequester() != null
                && approval.getRequester().getId().equals(userId);
        if (!isRequester && !hasPermission(Permissions.APPROVAL_VIEW_ALL)) {
            throw new BusinessException("Only the requester can perform this action");
        }
    }

    private static boolean hasPermission(String permission) {
        return SecurityUtils.principal()
                .map(principal -> principal.getAuthorities().stream()
                        .anyMatch(authority -> permission.equals(authority.getAuthority())))
                .orElse(false);
    }

    private void recordAction(ApprovalRequest approval, ApprovalStep step, User actor, String action, String comment) {
        ApprovalAction entry = new ApprovalAction();
        entry.setApproval(approval);
        entry.setStep(step);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setComment(comment);
        actionRepository.save(entry);
    }

    private void notifyRequester(ApprovalRequest approval, String title, String message) {
        if (approval.getRequester() != null) {
            notificationService.notify(
                    approval.getRequester().getId(),
                    NotificationType.APPROVAL_DECIDED.name(),
                    title,
                    message,
                    "/approvals/" + approval.getId());
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

    private User resolveApprover(Long approverId) {
        if (approverId == null) {
            return null;
        }
        return userRepository
                .findById(approverId)
                .orElseThrow(() -> new BusinessException("Approver " + approverId + " does not exist"));
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
    public ApprovalRequest requireApproval(Long id) {
        return approvalRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Approval", id));
    }

    // ------------------------------------------------------------------- mappers

    static ApprovalResponse toResponse(ApprovalRequest approval) {
        User requester = approval.getRequester();
        List<ApprovalResponse.Step> steps = approval.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(ApprovalStep::getStepOrder))
                .map(step -> new ApprovalResponse.Step(
                        step.getId(),
                        step.getName(),
                        step.getStepOrder(),
                        step.getStatus(),
                        step.getApprover() == null ? null : step.getApprover().getId(),
                        step.getApprover() == null ? null : step.getApprover().getFullName(),
                        step.getApproverRole() == null ? null : step.getApproverRole().getCode(),
                        step.getComment(),
                        step.getDecidedAt(),
                        step.getDecidedById()))
                .toList();

        return new ApprovalResponse(
                approval.getId(),
                approval.getReference(),
                approval.getTitle(),
                approval.getDescription(),
                approval.getStatus(),
                requester == null ? null : requester.getId(),
                requester == null ? null : requester.getFullName(),
                approval.getAmount(),
                approval.getCurrency(),
                approval.getCategory(),
                approval.getSourceEntityType(),
                approval.getSourceEntityId(),
                approval.getCurrentStepIndex(),
                steps,
                approval.getSubmittedAt(),
                approval.getCompletedAt(),
                approval.getCreatedAt());
    }

    static ApprovalActionResponse toActionResponse(ApprovalAction action) {
        User actor = action.getActor();
        return new ApprovalActionResponse(
                action.getId(),
                action.getStep() == null ? null : action.getStep().getId(),
                action.getStep() == null ? null : action.getStep().getName(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getFullName(),
                action.getAction(),
                action.getComment(),
                action.getCreatedAt());
    }
}