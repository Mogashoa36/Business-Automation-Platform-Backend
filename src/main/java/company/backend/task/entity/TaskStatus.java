package company.backend.task.entity;

/**
 * Task lifecycle.
 *
 * <p>{@code ON_HOLD} and {@code CANCELLED} are terminal-by-choice rather than by
 * completion, which keeps "done" unambiguous for reporting.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    /** True when no further work is expected, whatever the reason. */
    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED;
    }
}