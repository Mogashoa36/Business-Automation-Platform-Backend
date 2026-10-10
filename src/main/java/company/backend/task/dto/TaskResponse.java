package company.backend.task.dto;

import java.time.Instant;
import java.time.LocalDate;

import company.backend.task.entity.TaskPriority;
import company.backend.task.entity.TaskStatus;

/** Read model for a task. */
public record TaskResponse(
        Long id,
        String reference,
        String title,
        String description,
        Long assignedToId,
        String assignedToName,
        Long assignedDepartmentId,
        String assignedDepartmentName,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        boolean overdue,
        Integer estimatedHours,
        Integer actualHours,
        Instant startedAt,
        Instant completedAt,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt) {
}