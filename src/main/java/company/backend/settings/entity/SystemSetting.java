package company.backend.settings.entity;

import java.time.Instant;

import company.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * An editable, admin-managed configuration value.
 *
 * <p>Used for settings an administrator should be able to change at runtime
 * (company contact details, notification defaults, retention days). Secrets are
 * deliberately excluded: those belong in environment variables, not in a table
 * that the settings UI would happily render.
 */
@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dotted key such as {@code notification.email.enabled}. */
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", length = 2000)
    private String value;

    /** Human readable type hint used by the UI to pick a control. */
    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType = "STRING";

    @Column(name = "setting_group", nullable = false, length = 50)
    private String group = "GENERAL";

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "editable", nullable = false)
    private boolean editable = true;
}