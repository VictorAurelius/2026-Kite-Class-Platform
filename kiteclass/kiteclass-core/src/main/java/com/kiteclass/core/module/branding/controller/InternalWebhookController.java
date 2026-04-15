package com.kiteclass.core.module.branding.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.branding.service.CachingBrandingPackageProxy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal webhooks — called by trusted KiteHub services to signal cross-service events.
 *
 * <p>All routes under {@code /internal/**} should be filtered to internal network by the
 * gateway (existing {@code InternalRequestFilter}).
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4)
 */
@Slf4j
@RestController
@RequestMapping("/internal/notify")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Internal-only webhook endpoints")
public class InternalWebhookController {

    private final CachingBrandingPackageProxy packageProxy;

    /**
     * Evict branding package cache when a downstream service reports a new deployment.
     * Invoked by the outbox RabbitMQ dispatcher (future Sub-PR), and usable by ops for
     * manual invalidation.
     */
    @PostMapping("/instance-deployed")
    @Operation(summary = "Notify that an instance has deployed — evicts package cache")
    public ApiResponse<String> instanceDeployed(
            @RequestParam @NotNull Long instanceId,
            @RequestHeader(value = "X-Internal-Caller", required = false) String caller) {
        log.info("[webhook] instance-deployed id={} caller={}", instanceId, caller);
        packageProxy.evict(instanceId);
        return ApiResponse.success("evicted", "branding-package cache evicted for instance " + instanceId);
    }
}
