package com.kitehub.email.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for {@link EmailIdempotencyGuard} (GAP-580 — consumer-side email idempotency).
 */
@DisplayName("EmailIdempotencyGuard — TTL dedup + deterministic key (GAP-580)")
class EmailIdempotencyGuardTest {

    private final EmailIdempotencyGuard guard = new EmailIdempotencyGuard(60, 50000);

    @Test
    @DisplayName("first markIfFirstSeen → true; subsequent same key → false")
    void firstSeenThenDuplicate() {
        String key = "welcome:vy@test.vn:2026-06-02";
        assertThat(guard.markIfFirstSeen(key)).isTrue();
        assertThat(guard.markIfFirstSeen(key)).isFalse();
        assertThat(guard.markIfFirstSeen(key)).isFalse();
    }

    @Test
    @DisplayName("distinct keys → both first-seen true")
    void distinctKeysBothFirstSeen() {
        assertThat(guard.markIfFirstSeen("k1")).isTrue();
        assertThat(guard.markIfFirstSeen("k2")).isTrue();
    }

    @Test
    @DisplayName("null/blank key → fail-open (always true, never dedups a keyless send)")
    void nullOrBlankKeyFailsOpen() {
        assertThat(guard.markIfFirstSeen(null)).isTrue();
        assertThat(guard.markIfFirstSeen(null)).isTrue();
        assertThat(guard.markIfFirstSeen("")).isTrue();
        assertThat(guard.markIfFirstSeen("   ")).isTrue();
    }

    @Test
    @DisplayName("explicit key takes precedence over derived hash")
    void explicitKeyWins() {
        String key = guard.computeKey("EXPLICIT-123", "a@test.vn", "welcome", "welcome", null);
        assertThat(key).isEqualTo("EXPLICIT-123");
    }

    @Test
    @DisplayName("derived key is deterministic regardless of variable map ordering")
    void derivedKeyDeterministicAcrossMapOrder() {
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("recipientName", "Vy");
        ordered.put("orgName", "Sao Mai");

        Map<String, Object> reversed = new TreeMap<>((x, y) -> y.compareTo(x));
        reversed.put("recipientName", "Vy");
        reversed.put("orgName", "Sao Mai");

        String k1 = guard.computeKey(null, "a@test.vn", "welcome", "welcome", ordered);
        String k2 = guard.computeKey(null, "a@test.vn", "welcome", "welcome", reversed);
        assertThat(k1).isEqualTo(k2);
    }

    @Test
    @DisplayName("derived key differs for distinct recipient / template / vars")
    void derivedKeyDiffersOnInputs() {
        String base = guard.computeKey(null, "a@test.vn", "welcome", "welcome", Map.of("n", "Vy"));
        assertThat(base).isNotEqualTo(guard.computeKey(null, "b@test.vn", "welcome", "welcome", Map.of("n", "Vy")));
        assertThat(base).isNotEqualTo(guard.computeKey(null, "a@test.vn", "signup", "welcome", Map.of("n", "Vy")));
        assertThat(base).isNotEqualTo(guard.computeKey(null, "a@test.vn", "welcome", "welcome", Map.of("n", "Hằng")));
    }

    @Test
    @DisplayName("concurrent redelivery storm of same key → exactly one first-seen true")
    void concurrentSameKeyExactlyOneWins() throws InterruptedException {
        String key = "concurrent:burst:k";
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger trueCount = new AtomicInteger();
        ConcurrentHashMap.KeySetView<Integer, Boolean> ignored = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (guard.markIfFirstSeen(key)) {
                    trueCount.incrementAndGet();
                }
                ignored.add(1);
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // putIfAbsent atomicity → exactly one thread proceeds, 49 suppressed.
        assertThat(trueCount.get()).isEqualTo(1);
    }
}
