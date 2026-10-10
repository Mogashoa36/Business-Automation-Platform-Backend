package company.backend.approval.repository;

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

import company.backend.approval.entity.ApprovalRequest;
import company.backend.approval.entity.ApprovalStatus;

/** Data access for {@link ApprovalRequest}. */
public interface ApprovalRequestRepository
        extends JpaRepository<ApprovalRequest, Long>, JpaSpecificationExecutor<ApprovalRequest> {

    @EntityGraph(attributePaths = {"requester", "steps", "steps.approver"})
    Optional<ApprovalRequest> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"requester", "steps", "steps.approver"})
    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "steps", "steps.approver"})
    Page<ApprovalRequest> findByRequesterId(Long requesterId, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "steps", "steps.approver"})
    List<ApprovalRequest> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(ApprovalStatus status);

    @Query("select count(a) from ApprovalRequest a where a.requester.id = :userId and a.status not in "
            + "(company.backend.approval.entity.ApprovalStatus.APPROVED, "
            + "company.backend.approval.entity.ApprovalStatus.REJECTED, "
            + "company.backend.approval.entity.ApprovalStatus.CANCELLED, "
            + "company.backend.approval.entity.ApprovalStatus.DRAFT)")
    long countOpenByRequester(@Param("userId") Long userId);

    @Query("select count(a) from ApprovalRequest a where a.status not in "
            + "(company.backend.approval.entity.ApprovalStatus.APPROVED, "
            + "company.backend.approval.entity.ApprovalStatus.REJECTED, "
            + "company.backend.approval.entity.ApprovalStatus.CANCELLED, "
            + "company.backend.approval.entity.ApprovalStatus.DRAFT)")
    long countOpen();

    /** Steps currently awaiting a decision from a specific person. */
    @Query("select s from company.backend.approval.entity.ApprovalStep s "
            + "where s.approver.id = :userId and s.status in "
            + "(company.backend.approval.entity.ApprovalStepStatus.PENDING, "
            + "company.backend.approval.entity.ApprovalStepStatus.IN_REVIEW) "
            + "order by s.decidedAt asc")
    List<company.backend.approval.entity.ApprovalStep> findPendingStepsForUser(@Param("userId") Long userId);

    List<ApprovalRequest> findByIdIn(Collection<Long> ids);
}