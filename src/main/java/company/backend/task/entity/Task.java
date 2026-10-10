package company.backend.task.entity;

import java.time.Instant;
import java.time.LocalDate;

import company.backend.common.BaseEntity;
import company.backend.department.entity.Department;
import company.backend.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * A unit of assigned work.
 *
 * <p>Deliberately domain neutral: a task is a title, an owner, a due date and a
 * state. Client projects layer their own meaning on top by relating a task to
 * their entities rather than by extending this class.
 *
 * <p>{@code @Version} provides optimistic locking, so two people completing the
 * same task concurrently cannot silently overwrite each other.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human readable identifier such as {@code TASK-000042}. */
    @Column(name = "reference", unique = true, length = 32)
    private String reference;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_department_id")
    private Department assignedDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "actual_hours")
    private Integer actualHours;

    @Version
    @Column(name = "version")
    private Long version;

    public boolean isOverdue(java.time.LocalDate today) {
        return dueDate != null && !status.isClosed() && dueDate.isBefore(today);
    }
}