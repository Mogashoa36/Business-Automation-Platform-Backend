package company.backend.comment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.comment.dto.CommentResponse;
import company.backend.comment.dto.CreateCommentRequest;
import company.backend.comment.service.CommentService;
import company.backend.common.ApiResponse;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/**
 * Comments for any supported business record.
 *
 * <p>The target is expressed in the path rather than the body so that a comment
 * collection has a stable, cacheable, linkable URL:
 * {@code GET /api/comments/TASK/42}.
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('" + Permissions.COMMENT_VIEW + "')")
    public ApiResponse<List<CommentResponse>> findFor(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ApiResponse.ok(commentService.findFor(entityType, entityId));
    }

    @PostMapping("/{entityType}/{entityId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.COMMENT_CREATE + "')")
    public ApiResponse<CommentResponse> add(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.created("Comment added", commentService.add(entityType, entityId, request));
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.COMMENT_DELETE + "')")
    public void delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
    }
}