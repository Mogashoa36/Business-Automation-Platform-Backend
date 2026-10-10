package company.backend.notification.channel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import company.backend.notification.entity.NotificationChannel;

/**
 * Registers the placeholder senders for the external channels.
 *
 * <p>Setting {@code platform.notification.placeholder-senders=false} removes all
 * three, which is what a client project does once it supplies real
 * {@link NotificationSender} beans for those channels - otherwise its providers
 * would compete with the stand-ins.
 */
@Configuration
@ConditionalOnProperty(
        name = "platform.notification.placeholder-senders",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationChannelConfig {

    @Bean
    public NotificationSender loggingEmailSender() {
        return new LoggingNotificationSender(NotificationChannel.EMAIL);
    }

    @Bean
    public NotificationSender loggingSmsSender() {
        return new LoggingNotificationSender(NotificationChannel.SMS);
    }

    @Bean
    public NotificationSender loggingPushSender() {
        return new LoggingNotificationSender(NotificationChannel.PUSH);
    }
}