package company.backend.comment.entity;

import java.time.Instant;

import company.backend.common.BaseEntity;
import company.backend.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A note attached to any business record.
 *
 * <p>Comments are linked by {@code (entityType, entityId)} rather than by a foreign
 * key per target. That is what lets the same table serve tasks, workflows,
 * approvals, documents and any entity a client project adds later, without a
 * migration per target type. The trade-off is that the database cannot enforce the
 * reference, so {@code CommentService} validates the target before writing.
 */
@Entity
@Table(
        name = "comments",
        indexes = {
            @Index(name = "idx_comments_target", columnList = "entity_type,entity_id"),
            @Index(name = "idx_comments_author", columnList = "author_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Logical target type, e.g. {@code TASK}, {@code WORKFLOW_INSTANCE}, {@code APPROVAL}. */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Internal notes are visible to staff with the matching permission but hidden
     * from the person the record is about.
     */
    @Column(name = "internal", nullable = false)
    private boolean internal = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}