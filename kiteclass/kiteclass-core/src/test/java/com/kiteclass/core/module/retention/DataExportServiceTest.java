package com.kiteclass.core.module.retention;

import com.kiteclass.core.common.audit.AuditLog;
import com.kiteclass.core.common.audit.AuditLogRepository;
import com.kiteclass.core.common.audit.AuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {

    @Mock
    private AuditLogRepository auditRepository;

    @Mock
    private AuditLogWriter auditLog;

    @InjectMocks
    private DataExportService service;

    @Test
    void export_builds_zip_with_required_entries() throws Exception {
        when(auditRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(List.of());

        byte[] zipBytes = service.exportForUser(42L, UUID.randomUUID());

        assertThat(zipBytes).isNotEmpty();
        List<String> names = listZipEntries(zipBytes);
        assertThat(names).containsExactlyInAnyOrder(
                "profile.json", "audit-trail.csv", "README.txt");
    }

    @Test
    void export_includes_audit_entries_in_csv() throws Exception {
        AuditLog entry = AuditLog.builder()
                .actionType("deletion.requested")
                .aggregateType("DeletionRequest")
                .aggregateId("user:42")
                .actorUserId(42L)
                .reason("initial request")
                .build();
        entry.setId(1L);
        entry.setCreatedAt(Instant.now());
        when(auditRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(List.of(entry));

        byte[] zipBytes = service.exportForUser(42L, UUID.randomUUID());

        String csv = readZipEntry(zipBytes, "audit-trail.csv");
        assertThat(csv).contains("id,created_at,action_type");
        assertThat(csv).contains("deletion.requested");
        assertThat(csv).contains("initial request");
    }

    @Test
    void export_records_audit_event() {
        when(auditRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(List.of());

        service.exportForUser(42L, UUID.randomUUID());

        verify(auditLog).record(org.mockito.ArgumentMatchers.argThat(ev ->
                ev != null && "deletion.export_generated".equals(ev.getActionType())
                        && Long.valueOf(42L).equals(ev.getActorUserId())));
    }

    @Test
    void readme_contains_gdpr_reference_and_user_id() throws Exception {
        when(auditRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(List.of());

        byte[] zipBytes = service.exportForUser(42L, UUID.randomUUID());

        String readme = readZipEntry(zipBytes, "README.txt");
        assertThat(readme).contains("GDPR");
        assertThat(readme).contains("Art. 20");
        assertThat(readme).contains("42");
    }

    // --- helpers -----------------------------------------------------------

    private static List<String> listZipEntries(byte[] zipBytes) throws Exception {
        var names = new java.util.ArrayList<String>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                names.add(entry.getName());
                zin.closeEntry();
            }
        }
        return names;
    }

    private static String readZipEntry(byte[] zipBytes, String name) throws Exception {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return new String(zin.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
                zin.closeEntry();
            }
        }
        throw new AssertionError("ZIP entry not found: " + name);
    }
}
