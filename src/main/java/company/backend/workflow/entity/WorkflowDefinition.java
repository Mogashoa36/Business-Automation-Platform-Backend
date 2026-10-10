package company.backend.workflow.entity;

import java.util.ArrayList;
import java.util.List;

import company.backend.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A reusable process template.
 *
 * <p>A definition is inert data: it holds an ordered list of steps and is never
 * executed itself. Starting a process copies the steps into a
 * {@link WorkflowInstance}, which is what makes historical instances immune to
 * later edits of the template.
 */
@Entity
@Table(name = "workflow_definitions")
@Getter
@Setter
public class WorkflowDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** A running instance must keep using the version it started with. */
    @Column(name = "version", nullable = false)
    private int version = 1;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<WorkflowStepDefinition> steps = new ArrayList<>();

    public void addStep(WorkflowStepDefinition step) {
        step.setDefinition(this);
        this.steps.add(step);
    }
}