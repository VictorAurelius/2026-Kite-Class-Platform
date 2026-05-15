package com.kitehub.subscription.betastatus.service;

import com.kitehub.subscription.betastatus.config.BetaStatusConfig;
import com.kitehub.subscription.betastatus.dto.BetaStatusKnownIssue;
import com.kitehub.subscription.betastatus.dto.BetaStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static MVP beta-status content loader (Wave 78 GAP-539).
 *
 * <p>Phase 1 BETA design choice (per api-contract.md): markdown content lives
 * in {@code src/main/resources/beta-status/beta-status.md} and is loaded once
 * at startup + cached. Admin updates content via git PR → rebuild → redeploy.
 * Live status page deferred to Wave 79+.</p>
 *
 * @since Wave 78 — GAP-539
 */
@Service
@Slf4j
public class BetaStatusService {

    private static final String RESOURCE_PATH = "beta-status/beta-status.md";
    private static final String FALLBACK_MARKDOWN = "# Trạng thái Beta KiteHub\n\n"
            + "Hiện tại không có thông tin cập nhật. Vui lòng quay lại sau hoặc liên hệ support@kitehub.me.\n";

    /** Cached content snapshot; loaded at first request and reused. */
    private final AtomicReference<BetaStatusResponse> cached = new AtomicReference<>();

    // GAP-555 (Wave 79 Bucket A): BetaStatusConfig wires kitehub.beta-status.*
    // keys (content-source / cache-ttl-seconds / rate-limit-per-min-per-ip).
    // Content-source key parsing TBD when admin-edit UI lands (Wave 80+); for
    // Phase 1 BETA the classpath resource path is hardcoded. Cache TTL not yet
    // honored here (in-process AtomicReference cache); rate-limit enforced at
    // gateway. Injecting for grep-discoverability + future wiring.
    private final BetaStatusConfig config;

    public BetaStatusService(BetaStatusConfig config) {
        this.config = config;
    }

    public BetaStatusResponse getStatus() {
        BetaStatusResponse existing = cached.get();
        if (existing != null) {
            return existing;
        }
        BetaStatusResponse loaded = loadFromResource();
        cached.compareAndSet(null, loaded);
        return cached.get();
    }

    private BetaStatusResponse loadFromResource() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String markdown = FALLBACK_MARKDOWN;
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            markdown = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("beta-status.md not found at classpath:{}; returning fallback content", RESOURCE_PATH);
        }
        List<BetaStatusKnownIssue> knownIssues = Collections.emptyList();
        return new BetaStatusResponse(
                "2026-05-14-v1",
                now,
                markdown,
                "OPERATIONAL",
                knownIssues
        );
    }
}
