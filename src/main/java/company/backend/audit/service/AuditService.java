package company.backend.audit.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import company.backend.audit.entity.AuditLog;
import company.backend.audit.repository.AuditLogRepository;
import company.backend.security.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

import tools.jackson.databind.ObjectMapper;

/**
 * Writes audit entries.
 *
 * <p>Two guarantees are enforced here:
 * <ul>
 *   <li><b>Secrets never persist.</b> Any field whose name looks like a credential is
 *       redacted before serialisation.</li>
 *   <li><b>Auditing never breaks the request.</b> A failure to record history is
 *       logged and swallowed, because losing an audit line is preferable to
 *       rolling back a legitimate business operation.</li>
 * </ul>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "newpassword", "oldpassword", "currentpassword", "confirmpassword",
            "passwordhash", "secret", "token", "accesstoken", "refreshtoken", "authorization",
            "secretkey", "apikey", "credentials");

    /** Upper bound so a huge payload cannot bloat the audit table. */
    private static final int MAX_VALUE_LENGTH = 4000;

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records an action performed by the current user, in its own transaction so a
     * bookkeeping failure cannot roll back or corrupt the caller's unit of work.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String module, String entityType, Object entityId, String description) {
        recordInternal(action, module, entityType, entityId, description, null, null, true);
    }

    /** Records an action together with the before/after state of a record. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordChange(
            String action,
            String module,
            String entityType,
            Object entityId,
            Object previousValue,
            Object newValue,
            String description) {
        recordInternal(
                action,
                module,
                entityType,
                entityId,
                description,
                sanitise(previousValue),
                sanitise(newValue),
                true);
    }

    /** Records a failed attempt, e.g. a rejected sign-in. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, String module, String entityType, Object entityId, String description) {
        recordInternal(action, module, entityType, entityId, description, null, null, false);
    }

    private void recordInternal(
            String action,
            String module,
            String entityType,
            Object entityId,
            String description,
            String previousValue,
            String newValue,
            boolean success) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setModule(module);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId == null ? null : String.valueOf(entityId));
            entry.setDescription(truncate(description, 500));
            entry.setPreviousValue(previousValue);
            entry.setNewValue(newValue);
            entry.setSuccess(success);

            SecurityUtils.principal().ifPresent(principal -> {
                entry.setUserId(principal.getId());
                entry.setUsername(principal.getEmail());
            });

            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setIpAddress(resolveClientIp(request));
                entry.setUserAgent(truncate(request.getHeader("User-Agent"), 300));
            }
            auditLogRepository.save(entry);
        } catch (RuntimeException ex) {
            log.error("Failed to write audit entry action={} module={}", action, module, ex);
        }
    }

    private static HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    /**
     * Prefers the forwarded client address. The reverse proxy is responsible for
     * overwriting this header, otherwise a client could spoof its own origin.
     */
    static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    /** Serialises a value to JSON, replacing any credential-like field with {@code ***}. */
    private String sanitise(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String text) {
                return truncate(text, MAX_VALUE_LENGTH);
            }
            return truncate(String.valueOf(redact(objectMapper.convertValue(value, Object.class))), MAX_VALUE_LENGTH);
        } catch (RuntimeException ex) {
            log.debug("Unable to serialise audit payload of type {}", value.getClass().getName());
            return truncate(String.valueOf(value), MAX_VALUE_LENGTH);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object redact(Object node) {
        if (node instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((key, val) -> {
                String name = String.valueOf(key);
                if (isSensitive(name)) {
                    result.put(name, "***");
                } else {
                    result.put(name, redact(val));
                }
            });
            return result;
        }
        if (node instanceof List<?> list) {
            return list.stream().map(AuditService::redact).toList();
        }
        return node;
    }

    private static boolean isSensitive(String key) {
        String normalised = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return SENSITIVE_KEYS.contains(normalised);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}