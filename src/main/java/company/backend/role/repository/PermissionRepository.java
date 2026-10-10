package company.backend.role.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.role.entity.Permission;

/** Data access for {@link Permission}. */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAllByOrderByModuleAscNameAsc();

    List<Permission> findByCodeIn(List<String> codes);
}