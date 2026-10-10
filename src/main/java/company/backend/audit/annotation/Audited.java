package company.backend.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as auditable.
 *
 * <p>Applied to the service layer (not controllers) so the trail records real
 * state changes rather than the number of times an endpoint was called. The entry
 * is written inside the same transaction as the business change, so an audit row can
 * never exist for work that was rolled back.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Logical module name, e.g. {@code user}, {@code workflow}. */
    String module();

    /** One of {@link company.backend.audit.AuditAction}. */
    String action();

    /** Entity class name; defaults to the simple name of the return type. */
    String entityType() default "";
}