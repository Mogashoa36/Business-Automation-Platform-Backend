package company.backend.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import company.backend.user.entity.User;
import company.backend.user.repository.UserRepository;

/**
 * Persists failed sign-in attempts.
 *
 * <p>This lives in its own component for a specific reason: a failed login throws,
 * which rolls back the surrounding transaction and would discard the counter along
 * with it - leaving accounts that could be brute-forced indefinitely. Recording the
 * attempt in a separate, committed transaction is what makes the lockout real.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Increments the failure counter and locks the account once the threshold is
     * reached. Committed even though the sign-in that triggered it has failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        boolean justLocked = user.recordFailedLogin();
        userRepository.saveAndFlush(user);

        if (justLocked) {
            log.warn("Account {} locked after {} failed sign-in attempts", user.getUsername(),
                    user.getFailedLoginAttempts());
        }
    }
}