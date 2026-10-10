package company.backend.report.dto;

/** Catalog entry describing an available report. */
public record ReportDefinition(
        String key,
        String title,
        String description,
        String requiredPermission,
        boolean supportsDateRange,
        boolean supportsExport) {
}