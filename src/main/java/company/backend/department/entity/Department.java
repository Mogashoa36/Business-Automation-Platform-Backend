package company.backend.department.entity;

import java.util.LinkedHashSet;
import java.util.Set;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    /**
     * Manager of this department.
     *
     * <p>Deliberately a plain id rather than a JPA association: a manager always
     * belongs to a department, so a real association would create a circular
     * mapping between {@code departments} and {@code users}. The database mirrors
     * this with an index instead of a foreign key. {@link DepartmentService}
     * validates that the user exists before saving.
     */
    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private Set<User> members = new LinkedHashSet<>();
}