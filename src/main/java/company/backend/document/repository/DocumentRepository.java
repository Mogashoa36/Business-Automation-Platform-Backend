package company.backend.document.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import company.backend.document.entity.Document;

/** Data access for {@link Document}. */
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    @EntityGraph(attributePaths = "uploadedBy")
    List<Document> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "uploadedBy")
    Page<Document> findByCategory(String category, Pageable pageable);

    @EntityGraph(attributePaths = "uploadedBy")
    Page<Document> findByUploadedById(Long userId, Pageable pageable);

    long countByCategory(String category);
}