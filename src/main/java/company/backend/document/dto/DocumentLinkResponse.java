package company.backend.document.dto;

import java.time.Instant;

/** Read model for the attachment between a document and a business record. */
public record DocumentLinkResponse(
        Long linkId,
        Long documentId,
        String filename,
        long sizeBytes,
        String contentType,
        String entityType,
        Long entityId,
        Long linkedById,
        Instant linkedAt) {
}