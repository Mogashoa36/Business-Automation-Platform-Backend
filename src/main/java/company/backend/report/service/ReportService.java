package company.backend.report.service;

import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.approval.entity.ApprovalStatus;
import company.backend.approval.repository.ApprovalRequestRepository;
import company.backend.exception.ResourceNotFoundException;
import company.backend.report.dto.ReportDefinition;
import company.backend.report.dto.ReportResponse;
import company.backend.security.Permissions;
import company.backend.task.entity.Task;
import company.backend.task.entity.TaskStatus;
import company.backend.task.repository.TaskRepository;
import company.backend.user.repository.UserRepository;
import company.backend.workflow.entity.WorkflowStatus;
import company.backend.workflow.repository.WorkflowInstanceRepository;

/**
 * Generic reporting.
 *
 * <p>Reports are small composable queries over the platform's own tables. Client
 * specific reports are added the same way, next to their domain, rather than by
 * growing this class.
 *
 * <p>CSV output follows RFC 4180 and neutralises any cell starting with a formula
 * character, so exporting a value such as {@code =cmd()} cannot turn the receiving
 * spreadsheet into an execution vector.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    public static final String TASK_COMPLETION = "TASK_COMPLETION";
    public static final String USER_ACTIVITY = "USER_ACTIVITY";
    public static final String APPROVAL_VOLUME = "APPROVAL_VOLUME";
    public static final String WORKFLOW_THROUGHPUT = "WORKFLOW_THROUGHPUT";

    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final WorkflowInstanceRepository workflowRepository;

    public ReportService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            ApprovalRequestRepository approvalRepository,
            WorkflowInstanceRepository workflowRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.approvalRepository = approvalRepository;
        this.workflowRepository = workflowRepository;
    }

    /** The reports this deployment offers, filtered to what the caller may see. */
    public List<ReportDefinition> catalog(List<String> grantedPermissions) {
        List<ReportDefinition> all = List.of(
                new ReportDefinition(
                        TASK_COMPLETION,
                        "Task completion",
                        "Completed, open and overdue tasks per assignee",
                        Permissions.REPORT_VIEW,
                        true,
                        true),
                new ReportDefinition(
                        USER_ACTIVITY,
                        "User activity",
                        "Registered users, activation state and last sign-in",
                        Permissions.USER_VIEW,
                        true,
                        true),
                new ReportDefinition(
                        APPROVAL_VOLUME,
                        "Approval volume",
                        "Approval requests grouped by status",
                        Permissions.REPORT_VIEW,
                        true,
                        true),
                new ReportDefinition(
                        WORKFLOW_THROUGHPUT,
                        "Workflow throughput",
                        "Workflow instances grouped by status",
                        Permissions.WORKFLOW_VIEW,
                        true,
                        true));
        return all.stream()
                .filter(definition -> grantedPermissions.contains(definition.requiredPermission()))
                .toList();
    }

    /** Executes a report. An unknown key yields 404 rather than an empty result. */
    public ReportResponse run(String key, LocalDate from, LocalDate to, Long departmentId) {
        return switch (key == null ? "" : key.toUpperCase(Locale.ROOT)) {
            case TASK_COMPLETION -> taskCompletion(from, to, departmentId);
            case USER_ACTIVITY -> userActivity(from, to);
            case APPROVAL_VOLUME -> statusCounts(
                    ApprovalStatus.values(), approvalRepository::countByStatus, APPROVAL_VOLUME, "Approval volume");
            case WORKFLOW_THROUGHPUT -> statusCounts(
                    WorkflowStatus.values(), workflowRepository::countByStatus, WORKFLOW_THROUGHPUT, "Workflow throughput");
            default -> throw ResourceNotFoundException.of("Report", key);
        };
    }

    /** Renders a report as CSV. */
    public String toCsv(ReportResponse report) {
        StringBuilder writer = new StringBuilder();
        writer.append(report.columns().stream()
                        .map(ReportResponse.Column::label)
                        .map(ReportService::escapeCsv)
                        .collect(Collectors.joining(",")))
                .append('\n');
        for (Map<String, Object> row : report.rows()) {
            writer.append(report.columns().stream()
                            .map(column -> escapeCsv(String.valueOf(row.get(column.key()))))
                            .collect(Collectors.joining(",")))
                    .append('\n');
        }
        return writer.toString();
    }

    private ReportResponse taskCompletion(LocalDate from, LocalDate to, Long departmentId) {
        List<ReportResponse.Column> columns = List.of(
                new ReportResponse.Column("assignee", "Assignee", "text"),
                new ReportResponse.Column("total", "Tasks", "number"),
                new ReportResponse.Column("completed", "Completed", "number"),
                new ReportResponse.Column("open", "Open", "number"),
                new ReportResponse.Column("overdue", "Overdue", "number"));

        List<Task> tasks = taskRepository.findAll().stream()
                .filter(task -> departmentId == null
                        || (task.getAssignedDepartment() != null
                                && departmentId.equals(task.getAssignedDepartment().getId())))
                .filter(task -> withinRange(task.getCreatedAt(), from, to))
                .toList();

        Map<String, List<Task>> grouped = new LinkedHashMap<>();
        for (Task task : tasks) {
            String key = task.getAssignedTo() == null ? "Unassigned" : task.getAssignedTo().getFullName();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(task);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.forEach((assignee, assigned) -> {
            long completed = assigned.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
            long overdue = assigned.stream().filter(task -> task.isOverdue(LocalDate.now())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("assignee", assignee);
            row.put("total", assigned.size());
            row.put("completed", completed);
            row.put("open", assigned.size() - completed);
            row.put("overdue", overdue);
            rows.add(row);
        });

        return new ReportResponse(TASK_COMPLETION, "Task completion", "Tasks per assignee", columns, rows, rows.size());
    }

    private ReportResponse userActivity(LocalDate from, LocalDate to) {
        List<ReportResponse.Column> columns = List.of(
                new ReportResponse.Column("username", "Username", "text"),
                new ReportResponse.Column("fullName", "Name", "text"),
                new ReportResponse.Column("email", "Email", "text"),
                new ReportResponse.Column("status", "Status", "status"),
                new ReportResponse.Column("lastLogin", "Last sign-in", "date"));

        List<Map<String, Object>> rows = userRepository.findAll().stream()
                .filter(user -> withinRange(user.getCreatedAt(), from, to))
                .map(user -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("username", user.getUsername());
                    row.put("fullName", user.getFullName());
                    row.put("email", user.getEmail());
                    row.put("status", user.getStatus().name());
                    row.put("lastLogin", user.getLastLoginAt());
                    return row;
                })
                .toList();

        return new ReportResponse(USER_ACTIVITY, "User activity", "Registered users", columns, rows, rows.size());
    }

    /**
     * Shared shape for the "count per status" reports.
     *
     * <p>Generic over the enum so the counter stays type safe - a caller cannot
     * pass a mismatched function.
     */
    private <E extends Enum<E>> ReportResponse statusCounts(
            E[] statuses, Function<E, Long> counter, String key, String title) {

        List<ReportResponse.Column> columns = List.of(
                new ReportResponse.Column("status", "Status", "status"),
                new ReportResponse.Column("count", "Count", "number"));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (E status : statuses) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", status.name());
            row.put("count", counter.apply(status));
            rows.add(row);
        }
        return new ReportResponse(key, title, "Count per status", columns, rows, rows.size());
    }

    private static boolean withinRange(Instant createdAt, LocalDate from, LocalDate to) {
        if (createdAt == null) {
            return true;
        }
        LocalDate date = createdAt.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    /** RFC 4180 quoting, plus spreadsheet formula neutralisation. */
    static String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && FORMULA_PREFIXES.indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains("\"") || safe.contains(",") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}