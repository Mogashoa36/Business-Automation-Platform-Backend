package company.backend.task.entity;

/** Task urgency. Ordered from least to most urgent so it can be sorted meaningfully. */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}