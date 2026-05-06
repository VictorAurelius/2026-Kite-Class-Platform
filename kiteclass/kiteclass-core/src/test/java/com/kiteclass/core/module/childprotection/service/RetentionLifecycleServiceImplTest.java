package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RetentionLifecycleServiceImpl} — covers daily
 * retention sweep behaviour: secure-delete + audit append on expired
 * incidents, no-op on empty result, per-row isolation when audit append
 * fails (Phase 1C v1.5, GAP-359 sub-task 359.1).
 *
 * @since Wave 24 Bucket A — GAP-359 sub-task 359.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetentionLifecycleServiceImpl — daily retention sweep")
class RetentionLifecycleServiceImplTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ChildProtectionAuditService auditService;

    private RetentionLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RetentionLifecycleServiceImpl(incidentRepository, auditService);
    }

    @Test
    @DisplayName("empty repository → 0 processed, no audit appends")
    void emptyRepo_returnsZero() {
        when(incidentRepository.findExpiredRetention(any())).thenReturn(List.of());

        int processed = service.sweepExpiredIncidents();

        assertThat(processed).isZero();
        verify(auditService, never()).append(anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("expired incident → mark deleted + null sensitive fields + audit append")
    void expiredIncident_isSecureDeletedWithAuditAppend() {
        Incident expired = makeExpired(101L, TENANT_A);
        when(incidentRepository.findExpiredRetention(any())).thenReturn(List.of(expired));

        int processed = service.sweepExpiredIncidents();

        assertThat(processed).isEqualTo(1);
        // Sensitive fields cleared, deleted flag set
        assertThat(expired.isDeleted()).isTrue();
        assertThat(expired.getDescription()).isNull();
        assertThat(expired.getEvidencePaths()).isNull();
        // Title (non-sensitive) preserved per BR-CHILD-PROT-005
        assertThat(expired.getTitle()).isEqualTo("Sample title");
        verify(incidentRepository).save(expired);

        // Audit append fired with the lifecycle action + correct entity wiring
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).append(
                eq("Incident"),
                eq(101L),
                eq(RetentionLifecycleServiceImpl.ACTION_RETENTION_EXPIRED_DELETE),
                eq(null),
                payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("trigger", "scheduled");
        assertThat(payload).containsKey("retentionUntil");
    }

    @Test
    @DisplayName("multiple tenants → audit fires once per row in correct tenant scope")
    void multipleTenants_eachAudited() {
        Incident a = makeExpired(201L, TENANT_A);
        Incident b = makeExpired(202L, TENANT_B);
        when(incidentRepository.findExpiredRetention(any())).thenReturn(List.of(a, b));

        int processed = service.sweepExpiredIncidents();

        assertThat(processed).isEqualTo(2);
        verify(auditService).append(eq("Incident"), eq(201L), anyString(), eq(null), any());
        verify(auditService).append(eq("Incident"), eq(202L), anyString(), eq(null), any());
        verify(incidentRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("per-row isolation — failed audit on row 1 does NOT block row 2")
    void perRowIsolation_failureDoesNotAbortSweep() {
        Incident a = makeExpired(301L, TENANT_A);
        Incident b = makeExpired(302L, TENANT_B);
        when(incidentRepository.findExpiredRetention(any())).thenReturn(List.of(a, b));
        when(auditService.append(eq("Incident"), eq(301L), anyString(), any(), any()))
                .thenThrow(new RuntimeException("audit failure"));

        int processed = service.sweepExpiredIncidents();

        assertThat(processed).isEqualTo(1); // only row 2 succeeded
        // Row 2 still audited and saved despite row 1 failure
        verify(auditService).append(eq("Incident"), eq(302L), anyString(), any(), any());
    }

    private static Incident makeExpired(Long id, UUID tenantId) {
        Incident i = Incident.builder()
                .title("Sample title")
                .description("sensitive narrative " + id)
                .evidencePaths("minio/" + id + ".jpg")
                .severity(IncidentSeverity.HIGH)
                .category(IncidentCategory.BULLYING)
                .status(IncidentStatus.CLOSED)
                .reporterUserId(1L)
                .retentionUntil(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        i.setId(id);
        i.setInstanceId(tenantId);
        return i;
    }
}
