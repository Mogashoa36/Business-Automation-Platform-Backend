package company.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity does not exist. Rendered as HTTP 404. */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /** Builds the standard "Type [id] not found" message. */
    public static ResourceNotFoundException of(String type, Object id) {
        return new ResourceNotFoundException(type + " not found with id: " + id);
    }

    public static ResourceNotFoundException of(String type, String identifier) {
        return new ResourceNotFoundException(type + " not found: " + identifier);
    }
}