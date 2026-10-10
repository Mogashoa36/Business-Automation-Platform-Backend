package company.backend.comment.dto;

import java.time.Instant;

/** Read model for a comment. */
public record CommentResponse(
        Long id,
        String entityType,
        Long entityId,
        String body,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        boolean internal,
        Instant createdAt) {
}