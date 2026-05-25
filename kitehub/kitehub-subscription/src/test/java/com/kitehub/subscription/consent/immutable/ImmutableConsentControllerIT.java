package com.kitehub.subscription.consent.immutable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.consent.immutable.ImmutableConsentController.ConsentRequestDto;
import com.kitehub.subscription.consent.immutable.ImmutableConsentController.ConsentWithdrawRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-user IDOR + admin-override RBAC suite for {@link ImmutableConsentController}
 * — Wave beta-readiness-8 Bucket A (GAP-737).
 *
 * <p>Drives the {@link ConsentAuthorizationBean} guard end-to-end through the
 * Spring Security filter chain (per {@link SecurityConfig}). The
 * {@code XUserRolesHeaderFilter} translates {@code X-User-Id} +
 * {@code X-User-Roles} headers into the security context; {@link WithMockUser}
 * is also accepted because Spring Security Test installs the same principal
 * shape (principal-name = username, authorities = roles).</p>
 *
 * <p>Service layer is mocked — these are RBAC tests, not consent-flow tests
 * (real consent flow lives in {@code ImmutableConsentControllerFlowIT} +
 * {@code ConsentRecordImmutablePostgresIT}).</p>
 */
@WebMvcTest(controllers = ImmutableConsentController.class)
@Import({SecurityConfig.class, ConsentAuthorizationBean.class})
@DisplayName("ImmutableConsentController — IDOR + admin RBAC (GAP-737)")
class ImmutableConsentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ConsentService consentService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(consentService);
    }

    private ConsentRecordImmutable stubRow(Long userId) {
        return ConsentRecordImmutable.builder()
                .id(1L)
                .userId(userId)
                .tenantId(7L)
                .granted("{\"essential\":true,\"analytics\":true,\"marketing\":false}")
                .prevHash(null)
                .currentHash("a".repeat(64))
                .ipAddress("203.0.113.7")
                .userAgent("JUnit/5")
                .signedAt(OffsetDateTime.now())
                .build();
    }

    private ConsentRequestDto sampleRecordRequest(Long userId) {
        return ConsentRequestDto.builder()
                .userId(userId)
                .tenantId(7L)
                .granted(Map.of("essential", Boolean.TRUE, "analytics", Boolean.TRUE))
                .ipAddress("203.0.113.7")
                .userAgent("JUnit/5")
                .build();
    }

    private ConsentWithdrawRequestDto sampleWithdrawRequest(Long userId) {
        return ConsentWithdrawRequestDto.builder()
                .userId(userId)
                .tenantId(7L)
                .ipAddress("203.0.113.7")
                .userAgent("JUnit/5")
                .build();
    }

    // ─── GET history /{userId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/consent/v2/{userId}")
    class HistoryEndpoint {

        @Test
        @WithMockUser(username = "42", roles = "TENANT_USER")
        @DisplayName("same user → 200 + history rendered")
        void sameUserAllowed() throws Exception {
            when(consentService.findHistory(42L)).thenReturn(List.of(stubRow(42L)));
            mockMvc.perform(get("/api/v1/consent/v2/{userId}", 42L))
                    .andExpect(status().isOk());
            verify(consentService).findHistory(42L);
        }

        @Test
        @WithMockUser(username = "42", roles = "TENANT_USER")
        @DisplayName("cross-user → 403 + service never invoked (IDOR blocked)")
        void crossUserBlocked() throws Exception {
            mockMvc.perform(get("/api/v1/consent/v2/{userId}", 99L))
                    .andExpect(status().isForbidden());
            verify(consentService, never()).findHistory(anyLong());
        }

        @Test
        @WithMockUser(username = "admin-uuid", roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN cross-user → 200 (DSAR / audit scope)")
        void platformAdminAllowedCrossUser() throws Exception {
            when(consentService.findHistory(42L)).thenReturn(List.of(stubRow(42L)));
            mockMvc.perform(get("/api/v1/consent/v2/{userId}", 42L))
                    .andExpect(status().isOk());
            verify(consentService).findHistory(42L);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("anonymous → 401 (default-deny per SecurityConfig)")
        void anonymousUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/consent/v2/{userId}", 42L))
                    .andExpect(status().isUnauthorized());
            verify(consentService, never()).findHistory(anyLong());
        }
    }

    // ─── POST /record ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/consent/v2/record")
    class RecordEndpoint {

        @Test
        @WithMockUser(username = "42", roles = "TENANT_USER")
        @DisplayName("same user → 201")
        void sameUserCreates() throws Exception {
            when(consentService.recordConsent(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(stubRow(42L));
            mockMvc.perform(post("/api/v1/consent/v2/record")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRecordRequest(42L))))
                    .andExpect(status().isCreated());

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(consentService).recordConsent(userIdCaptor.capture(), any(), any(),
                    anyString(), anyString());
            assertThat(userIdCaptor.getValue()).isEqualTo(42L);
        }

        @Test
        @WithMockUser(username = "42", roles = "TENANT_USER")
        @DisplayName("cross-user body → 403 (IDOR blocked, service never invoked)")
        void crossUserBlocked() throws Exception {
            mockMvc.perform(post("/api/v1/consent/v2/record")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRecordRequest(99L))))
                    .andExpect(status().isForbidden());
            verify(consentService, never())
                    .recordConsent(any(), any(), any(), anyString(), anyString());
        }

        @Test
        @WithMockUser(username = "admin-uuid", roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN cross-user → 201")
        void platformAdminCrossUser() throws Exception {
            when(consentService.recordConsent(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(stubRow(42L));
            mockMvc.perform(post("/api/v1/consent/v2/record")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRecordRequest(42L))))
                    .andExpect(status().isCreated());
            verify(consentService).recordConsent(any(), any(), any(), anyString(), anyString());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("anonymous → 401")
        void anonymousUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/consent/v2/record")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRecordRequest(42L))))
                    .andExpect(status().isUnauthorized());
            verify(consentService, never())
                    .recordConsent(any(), any(), any(), anyString(), anyString());
        }
    }

    // ─── POST /withdraw ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/consent/v2/withdraw")
    class WithdrawEndpoint {

        @Test
        @WithMockUser(username = "42", roles = "TENANT_USER")
        @DisplayName("same user → 201")
        void sameUserWithdraws() throws Exception {
            when(consentService.withdrawConsent(any(), any(), anyString(), anyString()))
                    .thenReturn(stubRow(42L));
            mockMvc.perform(post("/api/v1/consent/v2/withdraw")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleWithdrawRequest(42L))))
                    .andExpect(status().isCreated());
            verify(consentService).withdrawConsent(any(), any(), anyString(), anyString());
        }

        @Test
        @WithMockUser(username = "42", roles = "TENANT_USER")
        @DisplayName("cross-user body → 403 (IDOR blocked, service never invoked)")
        void crossUserBlocked() throws Exception {
            mockMvc.perform(post("/api/v1/consent/v2/withdraw")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleWithdrawRequest(99L))))
                    .andExpect(status().isForbidden());
            verify(consentService, never())
                    .withdrawConsent(any(), any(), anyString(), anyString());
        }

        @Test
        @WithMockUser(username = "admin-uuid", roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN cross-user → 201")
        void platformAdminCrossUser() throws Exception {
            when(consentService.withdrawConsent(any(), any(), anyString(), anyString()))
                    .thenReturn(stubRow(42L));
            mockMvc.perform(post("/api/v1/consent/v2/withdraw")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleWithdrawRequest(42L))))
                    .andExpect(status().isCreated());
            verify(consentService).withdrawConsent(any(), any(), anyString(), anyString());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("anonymous → 401")
        void anonymousUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/consent/v2/withdraw")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleWithdrawRequest(42L))))
                    .andExpect(status().isUnauthorized());
            verify(consentService, never())
                    .withdrawConsent(any(), any(), anyString(), anyString());
        }
    }
}
