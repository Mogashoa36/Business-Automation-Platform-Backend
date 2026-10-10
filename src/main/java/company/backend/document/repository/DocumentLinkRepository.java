package company.backend.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.document.entity.DocumentLink;

/** Data access for {@link DocumentLink}. */
public interface DocumentLinkRepository extends JpaRepository<DocumentLink, Long> {

    @EntityGraph(attributePaths = {"document", "linkedBy"})
    List<DocumentLink> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @EntityGraph(attributePaths = {"document", "linkedBy"})
    List<DocumentLink> findByDocumentIdIn(List<Long> documentIds);

    long countByDocumentId(Long documentId);
}