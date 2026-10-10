package company.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap seed settings.
 *
 * <p>Seeding is idempotent: it only inserts what is missing, so it is safe to
 * leave enabled in every environment. What it must never do is invent a
 * production password, which is why the admin credential is opt-in.
 */
@ConfigurationProperties(prefix = "platform.seed")
public class SeedProperties {

    private boolean enabled = true;

    private String adminUsername = "admin";

    private String adminEmail = "admin@example.com";

    private String adminFirstName = "Platform";

    private String adminLastName = "Administrator";

    /**
     * Initial admin password. Supply it through the {@code PLATFORM_SEED_ADMIN_PASSWORD}
     * environment variable. When it is absent the seeder skips account creation and
     * explains how to do it, rather than shipping a well-known credential.
     */
    private String adminPassword;

    private String organizationName = "My Company";

    private List<String> departmentCodes = List.of("GENERAL", "OPERATIONS", "ADMINISTRATION");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminFirstName() {
        return adminFirstName;
    }

    public void setAdminFirstName(String adminFirstName) {
        this.adminFirstName = adminFirstName;
    }

    public String getAdminLastName() {
        return adminLastName;
    }

    public void setAdminLastName(String adminLastName) {
        this.adminLastName = adminLastName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public List<String> getDepartmentCodes() {
        return departmentCodes;
    }

    public void setDepartmentCodes(List<String> departmentCodes) {
        this.departmentCodes = departmentCodes;
    }
}