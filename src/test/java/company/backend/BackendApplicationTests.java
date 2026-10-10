package company.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import company.backend.role.entity.Role;
import company.backend.role.repository.PermissionRepository;
import company.backend.role.repository.RoleRepository;
import company.backend.user.entity.User;
import company.backend.user.entity.UserStatus;
import company.backend.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Verifies that the application context starts and that the seeder produces a
 * usable installation.
 *
 * <p>This is the test that would catch a broken bean wiring, an invalid entity
 * mapping or a seeder that quietly does nothing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BackendApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    void seederCreatesTheDefaultRoles() {
        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("MANAGER")).isPresent();
        assertThat(roleRepository.findByCode("EMPLOYEE")).isPresent();
        assertThat(roleRepository.findByCode("VIEWER")).isPresent();
    }

    @Test
    void seederRegistersPermissionsAndGrantsThemToTheAdministrator() {
        assertThat(permissionRepository.count()).isGreaterThan(20);

        Role admin = roleRepository.findByCode("ADMIN").orElseThrow();
        assertThat(admin.isSystemRole()).isTrue();
        assertThat(admin.getPermissions()).hasSize(permissionRepository.findAll().size());
    }

    @Test
    void seederCreatesAnAdministratorWhenAPasswordIsConfigured() {
        User admin = userRepository.findByEmailIgnoreCase("admin@test.local").orElseThrow();
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.hasRole("ADMIN")).isTrue();
        // The credential must be rotated on first sign-in.
        assertThat(admin.isMustChangePassword()).isTrue();
        // And it must never be stored in the clear.
        assertThat(admin.getPassword()).isNotEqualTo("TestAdmin123!").startsWith("$2");
    }

    @Test
    void auditingColumnsArePopulatedOnPersist() {
        User admin = userRepository.findByEmailIgnoreCase("admin@test.local").orElseThrow();
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(admin.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }
}
