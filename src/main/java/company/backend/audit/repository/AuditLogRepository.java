package company.backend.audit.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import company.backend.audit.entity.AuditLog;

/** Data access for {@link AuditLog}. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(Instant since);
}