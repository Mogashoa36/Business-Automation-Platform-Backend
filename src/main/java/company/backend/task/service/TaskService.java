package company.backend.task.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.audit.service.AuditService;
import company.backend.department.entity.Department;
import company.backend.department.repository.DepartmentRepository;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.security.SecurityUtils;
import company.backend.task.dto.CreateTaskRequest;
import company.backend.task.dto.TaskResponse;
import company.backend.task.dto.TaskSearchCriteria;
import company.backend.task.dto.UpdateTaskRequest;
import company.backend.task.entity.Task;
import company.backend.task.entity.TaskPriority;
import company.backend.task.entity.TaskStatus;
import company.backend.task.repository.TaskRepository;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** Task lifecycle: creation, assignment, status transitions and reporting counts. */
@Service
@Transactional
public class TaskService {

    private static final String REFERENCE_PREFIX = "TASK-";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AuditService auditService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> search(TaskSearchCriteria criteria, Pageable pageable) {
        return taskRepository.findAll(buildSpecification(criteria), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(requireTask(id));
    }

    @Audited(module = "task", action = AuditAction.CREATE, entityType = "Task")
    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setAssignedTo(resolveAssignee(request.assignedToId()));
        task.setAssignedDepartment(resolveDepartment(request.assignedDepartmentId()));
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(request.dueDate());
        task.setEstimatedHours(request.estimatedHours());

        Task saved = taskRepository.save(task);
        // The reference embeds the generated id, so it can only be set after insert.
        saved.setReference(REFERENCE_PREFIX + String.format("%06d", saved.getId()));
        return toResponse(taskRepository.save(saved));
    }

    @Audited(module = "task", action = AuditAction.UPDATE, entityType = "Task")
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = requireTask(id);
        TaskResponse before = toResponse(task);

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setAssignedTo(resolveAssignee(request.assignedToId()));
        task.setAssignedDepartment(resolveDepartment(request.assignedDepartmentId()));
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());
        task.setEstimatedHours(request.estimatedHours());
        task.setActualHours(request.actualHours());

        TaskResponse saved = toResponse(taskRepository.save(task));
        auditService.recordChange(AuditAction.UPDATE, "task", "Task", id, before, saved, "Task updated");
        return saved;
    }

    /** Applies a status change, maintaining the started/completed timestamps. */
    @Audited(module = "task", action = AuditAction.UPDATE, entityType = "Task")
    public TaskResponse changeStatus(Long id, TaskStatus status) {
        Task task = requireTask(id);
        if (task.getStatus() == status) {
            return toResponse(task);
        }
        if (task.getStatus().isClosed()) {
            throw new BusinessException("A " + task.getStatus() + " task cannot change status");
        }

        Instant now = Instant.now();
        if (status == TaskStatus.IN_PROGRESS && task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        task.setCompletedAt(status.isClosed() ? now : null);
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    @Audited(module = "task", action = AuditAction.DELETE, entityType = "Task")
    public void delete(Long id) {
        taskRepository.delete(requireTask(id));
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return taskRepository.countOpen();
    }

    @Transactional(readOnly = true)
    public long countOverdue() {
        return taskRepository.countOverdue(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long countOpenForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userId == null ? 0 : taskRepository.countByAssignedToIdAndStatusNot(userId, TaskStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public Task requireTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Task", id));
    }

    private static Specification<Task> buildSpecification(TaskSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String pattern = "%" + criteria.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("reference")), pattern)));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
                predicates.add(root.get("status").in(criteria.statuses()));
            }
            if (criteria.priority() != null) {
                predicates.add(builder.equal(root.get("priority"), criteria.priority()));
            }
            if (criteria.priorities() != null && !criteria.priorities().isEmpty()) {
                predicates.add(root.get("priority").in(criteria.priorities()));
            }
            if (criteria.assignedToId() != null) {
                predicates.add(builder.equal(root.get("assignedTo").get("id"), criteria.assignedToId()));
            }
            if (criteria.departmentId() != null) {
                predicates.add(builder.equal(root.get("assignedDepartment").get("id"), criteria.departmentId()));
            }
            if (criteria.dueFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dueDate"), criteria.dueFrom()));
            }
            if (criteria.dueTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dueDate"), criteria.dueTo()));
            }
            if (Boolean.TRUE.equals(criteria.overdue())) {
                predicates.add(builder.and(
                        builder.lessThan(root.get("dueDate"), LocalDate.now()),
                        root.get("status").in(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.ON_HOLD))));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private User resolveAssignee(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("Assignee " + userId + " does not exist"));
        if (!user.isActive()) {
            throw new BusinessException("Tasks cannot be assigned to an inactive user");
        }
        return user;
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new BusinessException("Department " + departmentId + " does not exist"));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getReference(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignedTo() == null ? null : task.getAssignedTo().getId(),
                task.getAssignedTo() == null ? null : task.getAssignedTo().getFullName(),
                task.getAssignedDepartment() == null ? null : task.getAssignedDepartment().getId(),
                task.getAssignedDepartment() == null ? null : task.getAssignedDepartment().getName(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.isOverdue(LocalDate.now()),
                task.getEstimatedHours(),
                task.getActualHours(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}