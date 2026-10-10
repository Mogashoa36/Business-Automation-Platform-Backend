package company.backend.workflow.entity;

import company.backend.common.BaseEntity;
import company.backend.role.entity.Role;

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
 * One ordered step of a {@link WorkflowDefinition}.
 *
 * <p>The assignee is resolved from a role rather than a fixed user, so a
 * "manager approves" step keeps working when the person in that role changes.
 */
@Entity
@Table(name = "workflow_step_definitions")
@Getter
@Setter
public class WorkflowStepDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private WorkflowDefinition definition;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 20)
    private WorkflowStepType stepType = WorkflowStepType.APPROVAL;

    /** Zero based position; steps run in ascending order. */
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_role_id")
    private Role assigneeRole;

    @Column(name = "allow_rejection", nullable = false)
    private boolean allowRejection = true;

    @Column(name = "instructions", length = 1000)
    private String instructions;
}