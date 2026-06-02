package com.kitehub.email.service;

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
 * Cross-restart integration coverage for {@link EmailIdempotencyGuard} (GAP-580 — closes
 * the carried-forward Wave phase2-beta acceptance criterion that the original Caffeine-only
 * guard could not satisfy: idempotency state survives a {@code kitehub-email} process restart
 * because the underlying Redis store outlives in-process memory).
 *
 * <p>Strategy: spin up a Redis Testcontainer once for the class, build a fresh
 * {@link StringRedisTemplate} pointing at it, then simulate a process restart by constructing
 * a SECOND {@link EmailIdempotencyGuard} instance against the same Redis container. The first
 * guard marks the key; the second guard — representing the post-restart process — sees the
 * key already in Redis and suppresses the duplicate.</p>
 */
@Testcontainers
@DisplayName("EmailIdempotencyGuard — Redis cross-restart dedup (GAP-580)")
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
        String key = "welcome:cross-restart:" + System.nanoTime();

        // Guard #1 — original process. Markds key in Redis with 60-min TTL.
        EmailIdempotencyGuard before = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(before.markIfFirstSeen(key))
                .as("first send pre-restart should proceed").isTrue();

        // Simulate kitehub-email crash + restart by creating a fresh guard instance against the
        // same Redis container. The new in-process Caffeine cache starts empty (would
        // previously allow a duplicate send), but Redis still holds the dedup key.
        EmailIdempotencyGuard after = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(after.markIfFirstSeen(key))
                .as("post-restart redelivery of identical event must be suppressed via Redis").isFalse();

        // Repeated checks after restart also block — TTL not expired.
        assertThat(after.markIfFirstSeen(key)).isFalse();
    }

    @Test
    @DisplayName("distinct keys after restart both proceed (no false positives)")
    void distinctKeysDoNotCollide() {
        StringRedisTemplate template = newTemplate();
        String original = "welcome:a:" + System.nanoTime();
        String other = "welcome:b:" + System.nanoTime();

        EmailIdempotencyGuard before = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(before.markIfFirstSeen(original)).isTrue();

        EmailIdempotencyGuard after = new EmailIdempotencyGuard(60, 50_000, template);
        assertThat(after.markIfFirstSeen(original)).as("same key still suppressed").isFalse();
        assertThat(after.markIfFirstSeen(other)).as("distinct key should proceed").isTrue();
    }

    @Test
    @DisplayName("Redis outage → guard fails open (Caffeine fallback path) — never drops a legit send")
    void redisOutageFailsOpen() {
        // Build a connection factory pointing at a closed port so every Redis op throws.
        LettuceConnectionFactory broken = new LettuceConnectionFactory("localhost", 1);
        broken.setShutdownTimeout(Duration.ZERO.toMillis());
        broken.afterPropertiesSet();
        StringRedisTemplate brokenTemplate = new StringRedisTemplate();
        brokenTemplate.setConnectionFactory(broken);
        brokenTemplate.afterPropertiesSet();

        EmailIdempotencyGuard guard = new EmailIdempotencyGuard(60, 50_000, brokenTemplate);
        String key = "welcome:outage:" + System.nanoTime();

        // First call falls back to Caffeine → first-seen true.
        assertThat(guard.markIfFirstSeen(key)).isTrue();
        // Same instance + same key → Caffeine suppresses duplicate in-process.
        assertThat(guard.markIfFirstSeen(key)).isFalse();
        // New instance after "restart" cannot see Caffeine state → fail-open allows the send
        // (we accept the duplicate-send risk during a Redis outage in exchange for not
        // silently dropping legitimate emails). Documenting the contract.
        EmailIdempotencyGuard restarted = new EmailIdempotencyGuard(60, 50_000, brokenTemplate);
        assertThat(restarted.markIfFirstSeen(key))
                .as("Redis outage + restart → fail-open (proceed) preferred over dropping").isTrue();

        // Discard connection factory; tolerate failure to avoid masking other test errors.
        try {
            broken.destroy();
        } catch (RuntimeException ignored) {
            // Best-effort cleanup of broken Lettuce factory.
        }
    }

}
