package com.kitehub.branding.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.lifecycle.dto.DeployStatusResponse;
import com.kitehub.branding.lifecycle.dto.LifecycleEventDto;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Returns chronological lifecycle events for a branding instance per
 * api-contract.md §"GET /api/v1/branding/instances/{instanceId}/lifecycle/events"
 * plus a compact deploy-status summary per
 * §"GET /api/v1/branding/instances/{instanceId}/deploy-status" (GAP-1108).
 *
 * @since Wave 34 (GAP-272l)
 */
@RestController
@RequestMapping("/api/v1/branding/instances")
public class LifecycleEventsController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final String DEPLOY_MARKER = "deploy-completed";

    private final BrandingLifecycleEventRepository eventRepo;
    private final BrandingInstanceStateRepository stateRepo;
    private final ObjectMapper objectMapper;

    public LifecycleEventsController(BrandingLifecycleEventRepository eventRepo,
                                     BrandingInstanceStateRepository stateRepo,
                                     ObjectMapper objectMapper) {
        this.eventRepo = eventRepo;
        this.stateRepo = stateRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Compact deploy-status summary for the post-deploy {@code /branding} page
     * (GAP-1108). Combines the current lifecycle state with the latest
     * {@code deploy-completed} marker's {@code frontendUrl} / templateId / slug.
     */
    @GetMapping("/{instanceId}/deploy-status")
    public ResponseEntity<DeployStatusResponse> getDeployStatus(
        @PathVariable("instanceId") UUID instanceId) {

        BrandingInstanceState state = stateRepo.findById(instanceId).orElse(null);
        LifecycleState lifecycleState = state == null ? null : state.getState();
        boolean deployed = lifecycleState == LifecycleState.DEPLOYED;
        Integer brandingVersion = state == null ? null : state.getBrandingVersion();

        // Latest deploy-completed marker carries frontendUrl + templateId + slug.
        // Events are returned newest-first; pick the first deploy-completed row.
        List<BrandingLifecycleEvent> rows = eventRepo.findByInstanceIdSince(
            instanceId, LocalDateTime.now().minusYears(5), PageRequest.of(0, MAX_LIMIT));
        Optional<BrandingLifecycleEvent> marker = rows.stream()
            .filter(e -> DEPLOY_MARKER.equals(e.getEventType()))
            .findFirst();

        String frontendUrl = null;
        String templateId = null;
        String slug = null;
        LocalDateTime deployedAt = null;
        if (marker.isPresent()) {
            BrandingLifecycleEvent e = marker.get();
            deployedAt = e.getOccurredAt();
            JsonNode meta = readMeta(e.getMetadataJson());
            frontendUrl = textOrNull(meta, "frontendUrl");
            templateId = textOrNull(meta, "templateId");
            slug = textOrNull(meta, "slug");
        }

        return ResponseEntity.ok(new DeployStatusResponse(
            instanceId,
            lifecycleState == null ? null : lifecycleState.name(),
            deployed,
            frontendUrl,
            templateId,
            slug,
            brandingVersion,
            deployedAt));
    }

    private JsonNode readMeta(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(metadataJson);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
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
