package company.backend.approval.entity;

import java.time.Instant;

import company.backend.role.entity.Role;
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
import lombok.Getter;
import lombok.Setter;

/**
 * One stage of a multi-stage approval.
 *
 * <p>Sequential by design: stage N is only opened once stages before it are
 * approved, which is what makes a chain such as manager then finance meaningful
 * without hard-coding those roles anywhere.
 */
@Entity
@Table(name = "approval_steps")
@Getter
@Setter
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_id", nullable = false)
    private ApprovalRequest approval;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_role_id")
    private Role approverRole;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by_id")
    private Long decidedById;

    public boolean isPending() {
        return status == ApprovalStepStatus.PENDING || status == ApprovalStepStatus.IN_REVIEW;
    }
}