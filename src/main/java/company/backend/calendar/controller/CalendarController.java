package company.backend.calendar.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import company.backend.calendar.dto.CalendarEntry;
import company.backend.calendar.service.CalendarService;
import company.backend.common.ApiResponse;
import company.backend.security.Permissions;

/** Read-only calendar feed combining task due dates and workflow milestones. */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    /** Refuse absurd windows rather than letting one request scan the whole history. */
    private static final int MAX_RANGE_DAYS = 366;

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.TASK_VIEW + "')")
    public ApiResponse<List<CalendarEntry>> findBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (to.isBefore(from)) {
            return ApiResponse.ok(List.of());
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new company.backend.exception.BusinessException(
                    "The requested range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        return ApiResponse.ok(calendarService.findBetween(from, to));
    }
}