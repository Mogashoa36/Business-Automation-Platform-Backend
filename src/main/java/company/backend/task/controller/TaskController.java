package company.backend.task.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import company.backend.task.dto.CreateTaskRequest;
import company.backend.task.dto.TaskResponse;
import company.backend.task.dto.TaskSearchCriteria;
import company.backend.task.dto.UpdateTaskRequest;
import company.backend.task.entity.TaskPriority;
import company.backend.task.entity.TaskStatus;
import company.backend.task.service.TaskService;

import jakarta.validation.Valid;

/** Task endpoints with full search, filter, sort and pagination. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "title", "reference", "priority", "status", "dueDate", "createdAt", "updatedAt", "completedAt");

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.TASK_VIEW + "')")
    public ApiResponse<PageResponse<TaskResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) List<TaskStatus> statusIn,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) List<TaskPriority> priorityIn,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TaskSearchCriteria criteria = new TaskSearchCriteria(
                search, status, statusIn, priority, priorityIn, assignedToId, departmentId, overdue, dueFrom, dueTo);
        Pageable pageable = PageableFactory.create(page, size, sort, direction, SORTABLE_FIELDS, "createdAt");
        return ApiResponse.ok(PageResponse.of(taskService.search(criteria, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.TASK_VIEW + "')")
    public ApiResponse<TaskResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(taskService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.TASK_CREATE + "')")
    public ApiResponse<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.created("Task created", taskService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.TASK_UPDATE + "')")
    public ApiResponse<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok("Task updated", taskService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + Permissions.TASK_UPDATE + "')")
    public ApiResponse<TaskResponse> changeStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        return ApiResponse.ok("Task status updated", taskService.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.TASK_DELETE + "')")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}