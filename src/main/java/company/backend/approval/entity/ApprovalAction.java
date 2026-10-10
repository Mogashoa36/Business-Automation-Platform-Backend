package company.backend.approval.entity;

import java.time.Instant;

import company.backend.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Append-only record of a decision on an approval step.
 *
 * <p>Kept separate from the step itself so the full decision history survives even
 * when several people act on the same stage.
 */
@Entity
@Table(name = "approval_actions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ApprovalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_id", nullable = false)
    private ApprovalRequest approval;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    private ApprovalStep step;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Plain string: {@code SUBMITTED}, {@code APPROVED}, {@code REJECTED}, {@code CANCELLED}. */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "comment", length = 2000)
    private String comment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}