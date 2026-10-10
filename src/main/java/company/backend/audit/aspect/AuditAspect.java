package company.backend.audit.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import company.backend.audit.annotation.Audited;
import company.backend.audit.service.AuditService;

/**
 * Turns an {@link Audited} annotation into a persisted audit entry.
 *
 * <p>Only successful returns are recorded: a rejected or rolled back operation
 * leaves no trace here, which is the behaviour an auditor expects. Exceptions are
 * logged separately by the error handler.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void recordSuccess(JoinPoint joinPoint, Audited audited, Object result) {
        auditService.record(
                audited.action(),
                audited.module(),
                resolveEntityType(audited, result),
                resolveEntityId(result, joinPoint.getArgs()),
                describe(joinPoint, audited));
    }

    private static String resolveEntityType(Audited audited, Object result) {
        if (!audited.entityType().isBlank()) {
            return audited.entityType();
        }
        if (result == null) {
            return null;
        }
        String name = result.getClass().getSimpleName();
        // Strip the "Response" suffix that DTOs carry so the trail reads naturally.
        return name.endsWith("Response") ? name.substring(0, name.length() - "Response".length()) : name;
    }

    /** Prefers the identifier on the returned entity, falling back to the arguments. */
    private static Object resolveEntityId(Object result, Object[] args) {
        Object fromResult = readId(result);
        if (fromResult != null) {
            return fromResult;
        }
        return Arrays.stream(args == null ? new Object[0] : args).map(AuditAspect::readId).filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static Object readId(Object candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate instanceof Number number) {
            return number;
        }
        if (candidate instanceof Long || candidate instanceof String) {
            return candidate;
        }
        try {
            var method = candidate.getClass().getMethod("getId");
            return method.invoke(candidate);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static String describe(JoinPoint joinPoint, Audited audited) {
        return audited.action() + " " + audited.module() + " via " + joinPoint.getSignature().getName();
    }
}