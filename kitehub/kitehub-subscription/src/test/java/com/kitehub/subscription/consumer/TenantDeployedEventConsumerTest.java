package com.kitehub.subscription.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.InstanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenantDeployedEventConsumer} (Wave provisioning-1 Bucket C — GAP-948).
 *
 * <p>Verifies owner-resolution + send contract:
 * <ul>
 *   <li>valid payload + resolvable instance with contactEmail → sendTenantReadyEmail invoked</li>
 *   <li>unknown instance → no send (swallow + ACK)</li>
 *   <li>instance without contactEmail → no send</li>
 *   <li>unparseable tenantId → no send</li>
 *   <li>malformed JSON → swallow (no send, no throw)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TenantDeployedEventConsumerTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private EmailServiceClient emailServiceClient;

    @Mock
    private InstanceService instanceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TenantDeployedEventConsumer consumer() {
        return new TenantDeployedEventConsumer(
                objectMapper, instanceRepository, emailServiceClient, instanceService);
    }

    private static String payload(String tenantId) {
        return "{\"tenantId\":\"" + tenantId + "\",\"slug\":\"acme-school\",\"frontendInstanceId\":42}";
    }

    /** Instance is a JPA entity (no @Builder) — build via Lombok setters (id from BaseEntity). */
    private static Instance instance(UUID id, String contactEmail, String orgName, String subdomain) {
        Instance instance = new Instance();
        instance.setId(id);
        instance.setContactEmail(contactEmail);
        instance.setOrganizationName(orgName);
        instance.setSubdomain(subdomain);
        return instance;
    }

    @Test
    void handle_resolvableInstance_sendsTenantReadyEmail() {
        UUID id = UUID.randomUUID();
        when(instanceRepository.findById(id))
                .thenReturn(Optional.of(instance(id, "owner@acme.edu.vn", "Acme School", "acme")));

        consumer().handle(payload(id.toString()));

        verify(instanceService).markProvisioned(id);
        verify(emailServiceClient).sendTenantReadyEmail(
                eq(id), eq("owner@acme.edu.vn"), eq("Acme School"), eq("acme"));
    }

    @Test
    void handle_resolvableInstance_flipsStatusBeforeEmail() {
        // GAP-945: tenant.deployed must flip Instance PENDING → TRIAL via InstanceService.
        UUID id = UUID.randomUUID();
        when(instanceRepository.findById(id))
                .thenReturn(Optional.of(instance(id, "owner@acme.edu.vn", "Acme School", "acme")));

        consumer().handle(payload(id.toString()));

        verify(instanceService).markProvisioned(id);
    }

    @Test
    void handle_markProvisionedThrows_stillSendsEmail() {
        // GAP-945: status-flip failure is best-effort — must not abort the email send.
        UUID id = UUID.randomUUID();
        when(instanceRepository.findById(id))
                .thenReturn(Optional.of(instance(id, "owner@acme.edu.vn", "Acme School", "acme")));
        doThrow(new RuntimeException("db down")).when(instanceService).markProvisioned(id);

        assertThatCode(() -> consumer().handle(payload(id.toString()))).doesNotThrowAnyException();

        verify(emailServiceClient).sendTenantReadyEmail(
                eq(id), eq("owner@acme.edu.vn"), eq("Acme School"), eq("acme"));
    }

    @Test
    void handle_unknownInstance_doesNotSend() {
        UUID id = UUID.randomUUID();
        when(instanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatCode(() -> consumer().handle(payload(id.toString()))).doesNotThrowAnyException();

        verify(emailServiceClient, never()).sendTenantReadyEmail(any(), any(), any(), any());
    }

    @Test
    void handle_instanceWithoutContactEmail_doesNotSend() {
        UUID id = UUID.randomUUID();
        when(instanceRepository.findById(id))
                .thenReturn(Optional.of(instance(id, "  ", "Acme School", "acme")));

        consumer().handle(payload(id.toString()));

        // GAP-945: status MUST still flip even when contactEmail is missing.
        verify(instanceService).markProvisioned(id);
        verify(emailServiceClient, never()).sendTenantReadyEmail(any(), any(), any(), any());
    }

    @Test
    void handle_unparseableTenantId_doesNotSend() {
        assertThatCode(() -> consumer().handle(payload("not-a-uuid"))).doesNotThrowAnyException();

        verifyNoInteractions(instanceRepository);
        verify(emailServiceClient, never()).sendTenantReadyEmail(any(), any(), any(), any());
    }

    @Test
    void handle_malformedPayload_swallows() {
        assertThatCode(() -> consumer().handle("{not-json")).doesNotThrowAnyException();

        verifyNoInteractions(instanceRepository);
        verifyNoInteractions(emailServiceClient);
    }
}
