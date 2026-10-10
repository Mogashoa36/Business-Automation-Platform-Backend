package company.backend.document.entity;

import company.backend.common.BaseEntity;
import company.backend.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Metadata for a stored file.
 *
 * <p>The file bytes never live in the database. Only a {@code storageKey} is kept -
 * an opaque identifier that the configured {@code StorageService} resolves. No
 * filesystem path is ever exposed through the API, which is what allows the
 * backend to move from local disk to S3 without an API change.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /** Opaque key resolved by the storage adapter; never a raw filesystem path. */
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Free-form classification; seeded from master data such as CONTRACT or INVOICE. */
    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Guards against two uploads of the same key racing each other. */
    @jakarta.persistence.Version
    @Column(name = "row_version")
    private Long rowVersion;

    public String getExtension() {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }
}