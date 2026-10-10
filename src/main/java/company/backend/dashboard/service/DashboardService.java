package company.backend.dashboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.approval.entity.ApprovalStatus;
import company.backend.approval.repository.ApprovalRequestRepository;
import company.backend.approval.service.ApprovalService;
import company.backend.audit.entity.AuditLog;
import company.backend.audit.repository.AuditLogRepository;
import company.backend.dashboard.dto.DashboardResponse;
import company.backend.notification.service.NotificationService;
import company.backend.security.SecurityUtils;
import company.backend.task.entity.TaskStatus;
import company.backend.task.repository.TaskRepository;
import company.backend.task.service.TaskService;
import company.backend.user.entity.UserStatus;
import company.backend.user.repository.UserRepository;
import company.backend.workflow.repository.WorkflowInstanceRepository;
import company.backend.workflow.service.WorkflowService;

/**
 * Dashboard aggregation.
 *
 * <p>Every number here is a live count from the database. The template ships no
 * static or sample figures, so a freshly copied project shows an honest - possibly
 * empty - dashboard rather than misleading placeholder data.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LIST_LIMIT = 5;

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final WorkflowInstanceRepository workflowRepository;
    private final AuditLogRepository auditLogRepository;
    private final TaskService taskService;
    private final ApprovalService approvalService;
    private final WorkflowService workflowService;
    private final NotificationService notificationService;

    public DashboardService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            ApprovalRequestRepository approvalRepository,
            WorkflowInstanceRepository workflowRepository,
            AuditLogRepository auditLogRepository,
            TaskService taskService,
            ApprovalService approvalService,
            WorkflowService workflowService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.approvalRepository = approvalRepository;
        this.workflowRepository = workflowRepository;
        this.auditLogRepository = auditLogRepository;
        this.taskService = taskService;
        this.approvalService = approvalService;
        this.workflowService = workflowService;
        this.notificationService = notificationService;
    }

    /** Builds the whole dashboard in one call so the UI makes a single request. */
    public DashboardResponse build() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return new DashboardResponse(
                summaryCards(),
                recentActivity(),
                myTasks(currentUserId),
                pendingApprovals(currentUserId),
                pendingWorkflows(currentUserId),
                taskStatusBreakdown(),
                approvalStatusBreakdown());
    }

    private List<DashboardResponse.SummaryCard> summaryCards() {
        List<DashboardResponse.SummaryCard> cards = new ArrayList<>();
        cards.add(card("totalUsers", "Total users", userRepository.count(), "/users", "users"));
        cards.add(card(
                "activeUsers", "Active users", userRepository.countByStatus(UserStatus.ACTIVE), "/users", "user-check"));
        cards.add(card("openTasks", "Open tasks", taskRepository.countOpen(), "/tasks", "check-square"));
        cards.add(card(
                "overdueTasks", "Overdue tasks", taskService.countOverdue(), "/tasks?overdue=true", "alert-triangle"));
        cards.add(card("pendingApprovals", "Pending approvals", approvalRepository.countOpen(), "/approvals", "check-circle"));
        cards.add(card(
                "workflowsInProgress",
                "Workflows in progress",
                workflowRepository.countInProgress(),
                "/workflows",
                "git-branch"));
        cards.add(card(
                "unreadNotifications",
                "Unread notifications",
                notificationService.countUnread(),
                "/notifications",
                "bell"));
        return cards;
    }

    private static DashboardResponse.SummaryCard card(
            String key, String label, long value, String linkUrl, String icon) {
        return new DashboardResponse.SummaryCard(key, label, value, linkUrl, null, icon);
    }

    private List<DashboardResponse.ActivityItem> recentActivity() {
        return auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(DashboardService::toActivityItem)
                .toList();
    }

    private List<DashboardResponse.WorkItem> myTasks(Long currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        return taskRepository
                .findByAssignedToId(
                        currentUserId, PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.ASC, "dueDate")))
                .getContent()
                .stream()
                .filter(task -> !task.getStatus().isClosed())
                .map(task -> new DashboardResponse.WorkItem(
                        task.getId(),
                        task.getReference(),
                        task.getTitle(),
                        task.getStatus().name(),
                        task.getDueDate() == null ? null : "Due " + task.getDueDate(),
                        "/tasks/" + task.getId()))
                .toList();
    }

    private List<DashboardResponse.WorkItem> pendingApprovals(Long currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        return approvalService.findPendingForCurrentUser().stream()
                .limit(LIST_LIMIT)
                .map(approval -> new DashboardResponse.WorkItem(
                        approval.id(),
                        approval.reference(),
                        approval.title(),
                        approval.status().name(),
                        approval.category(),
                        "/approvals/" + approval.id()))
                .toList();
    }

    private List<DashboardResponse.WorkItem> pendingWorkflows(Long currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        return workflowRepository
                .findByCurrentAssigneeId(currentUserId, PageRequest.of(0, LIST_LIMIT))
                .getContent()
                .stream()
                .filter(instance -> !instance.getStatus().isTerminal())
                .map(instance -> new DashboardResponse.WorkItem(
                        instance.getId(),
                        instance.getReference(),
                        instance.getTitle(),
                        instance.getStatus().name(),
                        instance.getDefinition() == null ? null : instance.getDefinition().getName(),
                        "/workflows/" + instance.getId()))
                .toList();
    }

    /** Counts per task status; drives the distribution chart. */
    private List<DashboardResponse.StatusCount> taskStatusBreakdown() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status.name(), taskRepository.countByStatus(status));
        }
        return counts.entrySet().stream()
                .map(entry -> new DashboardResponse.StatusCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DashboardResponse.StatusCount> approvalStatusBreakdown() {
        return java.util.Arrays.stream(ApprovalStatus.values())
                .map(status ->
                        new DashboardResponse.StatusCount(status.name(), approvalRepository.countByStatus(status)))
                .toList();
    }

    private static DashboardResponse.ActivityItem toActivityItem(AuditLog log) {
        return new DashboardResponse.ActivityItem(
                log.getId(),
                log.getAction(),
                log.getModule(),
                log.getDescription(),
                log.getUsername(),
                log.getCreatedAt() == null ? null : log.getCreatedAt().toString());
    }
}