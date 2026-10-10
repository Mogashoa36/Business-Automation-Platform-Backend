package company.backend.workflow.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import company.backend.common.BaseEntity;
import company.backend.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/** A single run of a {@link WorkflowDefinition}. */
@Entity
@Table(name = "workflow_instances")
@Getter
@Setter
public class WorkflowInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", unique = true, length = 32)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private WorkflowDefinition definition;

    /** Definition version this instance was started from. */
    @Column(name = "definition_version", nullable = false)
    private int definitionVersion;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private User currentAssignee;

    /** Index of the step currently awaiting action; -1 once the process is finished. */
    @Column(name = "current_step_index", nullable = false)
    private int currentStepIndex = -1;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Opaque payload for client specific data. A copied project can extend the
     * workflow without touching the base schema.
     */
    @Column(name = "payload", length = 4000)
    private String payload;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<WorkflowInstanceStep> steps = new ArrayList<>();

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    public void addStep(WorkflowInstanceStep step) {
        step.setInstance(this);
        this.steps.add(step);
    }
}