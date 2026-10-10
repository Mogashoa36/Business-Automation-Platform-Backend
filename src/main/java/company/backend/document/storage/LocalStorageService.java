package company.backend.document.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import company.backend.config.StorageProperties;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;

/**
 * Stores files on the local filesystem, sharded by upload date.
 *
 * <p>Two safeguards matter here:
 * <ul>
 *   <li>Storage keys are generated, never derived from the uploaded filename, so a
 *       name like {@code ../../etc/passwd} cannot influence the destination.</li>
 *   <li>Every resolved path is re-checked against the storage root, which protects
 *       against traversal even if a key were ever tampered with in the database.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(
        name = "platform.storage.type",
        havingValue = "local",
        matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final DateTimeFormatter SHARD = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties properties;
    private Path root;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialiseRoot() {
        this.root = Path.of(properties.getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create the document storage directory: " + root, ex);
        }
        log.info("Local document storage rooted at {}", root);
    }

    @Override
    public String store(String originalFilename, String contentType, InputStream input) throws IOException {
        String key = generateKey(originalFilename);
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public InputStream retrieve(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw ResourceNotFoundException.of("File", storageKey);
        }
        return Files.newInputStream(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            return Files.isRegularFile(resolve(storageKey));
        } catch (BusinessException ex) {
            return false;
        }
    }

    @Override
    public String describe() {
        return "Local filesystem (" + root + ")";
    }

    /**
     * Builds a key of the form {@code 2026/01/31/<uuid>.<ext>}.
     *
     * <p>Only the extension is taken from the caller, and it is sanitised, so the
     * result cannot contain path separators or traversal sequences.
     */
    private String generateKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            }
        }
        String name = UUID.randomUUID().toString().replace("-", "");
        String key = LocalDate.now().format(SHARD) + "/" + name + (extension.isEmpty() ? "" : "." + extension);
        return key;
    }

    /**
     * Maps a storage key onto a path, refusing anything that escapes the root.
     */
    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessException("Invalid storage key");
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            log.warn("Rejected storage key that escapes the root directory: {}", storageKey);
            throw new BusinessException("Invalid storage key");
        }
        return resolved;
    }
}