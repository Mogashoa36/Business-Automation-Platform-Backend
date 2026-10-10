package company.backend.document.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction over where uploaded bytes live.
 *
 * <p>Only the local filesystem adapter ships with the template. Adding S3, Azure
 * Blob or any other object store means writing one more implementation of this
 * interface and declaring it a bean - no entity, service, controller or API change,
 * because nothing outside this package ever sees a filesystem path.
 */
public interface StorageService {

    /**
     * Persists the stream and returns an opaque storage key.
     *
     * <p>Implementations must generate the key themselves; a caller supplied
     * filename is metadata only and must never influence where bytes are written.
     */
    String store(String originalFilename, String contentType, InputStream input) throws IOException;

    /** Opens a stream for reading. The caller closes it. */
    InputStream retrieve(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    boolean exists(String storageKey);

    /** Human readable description of the backend, surfaced through the settings API. */
    String describe();
}