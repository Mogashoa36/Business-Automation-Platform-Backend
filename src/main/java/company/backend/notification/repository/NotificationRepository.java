package company.backend.notification.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import company.backend.notification.entity.Notification;

/** Data access for {@link Notification}. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    List<Notification> findTop5ByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId, @Param("now") java.time.Instant now);

    long countByUserIdAndCreatedAtAfter(Long userId, java.time.Instant since);
}