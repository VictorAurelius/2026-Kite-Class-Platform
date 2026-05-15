package com.kitehub.subscription.auth.twofactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-fast guards for {@link TotpSecretCipher} (GAP-553) and round-trip smoke.
 */
@DisplayName("TotpSecretCipher fail-fast (GAP-553)")
class TotpSecretCipherTest {

    private static final String PROD_KEY =
        "this-is-a-32-byte-long-real-prod-key!!XYZ";  // > 32 bytes, not dev default

    @Nested
    @DisplayName("non-production profile")
    class NonProd {

        @Test
        @DisplayName("dev-default key boots OK with warn log (no exception)")
        void devDefaultBootsInNonProd() {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("local");
            TotpSecretCipher cipher = new TotpSecretCipher(TotpSecretCipher.DEV_DEFAULT_KEY, env);
            cipher.validate();
            assertThat(cipher.decrypt(cipher.encrypt("hello"))).isEqualTo("hello");
        }

        @Test
        @DisplayName("short non-default key boots OK with pad-warning")
        void shortKeyBootsInNonProd() {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("test");
            TotpSecretCipher cipher = new TotpSecretCipher("short", env);
            cipher.validate();
            assertThat(cipher.decrypt(cipher.encrypt("round-trip"))).isEqualTo("round-trip");
        }
    }

    @Nested
    @DisplayName("production profile")
    class Production {

        @Test
        @DisplayName("dev-default key triggers IllegalStateException at validate()")
        void devDefaultFailsFastInProd() {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("production");
            TotpSecretCipher cipher = new TotpSecretCipher(TotpSecretCipher.DEV_DEFAULT_KEY, env);
            assertThatThrownBy(cipher::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kitehub.auth.totp.encryption-key")
                .hasMessageContaining("isDevDefault=true");
        }

        @Test
        @DisplayName("short key (< 32 bytes) triggers IllegalStateException at validate()")
        void shortKeyFailsFastInProd() {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("prod");
            TotpSecretCipher cipher = new TotpSecretCipher("too-short", env);
            assertThatThrownBy(cipher::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("length=9");
        }

        @Test
        @DisplayName("real ≥32-byte key boots OK in production")
        void realKeyBootsInProd() {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("production");
            TotpSecretCipher cipher = new TotpSecretCipher(PROD_KEY, env);
            cipher.validate();
            assertThat(cipher.decrypt(cipher.encrypt("payload"))).isEqualTo("payload");
        }
    }
}
