package company.backend.comment.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.comment.dto.CommentResponse;
import company.backend.comment.dto.CreateCommentRequest;
import company.backend.comment.entity.Comment;
import company.backend.comment.repository.CommentRepository;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.security.Permissions;
import company.backend.security.PlatformUserPrincipal;
import company.backend.security.SecurityUtils;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

/**
 * Comments attached to any business record.
 *
 * <p>The {@code (entityType, entityId)} pair is validated against an allow list
 * before anything is written. Without that check the column would happily accept
 * a typo, and the comment would silently disappear into a void.
 */
@Service
@Transactional
public class CommentService {

    /**
     * Target types the base platform knows how to resolve. A client project adds
     * its own entities here when it starts attaching comments to them.
     */
    private static final Set<String> SUPPORTED_TARGETS = Set.of(
            "TASK", "WORKFLOW_INSTANCE", "APPROVAL", "DOCUMENT");

    private static final Pattern VALID_TYPE = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findFor(String entityType, Long entityId) {
        validateTargetType(entityType);
        return commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId).stream()
                .map(CommentService::toResponse)
                .filter(comment -> !comment.internal() || canSeeInternal())
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> findFor(String entityType, Long entityId, Pageable pageable) {
        validateTargetType(entityType);
        return commentRepository
                .findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(CommentService::toResponse);
    }

    @Audited(module = "comment", action = AuditAction.CREATE, entityType = "Comment")
    public CommentResponse add(String entityType, Long entityId, CreateCommentRequest request) {
        validateTargetType(entityType);
        if (request.isInternal() && !canSeeInternal()) {
            throw new BusinessException("You are not allowed to write internal notes");
        }

        Comment comment = new Comment();
        comment.setEntityType(entityType);
        comment.setEntityId(entityId);
        comment.setBody(request.body().trim());
        comment.setInternal(request.isInternal());
        comment.setAuthor(requireCurrentUser());

        return toResponse(commentRepository.save(comment));
    }

    /** Only the author may remove a comment, so notes stay attributable. */
    @Audited(module = "comment", action = AuditAction.DELETE, entityType = "Comment")
    public void delete(Long commentId) {
        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException("Only the author can delete a comment");
        }
        commentRepository.delete(comment);
    }

    private static void validateTargetType(String entityType) {
        if (entityType == null || !VALID_TYPE.matcher(entityType.toUpperCase(Locale.ROOT)).matches()) {
            throw new BusinessException("Invalid comment target type");
        }
        if (!SUPPORTED_TARGETS.contains(entityType.toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Comments cannot be attached to '" + entityType + "'");
        }
    }

    private static boolean canSeeInternal() {
        return SecurityUtils.principal()
                .map(PlatformUserPrincipal::getAuthorities)
                .map(authorities -> authorities.stream()
                        .anyMatch(authority -> Permissions.COMMENT_VIEW.equals(authority.getAuthority())))
                .orElse(false);
    }

    private User requireCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("No authenticated user");
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("The current user no longer exists"));
    }

    private static CommentResponse toResponse(Comment comment) {
        User author = comment.getAuthor();
        return new CommentResponse(
                comment.getId(),
                comment.getEntityType(),
                comment.getEntityId(),
                comment.getBody(),
                author == null ? null : author.getId(),
                author == null ? null : author.getFullName(),
                author == null ? null : author.getAvatarUrl(),
                comment.isInternal(),
                comment.getCreatedAt());
    }
}