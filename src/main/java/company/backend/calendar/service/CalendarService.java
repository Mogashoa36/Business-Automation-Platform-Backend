package company.backend.calendar.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.calendar.dto.CalendarEntry;
import company.backend.task.entity.Task;
import company.backend.task.entity.TaskStatus;
import company.backend.task.repository.TaskRepository;
import company.backend.workflow.entity.WorkflowInstance;
import company.backend.workflow.repository.WorkflowInstanceRepository;

/**
 * Calendar foundation.
 *
 * <p>Rather than duplicating task and workflow data into a calendar table, this
 * projects existing due dates into a single feed. A client project that needs
 * real events (meetings, reminders) adds a {@code calendar_events} entity and
 * merges it in the same way.
 */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    private static final Set<TaskStatus> HIDDEN_STATUSES = Set.of(TaskStatus.CANCELLED);

    private final TaskRepository taskRepository;
    private final WorkflowInstanceRepository workflowRepository;

    public CalendarService(TaskRepository taskRepository, WorkflowInstanceRepository workflowRepository) {
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
    }

    /** Every dated item in the window, ordered by date. */
    public List<CalendarEntry> findBetween(LocalDate from, LocalDate to) {
        List<CalendarEntry> entries = new ArrayList<>(taskEntries(from, to));
        entries.addAll(workflowEntries(from, to));
        entries.sort(java.util.Comparator.comparing(CalendarEntry::date));
        return entries;
    }

    private List<CalendarEntry> taskEntries(LocalDate from, LocalDate to) {
        return taskRepository.findByDueDateBetweenAndStatusNotIn(from, to, HIDDEN_STATUSES).stream()
                .map(this::toTaskEntry)
                .toList();
    }

    /** Workflows appear on the day they were submitted or, if still running, today. */
    private List<CalendarEntry> workflowEntries(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return workflowRepository.findAll().stream()
                .filter(instance -> !instance.getStatus().isTerminal())
                .filter(instance -> inWindow(instance, from, to, today))
                .map(instance -> toWorkflowEntry(instance, today))
                .toList();
    }

    private static boolean inWindow(WorkflowInstance instance, LocalDate from, LocalDate to, LocalDate today) {
        LocalDate date = instance.getSubmittedAt() == null
                ? today
                : instance.getSubmittedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private CalendarEntry toTaskEntry(Task task) {
        return new CalendarEntry(
                task.getId(),
                "TASK",
                task.getReference(),
                task.getTitle(),
                task.getStatus().name(),
                task.getDueDate(),
                "/tasks/" + task.getId());
    }

    private CalendarEntry toWorkflowEntry(WorkflowInstance instance, LocalDate today) {
        LocalDate date = instance.getSubmittedAt() == null
                ? today
                : instance.getSubmittedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return new CalendarEntry(
                instance.getId(),
                "WORKFLOW",
                instance.getReference(),
                instance.getTitle(),
                instance.getStatus().name(),
                date,
                "/workflows/" + instance.getId());
    }
}