package company.backend.audit.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Immutable record of a significant action.
 *
 * <p>Deliberately does not extend {@code BaseEntity}: an audit row is append-only
 * and must never carry a modification timestamp that implies it can be edited.
 * Passwords, tokens and other secrets are never written here - see
 * {@code AuditService}, which sanitises payloads before persisting them.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
            @Index(name = "idx_audit_logs_user", columnList = "user_id"),
            @Index(name = "idx_audit_logs_entity", columnList = "entity_type,entity_id"),
            @Index(name = "idx_audit_logs_created", columnList = "created_at"),
            @Index(name = "idx_audit_logs_module_action", columnList = "module,action")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    /** Denormalised so the trail stays readable after a user is renamed or removed. */
    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "previous_value", length = 4000)
    private String previousValue;

    @Column(name = "new_value", length = 4000)
    private String newValue;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "success", nullable = false)
    private boolean success = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}