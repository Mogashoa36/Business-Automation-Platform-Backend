package company.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Branding values surfaced to the Angular shell.
 *
 * <p>This is the single place a new client changes product level branding. Values
 * fall back to the defaults below, so a freshly copied project already renders
 * something sensible before anyone opens the configuration file.
 */
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {

    private String name = "Business Automation Platform";

    private String shortName = "Platform";

    private String version = "1.0.0";

    private String supportEmail = "support@example.com";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }
}