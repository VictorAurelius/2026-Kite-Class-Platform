package com.kitehub.subscription.betastatus.controller;

import com.kitehub.subscription.betastatus.dto.BetaStatusResponse;
import com.kitehub.subscription.betastatus.service.BetaStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Public Beta Status endpoint (Wave 78 GAP-539).
 *
 * <p>Schema source-of-truth:
 * {@code documents/01-business/kitehub/beta-status/api-contract.md}.</p>
 *
 * <p>Endpoint:</p>
 * <ul>
 *   <li>{@code GET /api/v1/beta-status} — public, cached 5 minutes</li>
 * </ul>
 *
 * @since Wave 78 — GAP-539
 */
@RestController
@RequestMapping("/api/v1/beta-status")
@RequiredArgsConstructor
@Tag(name = "BetaStatus", description = "Public beta status page content")
public class BetaStatusController {

    private final BetaStatusService service;

    @GetMapping
    @Operation(summary = "Get current beta status content (markdown payload, 5-min cache)")
    public ResponseEntity<BetaStatusResponse> getStatus() {
        BetaStatusResponse body = service.getStatus();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(body);
    }
}
