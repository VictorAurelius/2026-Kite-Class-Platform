package com.kiteclass.core.module.instance;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanceLifecycleServiceTest {

    @Mock
    private FrontendInstanceRepository repository;

    @Mock
    private OutboxEventWriter outbox;

    @InjectMocks
    private InstanceLifecycleService service;

    private FrontendInstance deployedInstance() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1")
                .slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0)
                .brandingVersion(0)
                .build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.transitionTo(FrontendInstanceStatus.DEPLOYED);
        i.setId(42L);
        return i;
    }

    @Test
    void initiate_rejects_duplicate_slug() {
        when(repository.existsBySlugAndDeletedFalse("acme")).thenReturn(true);

        assertThatThrownBy(() -> service.initiate("t-1", "acme"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void initiate_transitions_to_initializing() {
        when(repository.existsBySlugAndDeletedFalse("acme")).thenReturn(false);
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.initiate("t-1", "acme");

        assertThat(result.getStatus()).isEqualTo(FrontendInstanceStatus.INITIALIZING);
        assertThat(result.getTenantSlug()).isEqualTo("t-1");
    }

    @Test
    void markInfrastructureReady_moves_to_generating() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.markInfrastructureReady(7L);

        assertThat(result.getStatus()).isEqualTo(FrontendInstanceStatus.GENERATING);
    }

    @Test
    void markBrandingCompleted_sets_url_and_deployed() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.setId(9L);
        when(repository.findById(9L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.markBrandingCompleted(9L, "https://acme.kiteclass.com");

        assertThat(result.getStatus()).isEqualTo(FrontendInstanceStatus.DEPLOYED);
        assertThat(result.getFrontendUrl()).isEqualTo("https://acme.kiteclass.com");
        assertThat(result.getBrandingVersion()).isEqualTo(1);
    }

    @Test
    void rebrand_from_deployed_moves_to_regenerating() {
        FrontendInstance i = deployedInstance();
        when(repository.findById(42L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.rebrand(42L);

        assertThat(result.getStatus()).isEqualTo(FrontendInstanceStatus.REGENERATING);
    }

    @Test
    void markFailed_from_generating_records_reason() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.setId(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.markFailed(11L, "AI provider 500");

        assertThat(result.getStatus()).isEqualTo(FrontendInstanceStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("AI provider 500");
        assertThat(result.getRetryCount()).isEqualTo(1);
    }

    @Test
    void retry_from_failed_moves_to_initializing() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.FAILED);
        i.setId(13L);
        when(repository.findById(13L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.retry(13L);

        assertThat(result.getStatus()).isEqualTo(FrontendInstanceStatus.INITIALIZING);
    }

    @Test
    void retry_blocks_after_max_retries() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.FAILED)
                .retryCount(InstanceLifecycleService.MAX_RETRIES)
                .brandingVersion(0).build();
        i.setId(15L);
        when(repository.findById(15L)).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.retry(15L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAX_RETRIES");
    }

    @Test
    void load_missing_id_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markInfrastructureReady(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void markBrandingCompleted_without_url_keeps_existing() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .frontendUrl("https://existing.kiteclass.com")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.setId(17L);
        when(repository.findById(17L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        FrontendInstance result = service.markBrandingCompleted(17L, null);

        assertThat(result.getFrontendUrl()).isEqualTo("https://existing.kiteclass.com");
    }

    @Test
    void initiate_emits_initializing_event_to_outbox() {
        when(repository.existsBySlugAndDeletedFalse("acme")).thenReturn(false);
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> {
            FrontendInstance saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        service.initiate("t-1", "acme");

        verify(outbox).enqueue(eq("instance.initializing"), eq("FrontendInstance"),
                eq("100"), anyString());
    }

    @Test
    void markBrandingCompleted_emits_deployed_event_with_escaped_payload() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-\"quoted\"").slug("ac\\me")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.setId(9L);
        when(repository.findById(9L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markBrandingCompleted(9L, "https://acme.kiteclass.com");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outbox).enqueue(eq("instance.deployed"), eq("FrontendInstance"),
                eq("9"), payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"status\":\"DEPLOYED\"");
        assertThat(payload).contains("\"brandingVersion\":1");
        assertThat(payload).contains("t-\\\"quoted\\\"");
        assertThat(payload).contains("ac\\\\me");
    }

    @Test
    void markFailed_emits_failed_event_with_reason_ready_payload() {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.setId(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(i));
        when(repository.save(any(FrontendInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markFailed(11L, "AI timeout");

        verify(outbox).enqueue(eq("instance.failed"), eq("FrontendInstance"),
                eq("11"), anyString());
    }
}
