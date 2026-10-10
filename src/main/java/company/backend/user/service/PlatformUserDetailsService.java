package company.backend.user.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.role.entity.Role;
import company.backend.security.PlatformUserPrincipal;
import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

/**
 * Loads users together with their effective permissions.
 *
 * <p>Authorities are resolved from the database on every authentication rather than
 * baked into the JWT, so revoking a permission takes effect immediately instead of
 * when the access token happens to expire.
 */
@Service
public class PlatformUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public PlatformUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = resolve(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for: " + identifier));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public PlatformUserPrincipal loadPrincipalById(Long id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for id: " + id));
        return toPrincipal(user);
    }

    private java.util.Optional<User> resolve(String identifier) {
        try {
            return userRepository.findWithRolesById(Long.parseLong(identifier));
        } catch (NumberFormatException notAnId) {
            return userRepository.findByEmailIgnoreCase(identifier);
        }
    }

    private PlatformUserPrincipal toPrincipal(User user) {
        List<String> roleCodes = user.getRoles().stream().map(Role::getCode).sorted().toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(company.backend.role.entity.Permission::getCode)
                .distinct()
                .sorted()
                .toList();
        return new PlatformUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.isActive(),
                permissions,
                roleCodes);
    }
}