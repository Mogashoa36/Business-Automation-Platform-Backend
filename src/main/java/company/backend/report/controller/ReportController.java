package company.backend.report.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.report.dto.ReportDefinition;
import company.backend.report.dto.ReportResponse;
import company.backend.report.service.ReportService;
import company.backend.security.Permissions;
import company.backend.security.SecurityUtils;

/** Report catalog, execution and CSV export. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Only the reports the caller is actually allowed to see. */
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_VIEW + "')")
    public ApiResponse<List<ReportDefinition>> catalog() {
        List<String> permissions = SecurityUtils.principal()
                .map(principal -> principal.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList())
                .orElse(List.of());
        return ApiResponse.ok(reportService.catalog(permissions));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_VIEW + "')")
    public ApiResponse<ReportResponse> run(
            @PathVariable String key,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId) {
        return ApiResponse.ok(reportService.run(key, from, to, departmentId));
    }

    /** Streams the report as CSV. */
    @GetMapping("/{key}/export")
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_EXPORT + "')")
    public ResponseEntity<byte[]> export(
            @PathVariable String key,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId) {

        ReportResponse report = reportService.run(key, from, to, departmentId);
        byte[] body = reportService.toCsv(report).getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + ".csv\"")
                .body(body);
    }
}