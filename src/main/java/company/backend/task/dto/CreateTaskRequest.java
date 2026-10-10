package company.backend.task.dto;

import java.time.LocalDate;

import company.backend.task.entity.TaskPriority;
import company.backend.task.entity.TaskStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating a task. */
public record CreateTaskRequest(
        @NotBlank @Size(max = 250) String title,
        @Size(max = 4000) String description,
        Long assignedToId,
        Long assignedDepartmentId,
        TaskPriority priority,
        LocalDate dueDate,
        @Min(value = 0, message = "Estimated hours cannot be negative")
        @Max(value = 100000, message = "Estimated hours is unrealistically large")
                Integer estimatedHours) {
}