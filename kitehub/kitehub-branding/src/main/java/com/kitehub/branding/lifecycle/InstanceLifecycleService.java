package com.kitehub.branding.lifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository;
import com.kitehub.branding.outbox.BrandingEventEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Single source of truth for branding instance lifecycle transitions per
 * {@code ai-branding-guidelines.md} §6.
 *
 * <p>State Machine pattern (per {@code design-patterns.md} §3.3) — every state
 * change must flow through {@link #transition(UUID, LifecycleState, Actor, Map)}.
 * Direct mutation of {@link BrandingInstanceState#setState(LifecycleState)}
 * outside this service is BANNED — callers refactored in same PR.</p>
 *
 * <p>Each transition:
 * <ol>
 *   <li>Validates source → target legality (throws {@link IllegalStateException}).</li>
 *   <li>Persists the new state row (creating it on first transition).</li>
 *   <li>Appends a {@link BrandingLifecycleEvent} (audit trail).</li>
 *   <li>Publishes a RabbitMQ {@code branding.lifecycle.transition} event via
 *       {@link BrandingEventEmitter} (outbox-first per {@code §3.5.1 Exception A}).</li>
 * </ol>
 *
 * <p>Audit option chosen: <b>α + γ combined</b> — events table doubles as
 * audit log AND RabbitMQ event published for downstream consumers.</p>
 *
 * @since Wave 34 (GAP-272l)
 */
@Slf4j
@Service
public class InstanceLifecycleService {

    private final BrandingInstanceStateRepository stateRepo;
    private final BrandingLifecycleEventRepository eventRepo;
    private final BrandingEventEmitter outboxEmitter;
    private final ObjectMapper objectMapper;

    public InstanceLifecycleService(BrandingInstanceStateRepository stateRepo,
                                    BrandingLifecycleEventRepository eventRepo,
                                    BrandingEventEmitter outboxEmitter,
                                    ObjectMapper objectMapper) {
        this.stateRepo = stateRepo;
        this.eventRepo = eventRepo;
        this.outboxEmitter = outboxEmitter;
        this.objectMapper = objectMapper;
    }

    /**
     * Move {@code instanceId} into {@code target} state.
     *
     * @param instanceId tenant instance identifier
     * @param target     desired next state
     * @param actor      who/what initiated (system/user/admin)
     * @param metadata   free-form payload merged into the event row + RabbitMQ message
     * @return the persisted current-state row after transition
     * @throws IllegalStateException when the transition is not allowed by §6
     */
    @Transactional
    public BrandingInstanceState transition(UUID instanceId,
                                            LifecycleState target,
                                            Actor actor,
                                            Map<String, Object> metadata) {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId required");
        }
        if (target == null) {
            throw new IllegalArgumentException("target state required");
        }

        BrandingInstanceState current = stateRepo.findById(instanceId).orElse(null);
        LifecycleState from = current == null ? null : current.getState();

        if (!target.isReachableFrom(from)) {
            throw new IllegalStateException(
                "INVALID_TRANSITION: instance=" + instanceId
                    + " from=" + from + " to=" + target);
        }

        if (current == null) {
            current = BrandingInstanceState.builder()
                .instanceId(instanceId)
                .state(target)
                .brandingVersion(target == LifecycleState.DEPLOYED ? 1 : 0)
                .regenerateCount(0)
                .build();
        } else {
            current.setState(target);
            if (target == LifecycleState.REGENERATING) {
                current.setRegenerateCount(current.getRegenerateCount() + 1);
            }
            if (target == LifecycleState.DEPLOYED && from == LifecycleState.REGENERATING) {
                current.setBrandingVersion(current.getBrandingVersion() + 1);
            }
            if (target == LifecycleState.DEPLOYED && from == LifecycleState.GENERATING
                && current.getBrandingVersion() == 0) {
                current.setBrandingVersion(1);
            }
            if (target == LifecycleState.FAILED && metadata != null) {
                Object reason = metadata.get("reason");
                if (reason != null) {
                    current.setLastFailureReason(String.valueOf(reason));
                }
            }
        }

        current = stateRepo.save(current);

        Map<String, Object> meta = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        BrandingLifecycleEvent event = BrandingLifecycleEvent.builder()
            .instanceId(instanceId)
            .eventType("state-change")
            .fromState(from)
            .toState(target)
            .actorKind(actor == null ? "system" : actor.kind())
            .actorId(actor == null ? "unknown" : actor.id())
            .metadataJson(serialize(meta))
            .build();
        event = eventRepo.save(event);

        // Publish via outbox (reliability net) + best-effort fast-path.
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.getId());
        payload.put("instanceId", instanceId);
        payload.put("fromState", from == null ? null : from.name());
        payload.put("toState", target.name());
        payload.put("actorKind", event.getActorKind());
        payload.put("actorId", event.getActorId());
        payload.put("metadata", meta);

        outboxEmitter.emit(
            instanceId,
            instanceId,
            "branding.lifecycle.transition",
            RabbitMQConfig.BRANDING_EXCHANGE,
            "branding.lifecycle.transition",
            payload);

        log.info("Lifecycle transition instance={} {} -> {} actor={}/{}",
            instanceId, from, target, event.getActorKind(), event.getActorId());

        return current;
    }

    /**
     * Append a non-state-changing audit-relevant marker (e.g.
     * {@code regenerate-requested}, {@code quality-score-computed},
     * {@code deploy-completed}).
     *
     * <p>Runs in its OWN physical transaction ({@code REQUIRES_NEW}) per
     * {@code audit-service-isolation.md} §3.11 — a marker is a best-effort audit
     * side-effect, so a failed marker INSERT must NOT mark a calling transaction
     * rollback-only and poison its commit ({@code UnexpectedRollbackException}).
     * Callers additionally wrap the call in try/catch (GAP-1107 #1 hardening —
     * best-effort isolation of the deploy-completed marker write).</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BrandingLifecycleEvent recordMarker(UUID instanceId,
                                               String eventType,
                                               Actor actor,
                                               Map<String, Object> metadata) {
        BrandingInstanceState current = stateRepo.findById(instanceId).orElse(null);
        LifecycleState currentState = current == null ? null : current.getState();
        Map<String, Object> meta = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        BrandingLifecycleEvent event = BrandingLifecycleEvent.builder()
            .instanceId(instanceId)
            .eventType(eventType)
            .fromState(currentState)
            .toState(currentState)
            .actorKind(actor == null ? "system" : actor.kind())
            .actorId(actor == null ? "unknown" : actor.id())
            .metadataJson(serialize(meta))
            .build();
        return eventRepo.save(event);
    }

    private String serialize(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize lifecycle metadata: {}", ex.getMessage());
            return "{}";
        }
    }

    /** Actor descriptor — kind ∈ {system, user, admin}. */
    public record Actor(String kind, String id) {
        public static Actor system(String systemId) {
            return new Actor("system", systemId == null ? "unknown" : systemId);
        }

        public static Actor user(String userId) {
            return new Actor("user", userId == null ? "anonymous" : userId);
        }

        public static Actor admin(String adminId) {
            return new Actor("admin", adminId == null ? "anonymous" : adminId);
        }
    }
}
