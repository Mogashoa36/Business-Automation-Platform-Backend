package company.backend.report.dto;

import java.util.List;
import java.util.Map;

/** Result of a report run: a title, column definitions and rows. */
public record ReportResponse(
        String key,
        String title,
        String description,
        List<Column> columns,
        List<Map<String, Object>> rows,
        long rowCount) {

    /**
     * Column definition.
     *
     * @param type one of {@code text}, {@code number}, {@code date}, {@code status}
     */
    public record Column(String key, String label, String type) {
    }
}