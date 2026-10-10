package company.backend.notification.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.notification.channel.NotificationSender;
import company.backend.notification.dto.NotificationPreferenceResponse;
import company.backend.notification.dto.NotificationResponse;
import company.backend.notification.entity.Notification;
import company.backend.notification.entity.NotificationChannel;
import company.backend.notification.entity.NotificationPreference;
import company.backend.notification.repository.NotificationPreferenceRepository;
import company.backend.notification.repository.NotificationRepository;
import company.backend.security.SecurityUtils;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

/** In-app notifications and per-user delivery preferences. */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            List<NotificationSender> senders) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        senders.forEach(sender -> this.senders.put(sender.channel(), sender));
    }

    /**
     * Creates an in-app notification and attempts the external channels.
     *
     * <p>Runs in its own transaction so a notification is not lost because the
     * business operation that triggered it rolled back - the recipient still needs
     * to know that something happened.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(Long userId, String type, String title, String message, String linkUrl) {
        if (userId == null) {
            return;
        }
        User recipient = userRepository.findById(userId).orElse(null);
        if (recipient == null || !recipient.isActive()) {
            log.debug("Skipping notification for unknown or inactive user {}", userId);
            return;
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkUrl(linkUrl);
        notificationRepository.save(notification);

        dispatchExternal(recipient, notification);
    }

    /** Honours the recipient's preferences, ignoring any channel that fails. */
    private void dispatchExternal(User recipient, Notification notification) {
        NotificationPreference preference =
                preferenceRepository.findByUserIdAndNotificationType(recipient.getId(), notification.getType())
                        .orElse(null);

        for (NotificationSender sender : senders.values()) {
            if (!isChannelEnabled(preference, sender.channel())) {
                continue;
            }
            try {
                sender.send(recipient, notification);
            } catch (RuntimeException ex) {
                // A broken channel must never fail the notification itself.
                log.warn("Notification channel {} failed", sender.channel(), ex);
            }
        }
    }

    private static boolean isChannelEnabled(NotificationPreference preference, NotificationChannel channel) {
        if (preference == null) {
            // Secure default: external channels are opt-in.
            return false;
        }
        return switch (channel) {
            case IN_APP -> preference.isInAppEnabled();
            case EMAIL -> preference.isEmailEnabled();
            case SMS -> preference.isSmsEnabled();
            case PUSH -> preference.isPushEnabled();
        };
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findForCurrentUser(Boolean unreadOnly, Pageable pageable) {
        Long userId = requireCurrentUserId();
        Page<Notification> page = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findRecentUnread() {
        Long userId = requireCurrentUserId();
        return notificationRepository.findTop5ByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return notificationRepository.countByUserIdAndReadFalse(requireCurrentUserId());
    }

    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return notificationRepository.countByUserIdAndCreatedAtAfter(requireCurrentUserId(), since);
    }

    public NotificationResponse markRead(Long id) {
        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        assertOwnedByCurrentUser(notification);
        if (!notification.isRead()) {
            notification.markRead();
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    public long markAllRead() {
        return notificationRepository.markAllRead(requireCurrentUserId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> findPreferences() {
        return preferenceRepository.findByUserId(requireCurrentUserId()).stream()
                .map(preference -> new NotificationPreferenceResponse(
                        preference.getNotificationType(),
                        preference.isInAppEnabled(),
                        preference.isEmailEnabled(),
                        preference.isSmsEnabled(),
                        preference.isPushEnabled()))
                .toList();
    }

    public NotificationPreferenceResponse updatePreference(
            String type, boolean inApp, boolean email, boolean sms, boolean push) {
        Long userId = requireCurrentUserId();
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(userId, type)
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setNotificationType(type);
                    return created;
                });
        preference.setInAppEnabled(inApp);
        preference.setEmailEnabled(email);
        preference.setSmsEnabled(sms);
        preference.setPushEnabled(push);
        preferenceRepository.save(preference);

        return new NotificationPreferenceResponse(type, inApp, email, sms, push);
    }

    private void assertOwnedByCurrentUser(Notification notification) {
        Long userId = requireCurrentUserId();
        if (notification.getUser() == null || !userId.equals(notification.getUser().getId())) {
            // Reported as missing rather than forbidden, so the endpoint cannot be
            // used to probe for the existence of other people's notifications.
            throw ResourceNotFoundException.of("Notification", notification.getId());
        }
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("No authenticated user");
        }
        return userId;
    }

    private static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLinkUrl(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}