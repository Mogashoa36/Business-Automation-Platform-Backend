package company.backend.audit.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import company.backend.audit.dto.AuditLogResponse;
import company.backend.audit.entity.AuditLog;
import company.backend.audit.repository.AuditLogRepository;
import company.backend.common.ApiResponse;
import company.backend.common.PageResponse;
import company.backend.common.PageableFactory;
import company.backend.security.Permissions;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Read-only access to the audit trail. */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "action", "module", "username");

    private final AuditLogRepository auditLogRepository;
    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.AUDIT_VIEW + "')")
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Specification<AuditLog> spec = buildSpecification(search, module, action, userId, from, to);
        Pageable pageable = PageableFactory.create(page, size, "createdAt", "desc", SORTABLE_FIELDS, "createdAt");

        return ApiResponse.ok(
                PageResponse.from(auditLogRepository.findAll(spec, pageable), AuditLogController::toResponse));
    }

    private static Specification<AuditLog> buildSpecification(
            String search, String module, String action, Long userId, Instant from, Instant to) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("username")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("entityId")), pattern)));
            }
            if (module != null && !module.isBlank()) {
                predicates.add(builder.equal(root.get("module"), module));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(builder.equal(root.get("action"), action));
            }
            if (userId != null) {
                predicates.add(builder.equal(root.get("userId"), userId));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getAction(),
                log.getModule(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getIpAddress(),
                log.isSuccess(),
                log.getCreatedAt());
    }
}