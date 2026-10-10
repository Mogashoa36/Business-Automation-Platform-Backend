package company.backend.masterdata.dto;

import java.time.Instant;

/** Read model for a master data item. */
public record MasterDataResponse(
        Long id,
        String group,
        String code,
        String label,
        String description,
        int sortOrder,
        boolean active,
        boolean systemManaged,
        Instant updatedAt) {
}