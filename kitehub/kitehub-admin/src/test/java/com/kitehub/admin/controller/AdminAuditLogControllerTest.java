package com.kitehub.admin.controller;

import com.kitehub.admin.dto.AuditLogSummary;
import com.kitehub.subscription.audit.AdminAuditLog;
import com.kitehub.subscription.audit.AdminAuditLogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for {@link AdminAuditLogController} — verifies HTTP 200 + JSON shape for the
 * canonical {@code /api/v1/admin/audit-logs} path (GAP-774 — Wave 106 Mảng D4 fix).
 *
 * <p>Pure unit-level test using Mockito stubs — mirrors the {@link AdminInstancesControllerTest}
 * pattern. Real-DB JSONB round-trip is covered by the kitehub-subscription
 * {@code AdminAuditLogJsonbPostgresIT} Testcontainers test.</p>
 */
class AdminAuditLogControllerTest {

    private AdminAuditLogRepository auditLogRepository;
    private AdminAuditLogController controller;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AdminAuditLogRepository.class);
        controller = new AdminAuditLogController(auditLogRepository);
    }

    @Test
    void listAuditLogs_returnsHttp200AndPagedShape() {
        AdminAuditLog entry = buildEntry("BETA_REQUEST_APPROVE");
        Page<AdminAuditLog> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);
        when(auditLogRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<AuditLogSummary>> response =
                controller.listAuditLogs(null, null, null, null, PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getAction()).isEqualTo("BETA_REQUEST_APPROVE");
        assertThat(response.getBody().getContent().get(0).isSuccess()).isTrue();
    }

    @Test
    void listAuditLogs_emptyResult_returnsHttp200WithEmptyPage() {
        Page<AdminAuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(auditLogRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        ResponseEntity<Page<AuditLogSummary>> response =
                controller.listAuditLogs(null, null, null, null, PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalElements()).isZero();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void listAuditLogs_passesFiltersToRepository() {
        UUID adminId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        Page<AdminAuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(auditLogRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        controller.listAuditLogs("BETA_REQUEST_APPROVE", adminId, from, to, PageRequest.of(0, 20));

        verify(auditLogRepository).search(
                eq("BETA_REQUEST_APPROVE"), eq(adminId), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    void listAuditLogs_noFilters_passesNullsToRepository() {
        Page<AdminAuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(auditLogRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        controller.listAuditLogs(null, null, null, null, PageRequest.of(0, 20));

        verify(auditLogRepository).search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAuditLog_existingId_returnsHttp200WithSummary() {
        AdminAuditLog entry = buildEntry("INSTANCE_SUSPEND");
        entry.setId(42L);
        when(auditLogRepository.findById(42L)).thenReturn(Optional.of(entry));

        ResponseEntity<AuditLogSummary> response = controller.getAuditLog(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(42L);
        assertThat(response.getBody().getAction()).isEqualTo("INSTANCE_SUSPEND");
    }

    @Test
    void getAuditLog_unknownId_throwsEntityNotFound() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getAuditLog(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Audit log not found");
    }

    @Test
    void clampPageable_nullInput_returnsDefaultSize20() {
        Pageable result = AdminAuditLogController.clampPageable(null);
        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    void clampPageable_oversized_clampsTo100() {
        Pageable input = PageRequest.of(2, 5_000, Sort.by("createdAt"));
        Pageable result = AdminAuditLogController.clampPageable(input);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getSort()).isEqualTo(Sort.by("createdAt"));
    }

    @Test
    void constants_match_admin_instances_controller() {
        // Defense: v1 stub must keep page-size limits aligned with sibling v1 controllers.
        assertThat(AdminAuditLogController.DEFAULT_PAGE_SIZE)
                .isEqualTo(AdminInstancesController.DEFAULT_PAGE_SIZE);
        assertThat(AdminAuditLogController.MAX_PAGE_SIZE)
                .isEqualTo(AdminInstancesController.MAX_PAGE_SIZE);
    }

    private AdminAuditLog buildEntry(String action) {
        return AdminAuditLog.builder()
                .id(1L)
                .adminUserId(UUID.randomUUID())
                .action(action)
                .targetEntityType("beta_access_request")
                .targetEntityId("1")
                .requestIp("203.0.113.7")
                .userAgent("Mozilla/5.0")
                .payloadJson("{\"k\":\"v\"}")
                .success(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
