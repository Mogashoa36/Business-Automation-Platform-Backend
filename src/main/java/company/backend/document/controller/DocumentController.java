package company.backend.document.controller;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import company.backend.common.ApiResponse;
import company.backend.common.PageResponse;
import company.backend.common.PageableFactory;
import company.backend.document.dto.DocumentLinkResponse;
import company.backend.document.dto.DocumentResponse;
import company.backend.document.dto.LinkDocumentRequest;
import company.backend.document.service.DocumentService;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/** Document upload, download and attachment. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_VIEW + "')")
    public ApiResponse<PageResponse<DocumentResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long uploadedById,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageableFactory.create(page, size, "createdAt", "desc", Set.of("createdAt", "filename"), "createdAt");
        return ApiResponse.ok(PageResponse.of(documentService.search(search, category, uploadedById, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_VIEW + "')")
    public ApiResponse<DocumentResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(documentService.findById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_UPLOAD + "')")
    public ApiResponse<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description) {
        return ApiResponse.created("Document uploaded", documentService.upload(file, category, description));
    }

    /**
     * Streams the file back.
     *
     * <p>The filename is quoted and neutralised before being placed in the
     * {@code Content-Disposition} header so a crafted name cannot inject extra
     * header directives.
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_VIEW + "')")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        DocumentResponse document = documentService.findById(id);
        InputStream stream = documentService.openStream(id);

        String safeName = document.filename().replaceAll("[\\r\\n\"]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeName + "\"")
                .contentLength(document.sizeBytes())
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_DELETE + "')")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }

    @GetMapping("/attachments/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_VIEW + "')")
    public ApiResponse<List<DocumentLinkResponse>> findAttachments(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ApiResponse.ok(documentService.findAttachments(entityType, entityId));
    }

    @PostMapping("/{id}/link")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_UPLOAD + "')")
    public ApiResponse<DocumentLinkResponse> link(
            @PathVariable Long id, @Valid @RequestBody LinkDocumentRequest request) {
        return ApiResponse.created("Document attached", documentService.link(id, request));
    }

    @DeleteMapping("/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.DOCUMENT_DELETE + "')")
    public void unlink(@PathVariable Long linkId) {
        documentService.unlink(linkId);
    }
}