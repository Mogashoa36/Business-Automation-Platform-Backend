package company.backend.approval.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import company.backend.common.BaseEntity;
import company.backend.user.entity.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * A request for one or more people to sign off.
 *
 * <p>Approval is modelled separately from workflows because the two answer
 * different questions: a workflow moves a record through a process, an approval
 * asks a decision and records who made it. A workflow step of type
 * {@code APPROVAL} creates one of these, but a client project can also raise an
 * approval on its own.
 *
 * <p>{@code amount} and {@code currency} are optional and generic; they exist so
 * that value-bearing approvals can be displayed and filtered without a schema
 * change per client.
 */
@Entity
@Table(name = "approvals")
@Getter
@Setter
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", unique = true, length = 32)
    private String reference;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    /** Logical type such as {@code EXPENSE}, {@code PURCHASE}, {@code LEAVE}. */
    @Column(name = "category", length = 50)
    private String category;

    /** Optional link back to the record that triggered the approval. */
    @Column(name = "source_entity_type", length = 50)
    private String sourceEntityType;

    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "current_step_index", nullable = false)
    private int currentStepIndex = -1;

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    public void addStep(ApprovalStep step) {
        step.setApproval(this);
        this.steps.add(step);
    }
}