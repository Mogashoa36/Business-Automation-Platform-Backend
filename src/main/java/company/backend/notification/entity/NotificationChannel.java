package company.backend.notification.entity;

/** Delivery channels. Only IN_APP is wired up in the template; the rest are a contract. */
public enum NotificationChannel {
    IN_APP,
    EMAIL,
    SMS,
    PUSH
}