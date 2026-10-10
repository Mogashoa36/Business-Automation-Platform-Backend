package company.backend.task.dto;

import java.time.LocalDate;
import java.util.List;

import company.backend.task.entity.TaskPriority;
import company.backend.task.entity.TaskStatus;

/**
 * Query criteria for the task list.
 *
 * <p>Grouping the filters into one object keeps the controller and the service
 * signature stable as new filters are added.
 */
public record TaskSearchCriteria(
        String search,
        TaskStatus status,
        List<TaskStatus> statuses,
        TaskPriority priority,
        List<TaskPriority> priorities,
        Long assignedToId,
        Long departmentId,
        Boolean overdue,
        LocalDate dueFrom,
        LocalDate dueTo) {
}