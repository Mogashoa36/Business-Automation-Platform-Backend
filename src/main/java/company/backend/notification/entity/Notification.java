package company.backend.notification.entity;

import java.time.Instant;

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

/** An in-app notification addressed to a single user. */
@Entity
@Table(
        name = "notifications",
        indexes = {
            @Index(name = "idx_notifications_user_unread", columnList = "user_id,read"),
            @Index(name = "idx_notifications_created", columnList = "created_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Stored as a string so client specific types do not require a code change. */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    /** Relative or absolute path the UI can navigate to, e.g. {@code /tasks/42}. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void markRead() {
        this.read = true;
        this.readAt = Instant.now();
    }
}