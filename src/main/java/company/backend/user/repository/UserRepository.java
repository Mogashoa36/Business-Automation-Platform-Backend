package company.backend.user.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import company.backend.user.entity.User;
import company.backend.user.entity.UserStatus;

/** Data access for {@link User}. Specification queries back the search/filter screens. */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findWithRolesById(Long id);

    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findByIdAndStatus(Long id, UserStatus status);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"roles", "department"})
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "department"})
    Page<User> findByDepartmentId(Long departmentId, Pageable pageable);

    long countByStatus(UserStatus status);

    @EntityGraph(attributePaths = {"roles", "department"})
    List<User> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"roles", "department"})
    List<User> findByDepartmentIdIn(Collection<Long> departmentIds);

    @Query("select u from User u join u.roles r where r.code = :roleCode and u.status = 'ACTIVE'")
    List<User> findActiveUsersByRoleCode(@Param("roleCode") String roleCode);

    /** Guards against deactivating the account that administers the deployment. */
    @Query("select count(u) from User u join u.roles r where r.code = :roleCode and u.status = 'ACTIVE'")
    long countActiveByRoleCode(@Param("roleCode") String roleCode);

    /** Counters backing the dashboard summary cards. */
    @Query("select count(u) from User u where u.lastLoginAt >= :since")
    long countActiveSince(@Param("since") Instant since);
}