package company.backend.workflow.entity;

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
 * A step of a running {@link WorkflowInstance}.
 *
 * <p>Copied from the definition at start time so the record of what actually
 * happened survives later edits to the template.
 */
@Entity
@Table(name = "workflow_instance_steps")
@Getter
@Setter
public class WorkflowInstanceStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false)
    private WorkflowInstance instance;

    @Column(name = "step_name", nullable = false, length = 150)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 20)
    private WorkflowStepType stepType = WorkflowStepType.APPROVAL;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStepStatus status = WorkflowStepStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_role_id")
    private Role assigneeRole;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public boolean isPending() {
        return status == WorkflowStepStatus.PENDING || status == WorkflowStepStatus.IN_PROGRESS;
    }
}