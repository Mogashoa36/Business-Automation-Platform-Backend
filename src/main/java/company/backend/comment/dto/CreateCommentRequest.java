package company.backend.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for adding a comment. */
public record CreateCommentRequest(
        @NotBlank @Size(max = 4000) String body,
        Boolean internal) {

    public boolean isInternal() {
        return Boolean.TRUE.equals(internal);
    }
}