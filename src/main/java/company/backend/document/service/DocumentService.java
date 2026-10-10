package company.backend.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.config.StorageProperties;
import company.backend.document.dto.DocumentLinkResponse;
import company.backend.document.dto.DocumentResponse;
import company.backend.document.dto.LinkDocumentRequest;
import company.backend.document.entity.Document;
import company.backend.document.entity.DocumentLink;
import company.backend.document.repository.DocumentLinkRepository;
import company.backend.document.repository.DocumentRepository;
import company.backend.document.storage.StorageService;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.security.SecurityUtils;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;

/** Upload, download and attachment of documents. */
@Service
@Transactional
public class DocumentService {

    private static final Set<String> SUPPORTED_TARGETS = Set.of(
            "TASK", "WORKFLOW_INSTANCE", "APPROVAL", "DOCUMENT");
    private static final Pattern VALID_TYPE = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final UserRepository userRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentLinkRepository documentLinkRepository,
            StorageService storageService,
            StorageProperties storageProperties,
            UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> search(String search, String category, Long uploadedById, Pageable pageable) {
        Specification<Document> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("filename")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(root.get("category"), category));
            }
            if (uploadedById != null) {
                predicates.add(builder.equal(root.get("uploadedBy").get("id"), uploadedById));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return documentRepository.findAll(spec, pageable).map(DocumentService::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse findById(Long id) {
        return toResponse(requireDocument(id));
    }

    /**
     * Stores an uploaded file and records its metadata.
     *
     * <p>Validation happens before anything touches the disk: size, extension and
     * emptiness are all checked up front so a rejected upload never leaves an
     * orphaned blob behind.
     */
    @Audited(module = "document", action = AuditAction.UPLOAD, entityType = "Document")
    public DocumentResponse upload(MultipartFile file, String category, String description) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("The uploaded file is empty");
        }
        if (file.getSize() > storageProperties.getMaxFileSize().toBytes()) {
            throw new BusinessException("The uploaded file exceeds the maximum allowed size of "
                    + storageProperties.getMaxFileSize().toMegabytes() + " MB");
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        validateExtension(originalName);

        String storageKey;
        try (InputStream input = file.getInputStream()) {
            storageKey = storageService.store(originalName, file.getContentType(), input);
        } catch (IOException ex) {
            throw new BusinessException("The file could not be stored");
        }

        Document document = new Document();
        document.setFilename(originalName);
        document.setStorageKey(storageKey);
        document.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setCategory(category);
        document.setDescription(description);
        document.setUploadedBy(resolveUploader());
        return toResponse(documentRepository.save(document));
    }

    /** Opens a stream for download. The caller is responsible for closing it. */
    @Transactional(readOnly = true)
    public InputStream openStream(Long id) {
        Document document = requireDocument(id);
        if (!document.isActive()) {
            throw ResourceNotFoundException.of("Document", id);
        }
        try {
            return storageService.retrieve(document.getStorageKey());
        } catch (IOException ex) {
            throw ResourceNotFoundException.of("File", document.getStorageKey());
        }
    }

    /**
     * Soft deletes a document.
     *
     * <p>The row is retained so historic references still resolve, but the bytes
     * are released and the document no longer appears in listings. A document
     * still attached to a business record is refused, to avoid breaking it.
     */
    @Audited(module = "document", action = AuditAction.DELETE, entityType = "Document")
    public void delete(Long id) {
        Document document = requireDocument(id);
        long attachments = documentLinkRepository.countByDocumentId(id);
        if (attachments > 0) {
            throw new BusinessException(
                    "This document is attached to " + attachments + " record(s); detach it first");
        }
        try {
            storageService.delete(document.getStorageKey());
        } catch (IOException ex) {
            throw new BusinessException("The stored file could not be removed");
        }
        document.setActive(false);
        documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentLinkResponse> findAttachments(String entityType, Long entityId) {
        validateTargetType(entityType);
        return documentLinkRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(DocumentService::toLinkResponse)
                .toList();
    }

    @Audited(module = "document", action = AuditAction.CREATE, entityType = "DocumentLink")
    public DocumentLinkResponse link(Long documentId, LinkDocumentRequest request) {
        Document document = requireDocument(documentId);
        validateTargetType(request.entityType());
        if (request.entityId() == null) {
            throw new BusinessException("An entity id is required");
        }

        DocumentLink link = new DocumentLink();
        link.setDocument(document);
        link.setEntityType(request.entityType());
        link.setEntityId(request.entityId());
        link.setLinkedBy(resolveUploader());
        return toLinkResponse(documentLinkRepository.save(link));
    }

    @Audited(module = "document", action = AuditAction.DELETE, entityType = "DocumentLink")
    public void unlink(Long linkId) {
        DocumentLink link = documentLinkRepository
                .findById(linkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", linkId));
        documentLinkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public Document requireDocument(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Document", id));
    }

    /** Describes the active storage backend for the settings screen. */
    public String describeStorage() {
        return storageService.describe();
    }

    private void validateExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
        boolean allowed = storageProperties.getAllowedExtensions().stream().anyMatch(extension::equals);
        if (extension.isEmpty() || !allowed) {
            throw new BusinessException("Files of type '" + extension + "' are not accepted");
        }
    }

    /**
     * Strips any directory component from a client supplied filename.
     *
     * <p>The name is only ever used for display and to derive the extension; the
     * storage key itself is generated server side.
     */
    private static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("A file name is required");
        }
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            throw new BusinessException("A file name is required");
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static void validateTargetType(String entityType) {
        if (entityType == null || !VALID_TYPE.matcher(entityType.toUpperCase(Locale.ROOT)).matches()) {
            throw new BusinessException("Invalid attachment target type");
        }
        if (!SUPPORTED_TARGETS.contains(entityType.toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Documents cannot be attached to '" + entityType + "'");
        }
    }

    private User resolveUploader() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private static DocumentResponse toResponse(Document document) {
        User uploader = document.getUploadedBy();
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getCategory(),
                document.getDescription(),
                uploader == null ? null : uploader.getId(),
                uploader == null ? null : uploader.getFullName(),
                document.getVersion(),
                document.isActive(),
                document.getCreatedAt());
    }

    private static DocumentLinkResponse toLinkResponse(DocumentLink link) {
        Document document = link.getDocument();
        User linkedBy = link.getLinkedBy();
        return new DocumentLinkResponse(
                link.getId(),
                document.getId(),
                document.getFilename(),
                document.getSizeBytes(),
                document.getContentType(),
                link.getEntityType(),
                link.getEntityId(),
                linkedBy == null ? null : linkedBy.getId(),
                link.getCreatedAt());
    }
}