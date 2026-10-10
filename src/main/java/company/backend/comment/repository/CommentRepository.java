package company.backend.comment.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.comment.entity.Comment;

/** Data access for {@link Comment}. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);

    @EntityGraph(attributePaths = "author")
    Page<Comment> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    long countByEntityTypeAndEntityId(String entityType, Long entityId);
}