package company.backend.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.notification.entity.NotificationPreference;

/** Data access for {@link NotificationPreference}. */
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, String notificationType);
}