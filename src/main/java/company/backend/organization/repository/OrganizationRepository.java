package company.backend.organization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.organization.entity.Organization;

/**
 * A deployment holds exactly one organization record, so the primary key is always
 * known. The lookup by name is retained for imports and future migrations.
 */
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findFirstByOrderByIdAsc();

    Optional<Organization> findByNameIgnoreCase(String name);
}