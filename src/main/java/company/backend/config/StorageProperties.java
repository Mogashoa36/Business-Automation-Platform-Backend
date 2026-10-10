package company.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * File storage settings.
 *
 * <p>Only the local adapter is shipped with the template. Pointing the platform at
 * S3 (or any other object store) only requires adding a
 * {@code company.backend.document.storage.StorageService} bean - no schema, service
 * or controller changes are needed.
 */
@ConfigurationProperties(prefix = "platform.storage")
public class StorageProperties {

    /** Root folder for local files. Relative paths resolve against the working directory. */
    private String location = "./data/uploads";

    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    /** Extensions accepted for upload, lower case and dot prefixed. */
    private List<String> allowedExtensions = List.of(
            ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".webp",
            ".txt", ".csv", ".md", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip");

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}