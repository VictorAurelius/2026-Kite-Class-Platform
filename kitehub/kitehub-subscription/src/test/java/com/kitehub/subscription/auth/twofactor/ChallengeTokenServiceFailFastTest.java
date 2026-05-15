package com.kitehub.subscription.auth.twofactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-fast guards for {@link ChallengeTokenService#validate()} (GAP-553).
 */
@DisplayName("ChallengeTokenService fail-fast (GAP-553)")
class ChallengeTokenServiceFailFastTest {

    private static final String PROD_SECRET =
        "this-is-a-32-byte-long-real-prod-secret!XYZ";

    @Test
    @DisplayName("dev-default secret boots OK in non-production")
    void devDefaultBootsInNonProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        ChallengeTokenService svc = new ChallengeTokenService(
            ChallengeTokenService.DEV_DEFAULT_SECRET, env);
        assertThatCode(svc::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("dev-default secret triggers IllegalStateException in production")
    void devDefaultFailsFastInProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        ChallengeTokenService svc = new ChallengeTokenService(
            ChallengeTokenService.DEV_DEFAULT_SECRET, env);
        assertThatThrownBy(svc::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jwt.challenge-secret")
            .hasMessageContaining("isDevDefault=true");
    }

    @Test
    @DisplayName("short secret (<32 bytes) triggers IllegalStateException in production")
    void shortSecretFailsFastInProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ChallengeTokenService svc = new ChallengeTokenService("nope", env);
        assertThatThrownBy(svc::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("length=4");
    }

    @Test
    @DisplayName("real ≥32-byte secret boots OK in production")
    void realSecretBootsInProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        ChallengeTokenService svc = new ChallengeTokenService(PROD_SECRET, env);
        assertThatCode(svc::validate).doesNotThrowAnyException();
    }
}
