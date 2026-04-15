package com.kiteclass.core.module.retention;

import com.kiteclass.core.common.audit.AuditLog;
import com.kiteclass.core.common.audit.AuditLogRepository;
import com.kiteclass.core.common.audit.AuditLogWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * GDPR Art. 20 (data portability) export service (ADR-013, GAP-073).
 *
 * <p>Produces a ZIP byte stream of user-owned data. The current implementation is a
 * scaffold — real profile queries, full branding history, and streaming-to-MinIO are
 * deferred (see Sub-PR 4.4 PR body). The ZIP skeleton is production-compatible so future
 * data sources just plug in new {@link ZipEntry} writers.
 *
 * <p>ZIP contents (minimum):
 * <ul>
 *   <li>{@code profile.json} — user profile stub (full query deferred)</li>
 *   <li>{@code audit-trail.csv} — recent audit entries for this user</li>
 *   <li>{@code README.txt} — GDPR Art. 20 statement + generation timestamp</li>
 * </ul>
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataExportService {

    private static final String AGGREGATE_TYPE = "DeletionRequest";
    private static final int AUDIT_EXPORT_LIMIT = 500;

    private final AuditLogRepository auditRepository;
    private final AuditLogWriter auditLog;

    /**
     * Build the GDPR Art. 20 ZIP for a user+tenant. Returns the raw bytes — real
     * streaming-to-MinIO + signed URL generation is deferred.
     *
     * @param userId   the user requesting their data
     * @param tenantId the tenant scoping the export
     * @return ZIP archive bytes
     */
    @Transactional
    public byte[] exportForUser(Long userId, UUID tenantId) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            writeProfileStub(zip, userId, tenantId);
            writeAuditTrail(zip, userId);
            writeReadme(zip, userId, tenantId);
            zip.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build GDPR export ZIP", e);
        }

        auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("deletion.export_generated")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId("user:" + userId)
                .actorUserId(userId)
                .payload(String.format(
                        "{\"userId\":%d,\"tenantId\":\"%s\",\"size\":%d}",
                        userId, tenantId, buffer.size()))
                .build());

        log.info("[deletion] export generated user={} tenant={} size={}B",
                userId, tenantId, buffer.size());
        return buffer.toByteArray();
    }

    private void writeProfileStub(ZipOutputStream zip, Long userId, UUID tenantId)
            throws IOException {
        zip.putNextEntry(new ZipEntry("profile.json"));
        String json = String.format(
                "{%n  \"userId\": %d,%n  \"tenantId\": \"%s\",%n"
                        + "  \"note\": \"Full profile query deferred; "
                        + "see Sub-PR 4.4 PR body for scope.\",%n"
                        + "  \"generatedAt\": \"%s\"%n}",
                userId, tenantId, Instant.now());
        zip.write(json.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writeAuditTrail(ZipOutputStream zip, Long userId) throws IOException {
        zip.putNextEntry(new ZipEntry("audit-trail.csv"));
        StringBuilder csv = new StringBuilder(
                "id,created_at,action_type,aggregate_type,aggregate_id,actor_user_id,reason\n");
        List<AuditLog> entries = auditRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                        AGGREGATE_TYPE, "user:" + userId);
        int count = 0;
        for (AuditLog entry : entries) {
            if (count >= AUDIT_EXPORT_LIMIT) {
                break;
            }
            csv.append(entry.getId()).append(',')
                    .append(entry.getCreatedAt()).append(',')
                    .append(csvSafe(entry.getActionType())).append(',')
                    .append(csvSafe(entry.getAggregateType())).append(',')
                    .append(csvSafe(entry.getAggregateId())).append(',')
                    .append(entry.getActorUserId() == null ? "" : entry.getActorUserId())
                    .append(',')
                    .append(csvSafe(entry.getReason()))
                    .append('\n');
            count++;
        }
        zip.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writeReadme(ZipOutputStream zip, Long userId, UUID tenantId)
            throws IOException {
        zip.putNextEntry(new ZipEntry("README.txt"));
        String readme = String.format(
                "GDPR Art. 20 Data Export%n"
                        + "=========================%n%n"
                        + "User ID: %d%n"
                        + "Tenant ID: %s%n"
                        + "Generated at: %s%n%n"
                        + "This archive contains the personal data KiteClass holds for the "
                        + "above user, exported under GDPR Article 20 (right to data "
                        + "portability). If you initiated a deletion request (GDPR Art. 17), "
                        + "download and retain this archive — after the 7-day grace window "
                        + "your data will be purged or pseudonymized per ADR-013.%n%n"
                        + "Files included:%n"
                        + "  profile.json     — account profile stub%n"
                        + "  audit-trail.csv  — recent account audit events%n"
                        + "  README.txt       — this file%n",
                userId, tenantId, Instant.now());
        zip.write(readme.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String csvSafe(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
