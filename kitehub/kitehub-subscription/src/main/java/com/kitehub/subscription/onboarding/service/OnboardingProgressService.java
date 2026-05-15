package com.kitehub.subscription.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.onboarding.config.OnboardingConfig;
import com.kitehub.subscription.onboarding.domain.OnboardingStepId;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressResponse;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressUpdateCommand;
import com.kitehub.subscription.onboarding.dto.OnboardingStepDto;
import com.kitehub.subscription.onboarding.entity.OnboardingProgress;
import com.kitehub.subscription.onboarding.repository.OnboardingProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for Day-1 onboarding checklist state (Wave 78 GAP-538).
 *
 * <p>Lazy-init: first call per tenant auto-creates a row with all steps
 * {@code completed=false}. Step state stored as JSONB so adding new
 * {@link OnboardingStepId} values stays backward-compatible — missing steps
 * are inserted with {@code completed=false} on read; unknown steps in the DB
 * row are filtered out.</p>
 *
 * <p>Idempotency: PUT with the same {@code stepId} + {@code completed} value
 * is a no-op (timestamps remain unchanged). Only state mutation refreshes
 * {@code lastUpdatedAt}.</p>
 *
 * @since Wave 78 — GAP-538
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingProgressService {

    private final OnboardingProgressRepository repository;
    private final ObjectMapper objectMapper;
    // GAP-555: wired but informational — OnboardingStepId enum already enforces
    // the step whitelist at compile time. Injecting OnboardingConfig keeps the
    // rules.md key grep-discoverable + allows FE-render lists to be sourced from
    // config without code change.
    @SuppressWarnings("unused") // future @Value wiring per GAP-555 (Wave 80+)
    private final OnboardingConfig onboardingConfig;

    /** GET — return current tenant's checklist; lazy-init on first call. */
    @Transactional
    public OnboardingProgressResponse getProgress(UUID tenantId) {
        OnboardingProgress entity = repository.findByTenantId(tenantId)
                .orElseGet(() -> repository.save(buildDefaultRow(tenantId)));
        return toResponse(entity);
    }

    /** PUT — apply step completion update; idempotent on equal value. */
    @Transactional
    public OnboardingProgressResponse updateStep(UUID tenantId, OnboardingProgressUpdateCommand command) {
        OnboardingProgress entity = repository.findByTenantId(tenantId)
                .orElseGet(() -> repository.save(buildDefaultRow(tenantId)));

        Map<OnboardingStepId, OnboardingStepDto> current = readSteps(entity.getStepsJson());
        OnboardingStepDto existing = current.get(command.stepId());
        boolean wasCompleted = existing != null && existing.completed();

        if (wasCompleted == command.completed()) {
            // Idempotent no-op — return current state without touching lastUpdatedAt.
            return toResponse(entity);
        }

        OffsetDateTime now = OffsetDateTime.now();
        OnboardingStepDto updated = new OnboardingStepDto(
                command.stepId(),
                command.completed(),
                command.completed() ? now : null
        );
        current.put(command.stepId(), updated);

        List<OnboardingStepDto> reordered = reorderForEnum(current);
        entity.setStepsJson(writeSteps(reordered));
        entity.setCompletionPercent(computePercent(reordered));
        entity.setLastUpdatedAt(now);
        repository.save(entity);

        return toResponse(entity);
    }

    // ── helpers ──

    private OnboardingProgress buildDefaultRow(UUID tenantId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<OnboardingStepDto> defaults = new ArrayList<>();
        for (OnboardingStepId id : OnboardingStepId.values()) {
            defaults.add(new OnboardingStepDto(id, false, null));
        }
        return OnboardingProgress.builder()
                .tenantId(tenantId)
                .stepsJson(writeSteps(defaults))
                .completionPercent(0)
                .createdAt(now)
                .lastUpdatedAt(now)
                .build();
    }

    private OnboardingProgressResponse toResponse(OnboardingProgress entity) {
        Map<OnboardingStepId, OnboardingStepDto> map = readSteps(entity.getStepsJson());
        List<OnboardingStepDto> ordered = reorderForEnum(map);
        int completed = (int) ordered.stream().filter(OnboardingStepDto::completed).count();
        return new OnboardingProgressResponse(
                entity.getTenantId(),
                computePercent(ordered),
                ordered.size(),
                completed,
                entity.getLastUpdatedAt(),
                ordered
        );
    }

    /** Reconcile DB state with enum: enum is the whitelist, missing → false, unknown → drop. */
    private List<OnboardingStepDto> reorderForEnum(Map<OnboardingStepId, OnboardingStepDto> map) {
        List<OnboardingStepDto> ordered = new ArrayList<>();
        for (OnboardingStepId id : OnboardingStepId.values()) {
            OnboardingStepDto dto = map.get(id);
            if (dto == null) {
                dto = new OnboardingStepDto(id, false, null);
            }
            ordered.add(dto);
        }
        return ordered;
    }

    private int computePercent(List<OnboardingStepDto> steps) {
        if (steps.isEmpty()) {
            return 0;
        }
        long completed = steps.stream().filter(OnboardingStepDto::completed).count();
        return (int) Math.round(100.0 * completed / steps.size());
    }

    private Map<OnboardingStepId, OnboardingStepDto> readSteps(String json) {
        Map<OnboardingStepId, OnboardingStepDto> map = new EnumMap<>(OnboardingStepId.class);
        if (json == null || json.isBlank()) {
            return map;
        }
        try {
            List<RawStep> raws = objectMapper.readValue(json, new TypeReference<>() {});
            for (RawStep raw : raws) {
                if (raw == null || raw.stepId == null) {
                    continue;
                }
                try {
                    OnboardingStepId id = OnboardingStepId.valueOf(raw.stepId);
                    map.put(id, new OnboardingStepDto(id, raw.completed, raw.completedAt));
                } catch (IllegalArgumentException ignored) {
                    // Unknown stepId in legacy/forward-compat row — skip silently.
                }
            }
        } catch (JsonProcessingException ex) {
            log.warn("Onboarding progress stepsJson malformed; rebuilding from defaults: {}", ex.getMessage());
        }
        return map;
    }

    private String writeSteps(List<OnboardingStepDto> steps) {
        try {
            List<Map<String, Object>> serializable = new ArrayList<>(steps.size());
            for (OnboardingStepDto step : steps) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("stepId", step.stepId().name());
                entry.put("completed", step.completed());
                entry.put("completedAt", step.completedAt() == null ? null : step.completedAt().toString());
                serializable.add(entry);
            }
            return objectMapper.writeValueAsString(serializable);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize onboarding steps", ex);
        }
    }

    private static class RawStep {
        public String stepId;
        public boolean completed;
        public OffsetDateTime completedAt;
    }
}
