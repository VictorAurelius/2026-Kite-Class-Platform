package com.kitehub.branding.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.lifecycle.dto.LifecycleEventDto;
import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;
import com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Returns chronological lifecycle events for a branding instance per
 * api-contract.md §"GET /api/v1/branding/instances/{instanceId}/lifecycle/events".
 *
 * @since Wave 34 (GAP-272l)
 */
@RestController
@RequestMapping("/api/v1/branding/instances")
public class LifecycleEventsController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final BrandingLifecycleEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public LifecycleEventsController(BrandingLifecycleEventRepository eventRepo,
                                     ObjectMapper objectMapper) {
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{instanceId}/lifecycle/events")
    public ResponseEntity<LifecycleEventDto.EventsResponse> getEvents(
        @PathVariable("instanceId") UUID instanceId,
        @RequestParam(value = "since", required = false) String since,
        @RequestParam(value = "limit", required = false) Integer limit,
        @RequestParam(value = "cursor", required = false) String cursor) {

        LocalDateTime sinceTs = parseSince(since);
        int effectiveLimit = clampLimit(limit);

        List<BrandingLifecycleEvent> rows = eventRepo.findByInstanceIdSince(
            instanceId, sinceTs, PageRequest.of(0, effectiveLimit));

        List<LifecycleEventDto> dtos = rows.stream()
            .map(e -> LifecycleEventDto.from(e, objectMapper))
            .toList();

        // Cursor pagination not implemented in v1 — events per instance ≤200 typical;
        // returning null keeps contract-compliant.
        return ResponseEntity.ok(new LifecycleEventDto.EventsResponse(instanceId, dtos, null));
    }

    private LocalDateTime parseSince(String since) {
        if (since == null || since.isBlank()) {
            return LocalDateTime.now().minusDays(30);
        }
        try {
            return OffsetDateTime.parse(since).toLocalDateTime();
        } catch (Exception ex) {
            return LocalDateTime.now().minusDays(30);
        }
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
