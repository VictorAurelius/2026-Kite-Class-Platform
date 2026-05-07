package com.kitehub.subscription.seed;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Production data seed runner — populates platform admin user + system config rows
 * on first deploy. Idempotent: re-running has no effect after first successful run.
 *
 * <p>Activation: set property {@code kite.seed.mode=production} (or env
 * {@code KITE_SEED_MODE=production}) AND provide
 * {@code kite.seed.admin-password} (or {@code SEED_ADMIN_PASSWORD} env from secrets
 * manager — GAP-379).</p>
 *
 * <p>Modes:</p>
 * <ul>
 *   <li>{@code none} (default) — runner skips silently.</li>
 *   <li>{@code production} — seeds {@code admin@kitehub.vn} PLATFORM_ADMIN + system_config rows.</li>
 *   <li>{@code dev} / other — runner skips with INFO log (dev seed lives in
 *       {@code kitehub/scripts/seed-data.sh} which posts via gateway).</li>
 * </ul>
 *
 * <p>Skipped when:</p>
 * <ul>
 *   <li>Mode is not {@code production}.</li>
 *   <li>CLI arg {@code --seed-mode=production} not honored unless property/env set
 *       (this runner reads {@link SeedProperties}, not raw {@link ApplicationArguments};
 *       CLI users wrap via {@code scripts/seed-production.sh}).</li>
 * </ul>
 *
 * <p>Spec: {@code documents/04-quality/gaps/GAP-376-production-data-seed.md}.</p>
 *
 * @since 1.0.0
 */
@Component
@Configuration
@EnableConfigurationProperties(SeedProperties.class)
public class ProductionSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSeedRunner.class);

    /** Stable admin UUID for the platform-level admin (idempotency anchor). */
    static final UUID PLATFORM_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final SeedProperties properties;
    private final UserRepository userRepository;
    private final SystemConfigSeedDao systemConfigDao;
    private final BCryptPasswordEncoder passwordEncoder;

    public ProductionSeedRunner(SeedProperties properties,
                                UserRepository userRepository,
                                SystemConfigSeedDao systemConfigDao) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.systemConfigDao = systemConfigDao;
        // Strength 12 matches V9 seed convention.
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isProduction()) {
            log.debug("ProductionSeedRunner skipped — kite.seed.mode={} (expected 'production')",
                    properties.getMode());
            return;
        }

        log.info("ProductionSeedRunner starting — mode=production, admin-email={}",
                properties.getAdminEmail());

        seedAdminUser();
        seedSystemConfig();

        log.info("ProductionSeedRunner finished successfully — idempotent re-run safe.");
    }

    /**
     * Seed the platform admin. Idempotent: skips if a user with the configured email
     * already exists (matching by email is the natural uniqueness key).
     */
    void seedAdminUser() {
        String email = requireNonBlank(properties.getAdminEmail(), "kite.seed.admin-email");
        String password = requireNonBlank(properties.getAdminPassword(),
                "kite.seed.admin-password (or SEED_ADMIN_PASSWORD env)");

        if (userRepository.existsByEmail(email)) {
            log.info("Admin user already present (email={}). Seed step SKIPPED.", email);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        User admin = User.builder()
                .id(PLATFORM_ADMIN_ID)
                .email(email)
                .name(properties.getAdminName())
                .passwordHash(passwordEncoder.encode(password))
                .role("PLATFORM_ADMIN")
                .emailVerified(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(admin);
        log.info("Admin user seeded — id={}, email={}, role=PLATFORM_ADMIN",
                PLATFORM_ADMIN_ID, email);
    }

    /**
     * Seed system_config rows. Each row is upserted via INSERT ... ON CONFLICT DO NOTHING
     * inside {@link SystemConfigSeedDao}, so repeat runs are no-ops.
     */
    void seedSystemConfig() {
        systemConfigDao.upsert("default_tier", properties.getDefaultTier(),
                "Default pricing tier for new instances");
        systemConfigDao.upsert("currency", properties.getDefaultCurrency(),
                "Platform default currency code (ISO-4217)");
        systemConfigDao.upsert("locale", properties.getDefaultLocale(),
                "Platform default locale (BCP-47)");
        log.info("System config seeded — default_tier={}, currency={}, locale={}",
                properties.getDefaultTier(), properties.getDefaultCurrency(),
                properties.getDefaultLocale());
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Production seed aborted — " + name + " is missing or blank. "
                  + "Set the property or env var before launching with kite.seed.mode=production.");
        }
        return value;
    }
}
