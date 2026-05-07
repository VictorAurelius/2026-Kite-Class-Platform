package com.kitehub.branding.lifecycle.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Wire shape for {@code GET /api/v1/branding/instances/{instanceId}/lifecycle/events}
 * matching {@code documents/01-business/kitehub/ai-branding/api-contract.md}.
 */
public record LifecycleEventDto(
    String id,
    LocalDateTime ts,
    String type,
    String fromState,
    String toState,
    Actor actor,
    JsonNode metadata) {

    public record Actor(String kind, String id) {}

    public static LifecycleEventDto from(BrandingLifecycleEvent e, ObjectMapper mapper) {
        JsonNode meta;
        try {
            meta = (e.getMetadataJson() == null || e.getMetadataJson().isBlank())
                ? mapper.createObjectNode()
                : mapper.readTree(e.getMetadataJson());
        } catch (Exception ex) {
            meta = mapper.createObjectNode();
        }
        return new LifecycleEventDto(
            e.getId().toString(),
            e.getOccurredAt(),
            e.getEventType(),
            e.getFromState() == null ? null : e.getFromState().name(),
            e.getToState() == null ? null : e.getToState().name(),
            new Actor(e.getActorKind(), e.getActorId()),
            meta);
    }

    public record EventsResponse(
        UUID instanceId,
        java.util.List<LifecycleEventDto> events,
        String nextCursor) {}
}
