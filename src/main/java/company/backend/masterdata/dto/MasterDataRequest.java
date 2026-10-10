package company.backend.masterdata.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating a master data item. */
public record MasterDataRequest(
        @NotBlank @Size(max = 50) String group,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String label,
        @Size(max = 500) String description,
        Integer sortOrder,
        Boolean active) {

    public int sortOrderOrDefault() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public boolean isActiveOrDefault() {
        return active == null || active;
    }
}