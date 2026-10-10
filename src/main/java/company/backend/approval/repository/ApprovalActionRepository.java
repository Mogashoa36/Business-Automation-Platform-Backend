package company.backend.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.approval.entity.ApprovalAction;

/** Data access for {@link ApprovalAction}. */
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {

    List<ApprovalAction> findByApprovalIdOrderByCreatedAtAsc(Long approvalId);
}