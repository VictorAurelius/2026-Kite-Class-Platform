package com.kiteclass.gateway.config;

import com.kiteclass.gateway.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default user password on application startup.
 *
 * <p>Ensures the owner@kiteclass.local user has the correct BCrypt password hash
 * for "Admin@123" by encoding it with the application's configured PasswordEncoder.
 * This solves hash mismatch issues that can occur with hardcoded hashes in migrations.
 *
 * <p>Only runs in non-test profiles. Test classes should handle password setup
 * in their own @BeforeEach methods using the injected PasswordEncoder.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DefaultUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_EMAIL = "owner@kiteclass.local";
    private static final String DEFAULT_PASSWORD = "Admin@123";

    /**
     * Runs on application startup to initialize default user password.
     * Updates the owner user's password hash with a freshly encoded version
     * using the application's configured BCryptPasswordEncoder.
     *
     * @param args application arguments (not used)
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing default user password...");

        userRepository.findByEmailAndDeletedFalse(DEFAULT_EMAIL)
                .flatMap(user -> {
                    // Always update password hash to ensure it matches the current encoder configuration
                    String newHash = passwordEncoder.encode(DEFAULT_PASSWORD);
                    user.setPasswordHash(newHash);
                    log.info("Updated password hash for user: {}", DEFAULT_EMAIL);
                    return userRepository.save(user);
                })
                .doOnSuccess(user -> log.info("Default user password initialized successfully"))
                .doOnError(error -> log.error("Failed to initialize default user password: {}", error.getMessage()))
                .subscribe(); // Non-blocking execution
    }
}
