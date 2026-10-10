package company.backend.common;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds safe {@link Pageable} instances for every list endpoint.
 *
 * <p>Centralising this gives three guarantees the controllers would otherwise each
 * re-implement: page size is always bounded (no accidental full-table dumps),
 * negative input is corrected rather than throwing, and client supplied sort
 * properties are checked against an allow list so a crafted request cannot ask the
 * database to order by - or leak - an arbitrary column.
 */
public final class PageableFactory {

    public static final int MAX_PAGE_SIZE = 200;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private PageableFactory() {
    }

    /**
     * @param sortBy      property requested by the client; falls back to {@code defaultSort}
     * @param direction   {@code asc} or {@code desc}; anything else defaults to ascending
     * @param allowedSort properties the client is permitted to sort by
     */
    public static Pageable create(
            int page, int size, String sortBy, String direction, Set<String> allowedSort, String defaultSort) {

        String property = (sortBy == null || sortBy.isBlank()) ? defaultSort : sortBy;
        if (property == null || !allowedSort.contains(property)) {
            property = defaultSort;
        }
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(sortDirection, property));
    }
}