package company.backend.document.dto;

import java.time.Instant;

/** Read model for a document. Never exposes the storage key. */
public record DocumentResponse(
        Long id,
        String filename,
        String contentType,
        long sizeBytes,
        String category,
        String description,
        Long uploadedById,
        String uploadedByName,
        int version,
        boolean active,
        Instant createdAt) {
}