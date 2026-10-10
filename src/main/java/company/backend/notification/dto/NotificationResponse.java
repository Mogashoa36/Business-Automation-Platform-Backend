package company.backend.notification.dto;

import java.time.Instant;

/** Read model for a notification. */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String linkUrl,
        boolean read,
        Instant readAt,
        Instant createdAt) {
}