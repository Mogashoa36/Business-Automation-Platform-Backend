package company.backend.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Error counterpart of {@code ApiResponse}.
 *
 * @param fieldErrors optional bean-validation detail, keyed by field name
 */
public record ApiError(
        boolean success,
        String message,
        List<Map<String, String>> fieldErrors,
        Instant timestamp) {

    public static ApiError of(String message) {
        return new ApiError(false, message, List.of(), Instant.now());
    }

    public static ApiError of(String message, List<Map<String, String>> fieldErrors) {
        return new ApiError(false, message, fieldErrors == null ? List.of() : fieldErrors, Instant.now());
    }
}