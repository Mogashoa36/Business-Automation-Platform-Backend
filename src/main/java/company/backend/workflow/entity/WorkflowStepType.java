package company.backend.workflow.entity;

/**
 * The kind of work a workflow step asks for.
 *
 * <p>{@code TASK} and {@code APPROVAL} are the two the template implements out of
 * the box; {@code NOTIFICATION} and {@code SCRIPT} are extension points a client
 * project can specialise.
 */
public enum WorkflowStepType {
    APPROVAL,
    TASK,
    NOTIFICATION,
    SCRIPT
}