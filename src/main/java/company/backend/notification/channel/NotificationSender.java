package company.backend.notification.channel;

import company.backend.notification.entity.Notification;
import company.backend.notification.entity.NotificationChannel;
import company.backend.user.entity.User;

/**
 * A delivery channel implementation.
 *
 * <p>The template ships in-app delivery plus logging stand-ins for e-mail, SMS and
 * push. A client project replaces a stand-in with a real provider by providing its
 * own bean; nothing else in the platform has to change.
 */
public interface NotificationSender {

    NotificationChannel channel();

    /**
     * Delivers the notification.
     *
     * @return true when handed off successfully; false when the channel is
     *         unavailable or the recipient has no address for it
     */
    boolean send(User recipient, Notification notification);
}