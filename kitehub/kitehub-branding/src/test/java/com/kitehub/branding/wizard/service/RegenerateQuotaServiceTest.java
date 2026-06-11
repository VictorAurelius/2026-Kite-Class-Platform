package com.kitehub.branding.wizard.service;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.RegenerateQuotaResponse;
import com.kitehub.branding.wizard.entity.BrandingRegenerateUsage;
import com.kitehub.branding.wizard.repository.BrandingRegenerateUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RegenerateQuotaService")
class RegenerateQuotaServiceTest {

    private BrandingRegenerateUsageRepository usageRepo;
    private BrandingJobRepository jobRepo;
    private RegenerateQuotaService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        usageRepo = mock(BrandingRegenerateUsageRepository.class);
        jobRepo = mock(BrandingJobRepository.class);
        fixedClock = Clock.fixed(Instant.parse("2026-05-07T10:30:00Z"), ZoneOffset.UTC);
        service = new RegenerateQuotaService(usageRepo, jobRepo, fixedClock);
        // Defaults from rules — would come from @Value in real wiring.
        ReflectionTestUtils.setField(service, "freeLimit", 3);
        ReflectionTestUtils.setField(service, "proLimit", 10);
        ReflectionTestUtils.setField(service, "premiumLimit", 30);
        ReflectionTestUtils.setField(service, "enterpriseLimit", -1);
    }

    @Test
    @DisplayName("limitFor — tier caps mirror ai-branding-guidelines.md §4.3")
    void tierLimitsMirrorRules() {
        assertThat(service.limitFor("FREE")).isEqualTo(3);
        // BASIC = canonical (GAP-1228); "PRO" alias backward-compat JWT cũ — cùng cap
        assertThat(service.limitFor("BASIC")).isEqualTo(10);
        assertThat(service.limitFor("PRO")).isEqualTo(10);
        assertThat(service.limitFor("PREMIUM")).isEqualTo(30);
        assertThat(service.limitFor("ENTERPRISE")).isEqualTo(-1);
        // Unknown tier → fail-safe FREE
        assertThat(service.limitFor("BANANA")).isEqualTo(3);
        assertThat(service.limitFor(null)).isEqualTo(3);
    }

    @Test
    @DisplayName("getQuota — empty usage → used=0, resetAt = next UTC midnight")
    void quotaWhenNoUsage() {
        when(usageRepo.findByUserIdAndWindowStart(any(), any())).thenReturn(Optional.empty());

        RegenerateQuotaResponse resp = service.getQuota("usr-1", "PRO");

        // GAP-1228: response trả tier CANONICAL — alias JWT cũ "PRO" → "BASIC"
        assertThat(resp.tier()).isEqualTo("BASIC");
        assertThat(resp.used()).isZero();
        assertThat(resp.limit()).isEqualTo(10);
        assertThat(resp.resetAt()).isEqualTo(Instant.parse("2026-05-08T00:00:00Z"));
    }

    @Test
    @DisplayName("getQuota — ENTERPRISE returns limit=-1 and resetAt=null")
    void enterpriseUnlimited() {
        when(usageRepo.findByUserIdAndWindowStart(any(), any())).thenReturn(Optional.empty());
        RegenerateQuotaResponse resp = service.getQuota("usr-1", "ENTERPRISE");
        assertThat(resp.limit()).isEqualTo(-1);
        assertThat(resp.resetAt()).isNull();
    }

    @Test
    @DisplayName("regenerate — quota exhausted → QuotaExceededException")
    void quotaExceeded() {
        UUID instanceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, instanceId, JobStatus.COMPLETED);
        when(jobRepo.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));

        BrandingRegenerateUsage atLimit = new BrandingRegenerateUsage();
        atLimit.setUserId("usr-1");
        atLimit.setUsedCount(3);
        atLimit.setWindowStart(java.time.LocalDateTime.parse("2026-05-07T00:00:00"));
        atLimit.setWindowEnd(java.time.LocalDateTime.parse("2026-05-08T00:00:00"));
        atLimit.setTier("FREE");
        when(usageRepo.findByUserIdAndWindowStart(any(), any())).thenReturn(Optional.of(atLimit));

        assertThatThrownBy(() ->
                service.regenerate(jobId, instanceId, "usr-1", "FREE", "key-1"))
                .isInstanceOf(RegenerateQuotaService.QuotaExceededException.class);
    }

    @Test
    @DisplayName("regenerate — wrong job status → InvalidJobStateException")
    void invalidJobState() {
        UUID instanceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, instanceId, JobStatus.PROCESSING);
        when(jobRepo.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));
        when(usageRepo.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.regenerate(jobId, instanceId, "usr-1", "FREE", "key-1"))
                .isInstanceOf(RegenerateQuotaService.InvalidJobStateException.class);
    }

    @Test
    @DisplayName("regenerate — happy path consumes 1 slot + flips status")
    void happyPath() {
        UUID instanceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, instanceId, JobStatus.COMPLETED);
        when(jobRepo.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));
        when(usageRepo.findByUserIdAndWindowStart(any(), any())).thenReturn(Optional.empty());
        when(usageRepo.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(usageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandingJob result = service.regenerate(jobId, instanceId, "usr-1", "PRO", "key-1");

        assertThat(result.getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(result.getProgress()).isZero();
    }

    @Test
    @DisplayName("regenerate — idempotent replay returns same job without consuming quota")
    void idempotentReplay() {
        UUID instanceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        BrandingJob existing = makeJob(jobId, instanceId, JobStatus.PROCESSING);
        BrandingRegenerateUsage prior = new BrandingRegenerateUsage();
        prior.setUserId("usr-1");
        prior.setIdempotencyKey("key-1");
        prior.setJobId(jobId);
        prior.setUsedCount(1);
        when(usageRepo.findByUserIdAndIdempotencyKey("usr-1", "key-1")).thenReturn(Optional.of(prior));
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(existing));

        BrandingJob result = service.regenerate(jobId, instanceId, "usr-1", "FREE", "key-1");

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("Wave 36 GAP-393-D — idempotent replay served from local Caffeine cache (1 DB query saved)")
    void idempotencyCacheServesReplay() {
        UUID instanceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, instanceId, JobStatus.COMPLETED);
        when(jobRepo.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));
        when(usageRepo.findByUserIdAndWindowStart(any(), any())).thenReturn(Optional.empty());
        when(usageRepo.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(usageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));

        // First call seeds the local idempotency cache.
        BrandingJob first = service.regenerate(jobId, instanceId, "usr-1", "PRO", "key-cache");
        assertThat(first).isNotNull();
        assertThat(service.idempotencyCacheSize()).isEqualTo(1L);

        // Second call hits the cache → no usageRepo.findByUserIdAndIdempotencyKey
        // beyond the first (cache short-circuits before the DB lookup).
        BrandingJob second = service.regenerate(jobId, instanceId, "usr-1", "PRO", "key-cache");
        assertThat(second).isSameAs(job);
        // Only the first regenerate call should have queried the idempotency table by key.
        verify(usageRepo, times(1)).findByUserIdAndIdempotencyKey(any(), any());
    }

    @Test
    @DisplayName("Wave 36 GAP-393-D — null idempotencyKey skips cache (no NPE)")
    void idempotencyCacheSkippedWhenKeyNull() {
        UUID instanceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, instanceId, JobStatus.COMPLETED);
        when(jobRepo.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));
        when(usageRepo.findByUserIdAndWindowStart(any(), any())).thenReturn(Optional.empty());
        when(usageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandingJob result = service.regenerate(jobId, instanceId, "usr-1", "PRO", null);

        assertThat(result.getStatus()).isEqualTo(JobStatus.PROCESSING);
        // Cache untouched — null key is the explicit "no idempotency" signal.
        assertThat(service.idempotencyCacheSize()).isZero();
        verify(usageRepo, never()).findByUserIdAndIdempotencyKey(any(), any());
    }

    private BrandingJob makeJob(UUID id, UUID instanceId, JobStatus status) {
        BrandingJob job = new BrandingJob();
        job.setId(id);
        job.setInstanceId(instanceId);
        job.setStatus(status);
        job.setProgress(0);
        job.setOrganizationName("Test School");
        job.setLanguage("vi");
        return job;
    }
}
