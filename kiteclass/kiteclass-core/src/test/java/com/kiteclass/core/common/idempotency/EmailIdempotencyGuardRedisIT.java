package com.kiteclass.core.common.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-restart integration coverage for {@link EmailIdempotencyGuard} (GAP-840 sister
 * of GAP-580 — producer-side guard for {@code ClassRescheduledEmailConsumer} forwards).
 *
 * <p>Strategy mirrors {@code com.kitehub.email.service.EmailIdempotencyGuardRedisIT}:
 * spin up a Redis Testcontainer once for the class, build a fresh
 * {@link StringRedisTemplate} pointing at it, then simulate a process restart by
 * constructing a SECOND {@link EmailIdempotencyGuard} instance against the same Redis
 * container. The first guard marks the key; the second guard — representing the
 * post-restart process — sees the key already in Redis and suppresses the duplicate
 * forward.</p>
 */
@Testcontainers
@DisplayName("EmailIdempotencyGuard (kiteclass-core) — Redis cross-restart dedup (GAP-840)")
class EmailIdempotencyGuardRedisIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private StringRedisTemplate newTemplate() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

    @Test
    @DisplayName("first guard marks key → second guard (simulated post-restart) sees duplicate")
    void crossRestartDedupViaRedis() {
        StringRedisTemplate template = newTemplate();
        String key = "class-reschedule:cross-restart:" + System.nanoTime();

        EmailIdempotencyGuard before = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(before.markIfFirstSeen(key))
                .as("first forward pre-restart should proceed").isTrue();

        // Simulate kiteclass-core crash + restart by creating a fresh guard instance.
        EmailIdempotencyGuard after = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(after.markIfFirstSeen(key))
                .as("post-restart redelivery of identical event must be suppressed via Redis").isFalse();

        assertThat(after.markIfFirstSeen(key)).isFalse();
    }

    @Test
    @DisplayName("distinct keys after restart both proceed (no false positives)")
    void distinctKeysDoNotCollide() {
        StringRedisTemplate template = newTemplate();
        String original = "class-reschedule:a:" + System.nanoTime();
        String other = "class-reschedule:b:" + System.nanoTime();

        EmailIdempotencyGuard before = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(before.markIfFirstSeen(original)).isTrue();

        EmailIdempotencyGuard after = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(after.markIfFirstSeen(original)).as("same key still suppressed").isFalse();
        assertThat(after.markIfFirstSeen(other)).as("distinct key should proceed").isTrue();
    }

    @Test
    @DisplayName("Redis outage → guard fails open (Caffeine fallback path) — never drops a legit forward")
    void redisOutageFailsOpen() {
        LettuceConnectionFactory broken = new LettuceConnectionFactory("localhost", 1);
        broken.setShutdownTimeout(Duration.ZERO.toMillis());
        broken.afterPropertiesSet();
        StringRedisTemplate brokenTemplate = new StringRedisTemplate();
        brokenTemplate.setConnectionFactory(broken);
        brokenTemplate.afterPropertiesSet();

        EmailIdempotencyGuard guard = new EmailIdempotencyGuard(60, 50_000, brokenTemplate);
        String key = "class-reschedule:outage:" + System.nanoTime();

        assertThat(guard.markIfFirstSeen(key)).isTrue();
        assertThat(guard.markIfFirstSeen(key)).isFalse();
        EmailIdempotencyGuard restarted = new EmailIdempotencyGuard(60, 50_000, brokenTemplate);
        assertThat(restarted.markIfFirstSeen(key))
                .as("Redis outage + restart → fail-open (proceed) preferred over dropping").isTrue();

        try {
            broken.destroy();
        } catch (RuntimeException ignored) {
        }
    }
}
