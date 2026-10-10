package company.backend.workflow.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import company.backend.workflow.entity.WorkflowInstance;
import company.backend.workflow.entity.WorkflowStatus;

/** Data access for {@link WorkflowInstance}. */
public interface WorkflowInstanceRepository
        extends JpaRepository<WorkflowInstance, Long>, JpaSpecificationExecutor<WorkflowInstance> {

    @EntityGraph(attributePaths = {"definition", "requester", "currentAssignee"})
    Optional<WorkflowInstance> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"definition", "requester", "currentAssignee"})
    Page<WorkflowInstance> findByStatus(WorkflowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"definition", "requester", "currentAssignee"})
    Page<WorkflowInstance> findByRequesterId(Long requesterId, Pageable pageable);

    @EntityGraph(attributePaths = {"definition", "requester", "currentAssignee"})
    Page<WorkflowInstance> findByCurrentAssigneeId(Long assigneeId, Pageable pageable);

    long countByStatus(WorkflowStatus status);

    @Query("select count(i) from WorkflowInstance i where i.status not in "
            + "(company.backend.workflow.entity.WorkflowStatus.COMPLETED, "
            + "company.backend.workflow.entity.WorkflowStatus.CANCELLED, "
            + "company.backend.workflow.entity.WorkflowStatus.REJECTED, "
            + "company.backend.workflow.entity.WorkflowStatus.DRAFT)")
    long countInProgress();

    @Query("select count(i) from WorkflowInstance i where i.requester.id = :userId and i.status not in "
            + "(company.backend.workflow.entity.WorkflowStatus.COMPLETED, "
            + "company.backend.workflow.entity.WorkflowStatus.CANCELLED, "
            + "company.backend.workflow.entity.WorkflowStatus.REJECTED, "
            + "company.backend.workflow.entity.WorkflowStatus.DRAFT)")
    long countInProgressByRequester(@Param("userId") Long userId);

    /** Steps currently waiting on a specific person; powers the "my approvals" view. */
    @Query("select s from company.backend.workflow.entity.WorkflowInstanceStep s "
            + "where s.assignee.id = :userId and s.status in "
            + "(company.backend.workflow.entity.WorkflowStepStatus.PENDING, "
            + "company.backend.workflow.entity.WorkflowStepStatus.IN_PROGRESS) "
            + "order by s.startedAt desc")
    List<company.backend.workflow.entity.WorkflowInstanceStep> findPendingStepsForUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"definition", "requester", "currentAssignee"})
    List<WorkflowInstance> findTop5ByOrderByCreatedAtDesc();

    List<WorkflowInstance> findByIdIn(Collection<Long> ids);
}