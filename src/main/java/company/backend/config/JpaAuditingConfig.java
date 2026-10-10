package company.backend.config;

import company.backend.security.PlatformUserPrincipal;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/** Enables JPA auditing and resolves the current principal id for audit columns. */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * Resolves the id of the user performing the current request. Returns an empty
     * optional for background jobs, migrations and seeding, which legitimately run
     * without an authenticated principal.
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof PlatformUserPrincipal platformUser) {
                return Optional.of(platformUser.getId());
            }
            if (principal instanceof UserDetails userDetails) {
                try {
                    return Optional.of(Long.parseLong(userDetails.getUsername()));
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        };
    }
}