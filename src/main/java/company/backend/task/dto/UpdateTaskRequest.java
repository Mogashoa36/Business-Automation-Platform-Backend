package company.backend.task.dto;

import java.time.LocalDate;

import company.backend.task.entity.TaskPriority;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for editing a task.
 *
 * <p>Status is changed through the dedicated status endpoint rather than here, so
 * that the transition rules (and the completion timestamp) stay in one place.
 */
public record UpdateTaskRequest(
        @NotBlank @Size(max = 250) String title,
        @Size(max = 4000) String description,
        Long assignedToId,
        Long assignedDepartmentId,
        TaskPriority priority,
        LocalDate dueDate,
        @Min(0) @Max(100000) Integer estimatedHours,
        @Min(0) @Max(100000) Integer actualHours) {
}