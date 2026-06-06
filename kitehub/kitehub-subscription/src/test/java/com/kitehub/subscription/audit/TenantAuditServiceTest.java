package com.kitehub.subscription.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenantAuditService} (Wave provisioning-1 Bucket B — GAP-949).
 *
 * <p>Verifies (1) a {@code TENANT_PROVISIONED} {@link AdminAuditLog} row is built with the
 * expected action/target/payload fields, and (2) the REQUIRES_NEW isolation contract's
 * try/catch layer — a repository failure is swallowed so the method NEVER throws back to
 * the caller (per {@code .claude/rules/audit-service-isolation.md} §1).</p>
 */
@ExtendWith(MockitoExtension.class)
class TenantAuditServiceTest {

    @Mock AdminAuditLogRepository repository;
    @InjectMocks TenantAuditService service;

    @Test
    void recordTenantProvisioned_writesAuditRowWithExpectedFields() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(repository.save(any(AdminAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordTenantProvisioned(tenantId, ownerId, "owner@acme.test", "acme");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());
        AdminAuditLog row = captor.getValue();

        assertThat(row.getAction()).isEqualTo("TENANT_PROVISIONED");
        assertThat(row.getAdminUserId()).isEqualTo(ownerId);
        assertThat(row.getTargetEntityType()).isEqualTo("Instance");
        assertThat(row.getTargetEntityId()).isEqualTo(tenantId.toString());
        assertThat(row.getTargetResourceType()).isEqualTo("tenant");
        assertThat(row.getTargetResourceId()).isEqualTo("tenant/" + tenantId);
        assertThat(row.isSuccess()).isTrue();
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getPayloadJson())
            .contains("\"tenantId\":\"" + tenantId + "\"")
            .contains("\"subdomain\":\"acme\"")
            .contains("\"ownerEmail\":\"owner@acme.test\"");
    }

    @Test
    void recordTenantProvisioned_swallowsRepositoryFailure_doesNotThrow() {
        // REQUIRES_NEW isolates the physical txn; the try/catch keeps the failure from
        // ever reaching the caller — a failing audit MUST NOT fail tenant provisioning.
        when(repository.save(any(AdminAuditLog.class)))
            .thenThrow(new RuntimeException("simulated DB failure"));

        assertThatCode(() -> service.recordTenantProvisioned(
            UUID.randomUUID(), UUID.randomUUID(), "owner@acme.test", "acme"))
            .doesNotThrowAnyException();
    }
}
