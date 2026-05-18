package com.kitehub.subscription.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AdminAuditAspect} (GAP-521).
 *
 * @since 1.0.0 (Wave 72a)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditAspect — Admin Action Audit Log (GAP-521)")
class AdminAuditAspectTest {

    @Mock AdminAuditLogRepository repository;

    private UUID adminId;
    private DemoController proxy;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        // Wire authentication principal — the X-User-Id filter does this in prod.
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                adminId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))));

        // Wire a fake servlet request so the aspect can read IP + UA + request-id.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.42");
        req.addHeader("User-Agent", "TestRunner/1.0");
        req.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");
        req.addHeader("X-Request-Id", "req-test-correlation-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        // Build an AspectJ proxy that runs the real aspect against a mock-backed
        // target controller. No Spring context required.
        AdminAuditAspect aspect = new AdminAuditAspect(repository);
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoController());
        factory.addAspect(aspect);
        proxy = factory.getProxy();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("@Auditable method success → audit row persisted with admin id, action, target, IP, UA")
    void successPath() {
        proxy.approve(42L, "Looks good");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());

        AdminAuditLog row = captor.getValue();
        assertThat(row.getAction()).isEqualTo("BETA_REQUEST_APPROVE");
        assertThat(row.getTargetEntityType()).isEqualTo("beta_access_request");
        assertThat(row.getTargetEntityId()).isEqualTo("42");
        assertThat(row.getAdminUserId()).isEqualTo(adminId);
        assertThat(row.getRequestIp()).isEqualTo("203.0.113.42");
        assertThat(row.getUserAgent()).isEqualTo("TestRunner/1.0");
        assertThat(row.isSuccess()).isTrue();
        assertThat(row.getPayloadJson()).contains("\"id\":42").contains("\"notes\":\"Looks good\"");
    }

    @Test
    @DisplayName("Wave 92 GAP-521 Phase 2 — enrichment fields captured (requestId + resourceType + resourceId)")
    void enrichmentFieldsPopulated() {
        proxy.approveWithEnrichment(99L, "Approved with enrichment");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());

        AdminAuditLog row = captor.getValue();
        assertThat(row.getAction()).isEqualTo("BETA_REQUEST_APPROVE_ENRICHED");
        // legacy entity fields giữ nguyên
        assertThat(row.getTargetEntityType()).isEqualTo("beta_access_request");
        assertThat(row.getTargetEntityId()).isEqualTo("99");
        // Phase 2 enrichment
        assertThat(row.getTargetResourceType()).isEqualTo("beta_access_request");
        assertThat(row.getTargetResourceId()).isEqualTo("99");
        assertThat(row.getRequestId()).isEqualTo("req-test-correlation-123");
    }

    @Test
    @DisplayName("Wave 92 GAP-521 Phase 2 — annotation without resourceType keeps fields null")
    void enrichmentOptionalWhenAnnotationOmitsFields() {
        proxy.approve(7L, "Legacy call site");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());

        AdminAuditLog row = captor.getValue();
        // Backward compat: legacy @Auditable không khai báo resourceType / resourceIdSource
        // → enrichment fields null (chỉ requestId từ header still captured)
        assertThat(row.getTargetResourceType()).isNull();
        assertThat(row.getTargetResourceId()).isNull();
        assertThat(row.getBeforeState()).isNull();
        assertThat(row.getAfterState()).isNull();
        // requestId vẫn capture qua header
        assertThat(row.getRequestId()).isEqualTo("req-test-correlation-123");
    }

    @Test
    @DisplayName("sensitive arg name is redacted in payloadJson")
    void sensitiveArgRedacted() {
        proxy.login(42L, "hunter2-secret");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getPayloadJson())
            .contains("\"password\":\"<redacted>\"")
            .doesNotContain("hunter2-secret");
    }

    @Test
    @DisplayName("method throws → audit row marked failure + error captured, exception propagates")
    void failurePath() {
        assertThatThrownBy(() -> proxy.failing(99L))
            .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());

        AdminAuditLog row = captor.getValue();
        assertThat(row.isSuccess()).isFalse();
        assertThat(row.getErrorMessage()).contains("IllegalStateException").contains("bang");
    }

    /* test fixture controller */

    static class DemoController {
        @Auditable(action = "BETA_REQUEST_APPROVE", entityType = "beta_access_request")
        public String approve(Long id, String notes) {
            return "ok-" + id;
        }

        @Auditable(action = "USER_LOGIN_AS", entityType = "user")
        public void login(Long id, String password) {
            // simulates an endpoint that takes a password arg — must be redacted.
        }

        @Auditable(action = "WILL_FAIL", entityType = "thing")
        public void failing(Long id) {
            throw new IllegalStateException("bang");
        }

        /**
         * Wave 92 Bucket A — GAP-521 Phase 2 fixture: site declares enrichment
         * {@code resourceType} + {@code resourceIdSource} per new annotation API.
         */
        @Auditable(
            action = "BETA_REQUEST_APPROVE_ENRICHED",
            entityType = "beta_access_request",
            resourceType = "beta_access_request",
            resourceIdSource = "arg0")
        public String approveWithEnrichment(Long id, String notes) {
            return "enriched-" + id;
        }
    }
}
