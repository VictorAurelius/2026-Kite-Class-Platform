package com.kitehub.branding.controller;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.S3StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization + cross-tenant IDOR regression tests for {@link AssetStorageController}
 * (GAP-1526, OWASP A01 — kitehub-branding residual not covered by the GAP-1491 cluster).
 *
 * <p>Before the fix the upload (POST), list (GET) and delete (DELETE) asset endpoints had NO
 * {@code @PreAuthorize} and NO tenant binding — any authenticated caller could mutate/read any
 * tenant's assets. The fix adds OWNER-tier write authz + OWNER/STAFF read authz + a
 * {@link com.kitehub.branding.security.TenantOwnershipGuard} check binding the path {@code instanceId}
 * to the gateway-trusted {@code X-Tenant-Id}.</p>
 *
 * <p>Uses the enforcing {@code rbac-test} security profile so both the {@code @PreAuthorize} role
 * gate and the ownership guard run for real.</p>
 */
@WebMvcTest(controllers = AssetStorageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("AssetStorageController @PreAuthorize + tenant ownership (GAP-1526, OWASP A01)")
class AssetStorageControllerAuthzTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_INSTANCE = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private BrandingJobService brandingJobService;

    // NB: do NOT @MockitoBean the ObjectMapper — a Mockito mock breaks Spring MVC's Jackson
    // converters (routerFunctionMapping). The controller uses the real auto-configured one.

    // SecurityConfig injects SseTokenService (SseQueryTokenAuthFilter) — slice context needs it.
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    private static MockMultipartFile pngFile() {
        // Real PNG magic bytes so resolveSafeContentType() resolves to image/png.
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        return new MockMultipartFile("file", "logo.png", "image/png", png);
    }

    // ---- WRITE (upload) — OWNER-tier ---------------------------------------

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER uploads to own instance → not 403 (write allowed)")
    void owner_uploadOwnInstance_allowed() throws Exception {
        when(s3StorageService.generateAssetPath(any(), anyString(), anyString()))
                .thenReturn("instances/" + OWN_INSTANCE + "/branding/LOGO/logo.png");
        when(s3StorageService.uploadAsset(any(), anyString(), anyString(), anyLong()))
                .thenReturn("https://cdn.kitehub.me/instances/" + OWN_INSTANCE + "/logo.png");
        when(s3StorageService.getPresignedAssetUrl(anyString()))
                .thenReturn("https://minio.local/logo.png?sig=x");

        mockMvc.perform(multipart("/api/platform/branding/assets/{instanceId}/{assetType}",
                        OWN_INSTANCE, "LOGO")
                        .file(pngFile())
                        .with(csrf())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER upload own instance"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT upload → 403 (write is OWNER-tier)")
    void student_upload_denied() throws Exception {
        mockMvc.perform(multipart("/api/platform/branding/assets/{instanceId}/{assetType}",
                        OWN_INSTANCE, "LOGO")
                        .file(pngFile())
                        .with(csrf())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER upload to ANOTHER tenant's instance → 403")
    void owner_uploadCrossTenant_denied() throws Exception {
        mockMvc.perform(multipart("/api/platform/branding/assets/{instanceId}/{assetType}",
                        OTHER_INSTANCE, "LOGO")
                        .file(pngFile())
                        .with(csrf())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    // ---- DELETE — OWNER-tier -----------------------------------------------

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("OWASP A01: TEACHER (staff) DELETE assets → 403 (delete is OWNER-tier)")
    void teacher_delete_denied() throws Exception {
        mockMvc.perform(delete("/api/platform/branding/assets/{instanceId}", OWN_INSTANCE)
                        .with(csrf())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER DELETE another tenant's assets → 403")
    void owner_deleteCrossTenant_denied() throws Exception {
        mockMvc.perform(delete("/api/platform/branding/assets/{instanceId}", OTHER_INSTANCE)
                        .with(csrf())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    // ---- READ (list) — OWNER + STAFF ---------------------------------------

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("STAFF (TEACHER) GET own-instance assets → not 403 (staff read allowed)")
    void teacher_getOwnInstance_allowed() throws Exception {
        when(brandingJobService.getJobsByInstance(any())).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/api/platform/branding/assets/{instanceId}", OWN_INSTANCE)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "TEACHER read own instance"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER GET another tenant's assets → 403")
    void owner_getCrossTenant_denied() throws Exception {
        mockMvc.perform(get("/api/platform/branding/assets/{instanceId}", OTHER_INSTANCE)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role + own tenant)");
        }
    }
}
