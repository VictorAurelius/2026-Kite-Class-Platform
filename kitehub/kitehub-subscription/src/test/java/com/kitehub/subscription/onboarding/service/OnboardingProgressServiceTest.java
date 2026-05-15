package com.kitehub.subscription.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kitehub.subscription.onboarding.config.OnboardingConfig;
import com.kitehub.subscription.onboarding.domain.OnboardingStepId;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressResponse;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressUpdateCommand;
import com.kitehub.subscription.onboarding.entity.OnboardingProgress;
import com.kitehub.subscription.onboarding.repository.OnboardingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnboardingProgressService} — lazy-init, idempotency,
 * percent computation, enum reconciliation (Wave 78 GAP-538).
 */
@DisplayName("OnboardingProgressService")
class OnboardingProgressServiceTest {

    private OnboardingProgressRepository repository;
    private OnboardingProgressService service;
    private final UUID tenantId = UUID.fromString("aaaa1111-bbbb-2222-cccc-333333333333");

    @BeforeEach
    void setUp() {
        repository = mock(OnboardingProgressRepository.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        OnboardingConfig onboardingConfig = new OnboardingConfig(
                "PROFILE_SETUP,INVITE_TEAM,IMPORT_DATA,CREATE_FIRST_CLASS,EXPLORE_FEATURES", 60);
        service = new OnboardingProgressService(repository, mapper, onboardingConfig);
    }

    @Test
    @DisplayName("GET — lazy-inits row when none exists, returns 5 steps all false")
    void getLazyInits() {
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(repository.save(any(OnboardingProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        OnboardingProgressResponse response = service.getProgress(tenantId);

        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.totalSteps()).isEqualTo(5);
        assertThat(response.completedSteps()).isZero();
        assertThat(response.completionPercent()).isZero();
        assertThat(response.steps()).hasSize(5);
        assertThat(response.steps().get(0).stepId()).isEqualTo(OnboardingStepId.PROFILE_SETUP);
        assertThat(response.steps()).allMatch(s -> !s.completed());
    }

    @Test
    @DisplayName("PUT — marking step completed updates percent + completedAt")
    void putMarksStepCompleted() {
        OnboardingProgress row = OnboardingProgress.builder()
                .tenantId(tenantId)
                .stepsJson("[]")
                .completionPercent(0)
                .build();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(row));
        when(repository.save(any(OnboardingProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        OnboardingProgressResponse response = service.updateStep(tenantId,
                new OnboardingProgressUpdateCommand(OnboardingStepId.PROFILE_SETUP, true));

        assertThat(response.completionPercent()).isEqualTo(20);
        assertThat(response.completedSteps()).isEqualTo(1);
        assertThat(response.steps().get(0).completed()).isTrue();
        assertThat(response.steps().get(0).completedAt()).isNotNull();
    }

    @Test
    @DisplayName("PUT — idempotent no-op when value unchanged")
    void putIdempotentNoOp() {
        // First seed row with PROFILE_SETUP already true.
        OnboardingProgress row = OnboardingProgress.builder()
                .tenantId(tenantId)
                .stepsJson("[{\"stepId\":\"PROFILE_SETUP\",\"completed\":true,\"completedAt\":\"2026-05-01T00:00:00Z\"}]")
                .completionPercent(20)
                .build();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(row));

        OnboardingProgressResponse response = service.updateStep(tenantId,
                new OnboardingProgressUpdateCommand(OnboardingStepId.PROFILE_SETUP, true));

        assertThat(response.steps().get(0).completed()).isTrue();
        // Idempotent path doesn't save → percent reflects existing row.
        assertThat(response.completionPercent()).isEqualTo(20);
    }

    @Test
    @DisplayName("PUT — un-checking step decrements percent")
    void putUncheckDecrementsPercent() {
        OnboardingProgress row = OnboardingProgress.builder()
                .tenantId(tenantId)
                .stepsJson("[{\"stepId\":\"PROFILE_SETUP\",\"completed\":true,\"completedAt\":\"2026-05-01T00:00:00Z\"}]")
                .completionPercent(20)
                .build();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(row));
        when(repository.save(any(OnboardingProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        OnboardingProgressResponse response = service.updateStep(tenantId,
                new OnboardingProgressUpdateCommand(OnboardingStepId.PROFILE_SETUP, false));

        assertThat(response.completionPercent()).isZero();
        assertThat(response.steps().get(0).completed()).isFalse();
        assertThat(response.steps().get(0).completedAt()).isNull();
    }

    @Test
    @DisplayName("GET — unknown step in legacy DB row is filtered out")
    void getFiltersUnknownStep() {
        OnboardingProgress row = OnboardingProgress.builder()
                .tenantId(tenantId)
                .stepsJson("[{\"stepId\":\"LEGACY_UNKNOWN_STEP\",\"completed\":true,\"completedAt\":null}]")
                .completionPercent(0)
                .build();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(row));

        OnboardingProgressResponse response = service.getProgress(tenantId);

        // 5 enum steps returned; unknown skipped; none completed (LEGACY ignored).
        assertThat(response.steps()).hasSize(5);
        assertThat(response.completedSteps()).isZero();
    }
}
