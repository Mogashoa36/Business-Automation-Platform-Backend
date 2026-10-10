package company.backend.notification.dto;

/**
 * Per-type delivery preferences.
 *
 * <p>Absence of an entry means "use the defaults": in-app on, external channels
 * off. This keeps the table small instead of storing a row per user per type.
 */
public record NotificationPreferenceResponse(
        String notificationType,
        boolean inAppEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        boolean pushEnabled) {
}