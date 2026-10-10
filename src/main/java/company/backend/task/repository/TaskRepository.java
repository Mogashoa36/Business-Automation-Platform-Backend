package company.backend.task.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import company.backend.task.entity.Task;
import company.backend.task.entity.TaskStatus;

/** Data access for {@link Task}. */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = {"assignedTo", "assignedDepartment"})
    List<Task> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"assignedTo", "assignedDepartment"})
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"assignedTo", "assignedDepartment"})
    Page<Task> findByAssignedToId(Long userId, Pageable pageable);

    long countByStatus(TaskStatus status);

    long countByAssignedToIdAndStatusNot(Long userId, TaskStatus status);

    @Query("select count(t) from Task t where t.status not in "
            + "(company.backend.task.entity.TaskStatus.COMPLETED, company.backend.task.entity.TaskStatus.CANCELLED)")
    long countOpen();

    @Query("select count(t) from Task t where t.dueDate < :today and t.status not in "
            + "(company.backend.task.entity.TaskStatus.COMPLETED, company.backend.task.entity.TaskStatus.CANCELLED)")
    long countOverdue(@Param("today") LocalDate today);

    long countByCreatedAtAfter(Instant since);

    /** Task due dates inside a window, used by the calendar view. */
    @EntityGraph(attributePaths = {"assignedTo"})
    List<Task> findByDueDateBetweenAndStatusNotIn(LocalDate from, LocalDate to, Collection<TaskStatus> excluded);
}