package company.backend.calendar.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A single day in the calendar.
 *
 * <p>Task due dates and workflow milestones are merged into one feed so the UI
 * renders a single unified calendar without joining the two itself.
 */
public record CalendarEntry(
        Long id,
        /** {@code TASK} or {@code WORKFLOW}. */
        String type,
        String reference,
        String title,
        String status,
        LocalDate date,
        String linkUrl) {
}