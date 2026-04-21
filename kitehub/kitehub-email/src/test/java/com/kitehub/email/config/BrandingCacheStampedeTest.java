package com.kitehub.email.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-043 — verifies that {@code Cache#get(key, Callable)} (the method Spring's
 * {@code @Cacheable(sync = true)} delegates to) coalesces concurrent misses onto
 * a single loader invocation.
 *
 * <p>Without request-coalescing, 10 concurrent lookups on an empty cache with a
 * slow loader would run the loader 10 times (cache stampede). Caffeine's
 * {@code Cache#get(K, Function)} guarantees per-key mutual exclusion of the loader,
 * so we expect the loader counter to equal exactly 1.
 *
 * <p>This is the same mechanism Spring wires under the hood when
 * {@code @Cacheable(sync = true)} is declared on a service method.
 */
class BrandingCacheStampedeTest {

    @Test
    void concurrentMisses_onEmptyCache_loaderRunsOnce() throws InterruptedException {
        CacheManager manager = new BrandingCacheConfig().brandingCacheManager();
        Cache cache = manager.getCache(BrandingCacheConfig.TENANT_BRANDING_CACHE);
        assertThat(cache).isNotNull();

        AtomicInteger loaderInvocations = new AtomicInteger(0);
        int concurrentCallers = 10;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(concurrentCallers);
        ExecutorService pool = Executors.newFixedThreadPool(concurrentCallers);

        try {
            for (int i = 0; i < concurrentCallers; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        // This is exactly what Spring's sync=true path invokes internally.
                        String result = cache.get("instance-42", () -> {
                            loaderInvocations.incrementAndGet();
                            // Simulate a slow DB/HTTP fetch so other threads pile up.
                            Thread.sleep(100);
                            return "branding-payload";
                        });
                        assertThat(result).isEqualTo("branding-payload");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean finishedInTime = doneGate.await(5, TimeUnit.SECONDS);
            assertThat(finishedInTime).as("all workers completed").isTrue();
        } finally {
            pool.shutdownNow();
        }

        // The cornerstone assertion of GAP-043 — no stampede.
        assertThat(loaderInvocations.get())
                .as("loader runs exactly once across %d concurrent callers", concurrentCallers)
                .isEqualTo(1);

        // Subsequent reads are pure cache hits.
        assertThat(cache.get("instance-42")).isNotNull();
        assertThat(cache.get("instance-42").get()).isEqualTo("branding-payload");
    }
}
