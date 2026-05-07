package com.kitehub.subscription.seed;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductionSeedRunner}. Covers AC items from
 * `documents/04-quality/gaps/GAP-376-production-data-seed.md`:
 * <ul>
 *   <li>idempotent re-run (admin already exists → skip).</li>
 *   <li>skip-no-flag (mode != production → no DB writes).</li>
 *   <li>admin created when fresh DB (creates user + system_config rows).</li>
 *   <li>missing password → fail-fast with descriptive error.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProductionSeedRunnerTest {

    private UserRepository userRepository;
    private SystemConfigSeedDao systemConfigDao;
    private SeedProperties properties;
    private ProductionSeedRunner runner;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        systemConfigDao = mock(SystemConfigSeedDao.class);
        properties = new SeedProperties();
        properties.setAdminEmail("admin@kitehub.vn");
        properties.setAdminPassword("temp-Strong-P@ss-2026");
        runner = new ProductionSeedRunner(properties, userRepository, systemConfigDao);
    }

    @Nested
    @DisplayName("Skip when not in production mode")
    class SkipMode {

        @Test
        @DisplayName("mode=none → no DB interaction at all")
        void skipsWhenModeIsNone() {
            properties.setMode("none");

            runner.run(new DefaultApplicationArguments());

            verifyNoInteractions(userRepository);
            verifyNoInteractions(systemConfigDao);
        }

        @Test
        @DisplayName("mode=dev → still no DB interaction (dev seeds via HTTP)")
        void skipsWhenModeIsDev() {
            properties.setMode("dev");

            runner.run(new DefaultApplicationArguments());

            verifyNoInteractions(userRepository);
            verifyNoInteractions(systemConfigDao);
        }

        @Test
        @DisplayName("default-constructed properties stay in 'none' mode → skip")
        void skipsWhenModeUnset() {
            // properties.mode default = "none"
            runner.run(new DefaultApplicationArguments());

            verifyNoInteractions(userRepository);
            verifyNoInteractions(systemConfigDao);
        }
    }

    @Nested
    @DisplayName("Production mode — fresh DB")
    class FreshDb {

        @BeforeEach
        void enableProduction() {
            properties.setMode("production");
        }

        @Test
        @DisplayName("Creates admin user + seeds 3 system_config rows when DB empty")
        void createsAdminAndConfig() {
            when(userRepository.existsByEmail("admin@kitehub.vn")).thenReturn(false);

            runner.run(new DefaultApplicationArguments());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User saved = userCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo("admin@kitehub.vn");
            assertThat(saved.getRole()).isEqualTo("PLATFORM_ADMIN");
            assertThat(saved.isEmailVerified()).isTrue();
            assertThat(saved.getId()).isEqualTo(ProductionSeedRunner.PLATFORM_ADMIN_ID);
            // BCrypt hash should NOT contain plaintext password
            assertThat(saved.getPasswordHash())
                    .startsWith("$2")
                    .doesNotContain("temp-Strong-P@ss-2026");

            verify(systemConfigDao).upsert("default_tier", "FREE",
                    "Default pricing tier for new instances");
            verify(systemConfigDao).upsert("currency", "VND",
                    "Platform default currency code (ISO-4217)");
            verify(systemConfigDao).upsert("locale", "vi",
                    "Platform default locale (BCP-47)");
            verify(systemConfigDao, times(3)).upsert(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Production mode — idempotent re-run")
    class IdempotentReRun {

        @BeforeEach
        void enableProduction() {
            properties.setMode("production");
        }

        @Test
        @DisplayName("Admin already exists → skips save() but still upserts system_config")
        void skipsAdminButRefreshesConfig() {
            when(userRepository.existsByEmail("admin@kitehub.vn")).thenReturn(true);

            runner.run(new DefaultApplicationArguments());

            verify(userRepository).existsByEmail("admin@kitehub.vn");
            verify(userRepository, never()).save(any(User.class));
            // system_config still upserted — relies on ON CONFLICT DO NOTHING for idempotency
            verify(systemConfigDao, times(3)).upsert(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Two consecutive runs converge to same DB state (idempotent)")
        void twoConsecutiveRunsAreIdempotent() {
            // First run: empty DB.
            when(userRepository.existsByEmail("admin@kitehub.vn"))
                    .thenReturn(false)   // first call
                    .thenReturn(true);   // second call (admin now present)

            runner.run(new DefaultApplicationArguments());
            runner.run(new DefaultApplicationArguments());

            // save() invoked exactly once across both runs.
            verify(userRepository, times(1)).save(any(User.class));
            // system_config upserted twice × 3 keys = 6 calls total (no duplicate rows
            // thanks to ON CONFLICT DO NOTHING in SystemConfigSeedDao).
            verify(systemConfigDao, times(6)).upsert(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Validation guards")
    class Validation {

        @Test
        @DisplayName("Missing admin password → throws IllegalStateException with helpful message")
        void missingPasswordFailsFast() {
            properties.setMode("production");
            properties.setAdminPassword(null);

            assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("kite.seed.admin-password")
                    .hasMessageContaining("SEED_ADMIN_PASSWORD");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Blank admin email → throws IllegalStateException")
        void blankEmailFailsFast() {
            properties.setMode("production");
            properties.setAdminEmail("");

            assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("kite.seed.admin-email");
        }
    }
}
