package com.kitehub.branding.service;

import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.dto.BrandingJobMessage;
import com.kitehub.branding.lifecycle.InstanceLifecycleService;
import com.kitehub.branding.lifecycle.LifecycleState;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.outbox.BrandingEventEmitter;
import com.kitehub.branding.repository.BrandingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for BrandingJobService.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class BrandingJobServiceTest {

    @Mock
    private BrandingJobRepository jobRepository;

    @Mock
    private BrandingEventEmitter outboxEmitter;

    @Mock
    private InstanceLifecycleService lifecycleService;

    @Mock
    private BrandingInstanceStateRepository instanceStateRepository;

    @InjectMocks
    private BrandingJobService jobService;

    private UUID instanceId;
    private String organizationName;
    private String language;
    private String logoUrl;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        organizationName = "Test Organization";
        language = "vi";
        logoUrl = "https://s3.amazonaws.com/test-logo.png";
    }

    @Test
    void testCreateJob_Success() {
        // Given
        BrandingJob savedJob = new BrandingJob();
        savedJob.setId(UUID.randomUUID());
        savedJob.setInstanceId(instanceId);
        savedJob.setOrganizationName(organizationName);
        savedJob.setLanguage(language);
        savedJob.setLogoUrl(logoUrl);
        savedJob.setStatus(JobStatus.QUEUED);

        when(jobRepository.save(any(BrandingJob.class))).thenReturn(savedJob);

        // When
        BrandingJob result = jobService.createJob(instanceId, organizationName, language, logoUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInstanceId()).isEqualTo(instanceId);
        assertThat(result.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(result.getProgress()).isEqualTo(0);

        // Verify outbox emitter called with correct routing + payload
        ArgumentCaptor<BrandingJobMessage> messageCaptor = ArgumentCaptor.forClass(BrandingJobMessage.class);
        verify(outboxEmitter).emit(
                eq(savedJob.getId()),
                eq(instanceId),
                eq("branding.job.queued"),
                eq(RabbitMQConfig.BRANDING_EXCHANGE),
                eq(RabbitMQConfig.BRANDING_ROUTING_KEY),
                messageCaptor.capture()
        );

        BrandingJobMessage message = messageCaptor.getValue();
        assertThat(message.getJobId()).isEqualTo(savedJob.getId());
        assertThat(message.getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    void createJob_propagatesTierToMessage_GAP1137() {
        // GAP-1135/1137: 5-arg createJob carries subscription tier so the processor
        // can route FULL_AI (PREMIUM/ENTERPRISE) vs TEMPLATE.
        BrandingJob savedJob = new BrandingJob();
        savedJob.setId(UUID.randomUUID());
        savedJob.setInstanceId(instanceId);
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(savedJob);

        jobService.createJob(instanceId, organizationName, language, logoUrl, "PREMIUM");

        ArgumentCaptor<BrandingJobMessage> captor = ArgumentCaptor.forClass(BrandingJobMessage.class);
        verify(outboxEmitter).emit(any(), any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getTier()).isEqualTo("PREMIUM");
    }

    @Test
    void createJob_tierlessOverload_emitsNullTier_GAP1137() {
        // Legacy 4-arg overload (draft auto-create) → null tier → FREE/TEMPLATE.
        BrandingJob savedJob = new BrandingJob();
        savedJob.setId(UUID.randomUUID());
        savedJob.setInstanceId(instanceId);
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(savedJob);

        jobService.createJob(instanceId, organizationName, language, logoUrl);

        ArgumentCaptor<BrandingJobMessage> captor = ArgumentCaptor.forClass(BrandingJobMessage.class);
        verify(outboxEmitter).emit(any(), any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getTier()).isNull();
    }

    @Test
    void testUpdateJobProgress() {
        // Given
        UUID jobId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(job);

        // When
        jobService.updateJobProgress(jobId, JobStatus.PROCESSING, 50, "Generating images");

        // Then
        verify(jobRepository).save(argThat(savedJob ->
            savedJob.getStatus() == JobStatus.PROCESSING &&
            savedJob.getProgress() == 50 &&
            "Generating images".equals(savedJob.getCurrentStep())
        ));
    }

    @Test
    void testMarkJobFailed() {
        // Given
        UUID jobId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setStatus(JobStatus.PROCESSING);
        job.setRetryCount(0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(job);

        // When
        jobService.markJobFailed(jobId, "OpenAI API error");

        // Then
        verify(jobRepository).save(argThat(savedJob ->
            savedJob.getStatus() == JobStatus.FAILED &&
            "OpenAI API error".equals(savedJob.getErrorMessage()) &&
            savedJob.getRetryCount() == 1
        ));
    }

    @Test
    void testCancelJob_Success() {
        // Given
        UUID jobId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(instanceId);
        job.setStatus(JobStatus.QUEUED);

        when(jobRepository.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(job);

        // When
        boolean result = jobService.cancelJob(jobId, instanceId);

        // Then
        assertThat(result).isTrue();
        verify(jobRepository).save(argThat(savedJob ->
            savedJob.getStatus() == JobStatus.CANCELLED
        ));
    }

    @Test
    void testCreateJob_RoutesLifecycleTransitionThroughService() {
        BrandingJob savedJob = new BrandingJob();
        savedJob.setId(UUID.randomUUID());
        savedJob.setInstanceId(instanceId);
        savedJob.setStatus(JobStatus.QUEUED);

        when(jobRepository.save(any(BrandingJob.class))).thenReturn(savedJob);
        when(instanceStateRepository.findById(instanceId)).thenReturn(Optional.empty());

        jobService.createJob(instanceId, organizationName, language, logoUrl);

        // §6 hinge — instance transitions through service, NOT direct setState.
        verify(lifecycleService).transition(
            eq(instanceId), eq(LifecycleState.INITIALIZING), any(), any());
    }

    @Test
    void testCreateJob_OnDeployedInstanceTransitionsToRegenerating() {
        BrandingJob savedJob = new BrandingJob();
        savedJob.setId(UUID.randomUUID());
        savedJob.setInstanceId(instanceId);
        savedJob.setStatus(JobStatus.QUEUED);

        BrandingInstanceState deployed = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.DEPLOYED)
            .brandingVersion(1).regenerateCount(0).build();

        when(jobRepository.save(any(BrandingJob.class))).thenReturn(savedJob);
        when(instanceStateRepository.findById(instanceId)).thenReturn(Optional.of(deployed));

        jobService.createJob(instanceId, organizationName, language, logoUrl);

        verify(lifecycleService).transition(
            eq(instanceId), eq(LifecycleState.REGENERATING), any(), any());
    }

    @Test
    void testUpdateJobProgress_ProcessingDrivesGenerating() {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(instanceId);
        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(job);
        // GAP-1021 state-aware lifecycle: GENERATING is reachable only from
        // INITIALIZING, so the instance must already be INITIALIZING when the job
        // reaches PROCESSING (empty/null state would be skipped as not-reachable).
        BrandingInstanceState initializing = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.INITIALIZING)
            .brandingVersion(0).regenerateCount(0).build();
        when(instanceStateRepository.findById(instanceId)).thenReturn(Optional.of(initializing));

        jobService.updateJobProgress(jobId, JobStatus.PROCESSING, 50, "Generating");

        verify(lifecycleService).transition(
            eq(instanceId), eq(LifecycleState.GENERATING), any(), any());
    }

    @Test
    void testMarkJobFailedDrivesFailedTransition() {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(instanceId);
        job.setStatus(JobStatus.PROCESSING);
        job.setRetryCount(0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BrandingJob.class))).thenReturn(job);
        // GAP-1021 state-aware lifecycle: FAILED is reachable from GENERATING (the job
        // is PROCESSING ⇒ instance is GENERATING); a null state would be skipped.
        BrandingInstanceState generating = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.GENERATING)
            .brandingVersion(0).regenerateCount(0).build();
        when(instanceStateRepository.findById(instanceId)).thenReturn(Optional.of(generating));

        jobService.markJobFailed(jobId, "OpenAI API error");

        verify(lifecycleService).transition(
            eq(instanceId), eq(LifecycleState.FAILED), any(), any());
    }

    @Test
    void testCancelJob_AlreadyCompleted() {
        // Given
        UUID jobId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(instanceId);
        job.setStatus(JobStatus.COMPLETED);

        when(jobRepository.findByIdAndInstanceId(jobId, instanceId)).thenReturn(Optional.of(job));

        // When
        boolean result = jobService.cancelJob(jobId, instanceId);

        // Then
        assertThat(result).isFalse();
        verify(jobRepository, never()).save(any());
    }

    @Test
    void testCreateWizardJob_persistsOrgType() {
        // Given — GAP-1133: createWizardJob carries + persists the user-type axis.
        when(jobRepository.save(any(BrandingJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instanceStateRepository.findById(instanceId)).thenReturn(Optional.empty());

        // When
        BrandingJob result = jobService.createWizardJob(
                instanceId, organizationName, language, logoUrl, "LARGE_CENTER",
                "professional", "T1");

        // Then — orgType + tone + templateId set on the entity + persisted via save
        assertThat(result.getOrgType()).isEqualTo("LARGE_CENTER");
        assertThat(result.getTone()).isEqualTo("professional");
        assertThat(result.getTemplateId()).isEqualTo("T1");
        verify(jobRepository).save(argThat(saved -> "LARGE_CENTER".equals(saved.getOrgType())
                && "professional".equals(saved.getTone())));
    }

    @Test
    void testCreateWizardJob_nullOrgTypeOk() {
        // Given — orgType nullable for backward-compat (pre-GAP-1133).
        when(jobRepository.save(any(BrandingJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instanceStateRepository.findById(instanceId)).thenReturn(Optional.empty());

        // When
        BrandingJob result = jobService.createWizardJob(
                instanceId, organizationName, language, logoUrl, null, null, null);

        // Then
        assertThat(result.getOrgType()).isNull();
        assertThat(result.getTone()).isNull();
        assertThat(result.getStatus()).isEqualTo(JobStatus.QUEUED);
    }
}
