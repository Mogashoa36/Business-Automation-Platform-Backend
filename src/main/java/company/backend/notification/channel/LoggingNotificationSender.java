package company.backend.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import company.backend.notification.entity.Notification;
import company.backend.notification.entity.NotificationChannel;
import company.backend.user.entity.User;

/**
 * Placeholder for an external delivery channel.
 *
 * <p>Records the intent in the application log instead of contacting a provider.
 * That is deliberate: the template must run without an SMTP server, an SMS gateway
 * or a push service, and it must never quietly send real messages to real people.
 * A client project replaces this bean with a real implementation for the same
 * {@link NotificationChannel} and the dispatcher picks it up automatically.
 *
 * <p>Not a Spring bean on its own - {@link NotificationChannelConfig} registers one
 * instance per external channel.
 */
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    private final NotificationChannel channel;

    public LoggingNotificationSender(NotificationChannel channel) {
        this.channel = channel;
    }

    @Override
    public NotificationChannel channel() {
        return channel;
    }

    @Override
    public boolean send(User recipient, Notification notification) {
        log.info(
                "[notification:{}] to={} subject='{}' body='{}'",
                channel,
                recipient.getEmail(),
                notification.getTitle(),
                notification.getMessage());
        // Reported as undelivered so callers can tell "logged" apart from "sent".
        return false;
    }
}