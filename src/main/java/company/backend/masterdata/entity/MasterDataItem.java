package company.backend.masterdata.entity;

import company.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A reusable reference value, e.g. a document category or a location.
 *
 * <p>Deliberately minimal - a group, a code and a label. Anything that needs
 * business rules (departments, priorities, statuses) gets a dedicated entity
 * instead; this table exists so a client project can add a dropdown without a
 * migration.
 */
@Entity
@Table(
        name = "master_data_items",
        indexes = @Index(name = "idx_master_data_group", columnList = "data_group,code", unique = true))
@Getter
@Setter
public class MasterDataItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Namespace for the list, e.g. {@code DOCUMENT_CATEGORY} or {@code LOCATION}. */
    @Column(name = "data_group", nullable = false, length = 50)
    private String group;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "system_managed", nullable = false)
    private boolean systemManaged = false;
}