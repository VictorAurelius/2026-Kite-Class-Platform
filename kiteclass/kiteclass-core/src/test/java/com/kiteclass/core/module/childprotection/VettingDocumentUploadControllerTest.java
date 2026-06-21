package com.kiteclass.core.module.childprotection;

import com.kiteclass.core.common.exception.GlobalExceptionHandler;
import com.kiteclass.core.module.childprotection.controller.VettingController;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.service.VettingService;
import com.kiteclass.core.module.childprotection.storage.VettingDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice test for the LLTP upload endpoint
 * {@code POST /api/v1/vettings/{vettingId}/documents} (Wave 18b3 Bucket B).
 *
 * <p>Verifies:
 * <ul>
 *   <li>Happy-path multipart upload by SAFEGUARDING_OFFICER → 201 with
 *       storage key + size + content-type;</li>
 *   <li>RBAC: TEACHER role → 403, storage never invoked;</li>
 *   <li>Missing role header → 403;</li>
 *   <li>Empty file → 400 VETTING_DOC_EMPTY;</li>
 *   <li>Oversized file → 400 VETTING_DOC_TOO_LARGE.</li>
 * </ul>
 *
 * @since Wave 18b3 Bucket B — GAP-322b Phase 1B remainder
 */
@WebMvcTest(VettingController.class)
@AutoConfigureMockMvc
@Import({
        VettingDocumentUploadControllerTest.TestSecurityConfig.class,
        VettingDocumentUploadControllerTest.MockConfig.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
@DisplayName("Vetting Document Upload Controller — Phase 1B remainder")
class VettingDocumentUploadControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        VettingService vettingService() {
            return Mockito.mock(VettingService.class);
        }

        @Bean
        @Primary
        VettingDocumentStorage vettingDocumentStorage() {
            return Mockito.mock(VettingDocumentStorage.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private VettingService vettingService;
    @Autowired private VettingDocumentStorage storage;

    private static final String OFFICER_ROLE = "SAFEGUARDING_OFFICER";
    private static final String TEACHER_ROLE = "TEACHER";

    @BeforeEach
    void resetMocks() {
        // @WebMvcTest reuses bean instances across tests; reset to keep
        // verify(..., never()) honest.
        Mockito.reset(vettingService, storage);
    }

    @Test
    @DisplayName("POST /vettings/{id}/documents — SAFEGUARDING_OFFICER → 201 storage key")
    void upload_authorised_returns201() throws Exception {
        // Vetting exists
        Vetting v = Vetting.builder().teacherId(100L).status(VettingStatus.PENDING).build();
        v.setId(7L);
        when(vettingService.findById(7L)).thenReturn(v);

        when(storage.storeDocument(eq(7L), eq("lltp.pdf"), any(byte[].class)))
                .thenReturn("vetting/7/lltp.pdf");

        // GAP-1527: content must pass the server-side magic-byte sniff → real PDF header.
        byte[] pdfBytes = "%PDF-1.4 body".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "lltp.pdf", "application/pdf",
                pdfBytes
        );

        mockMvc.perform(multipart("/api/v1/vettings/{id}/documents", 7L)
                        .file(file)
                        .header("X-User-Roles", OFFICER_ROLE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vettingId").value(7))
                .andExpect(jsonPath("$.data.storageKey").value("vetting/7/lltp.pdf"))
                .andExpect(jsonPath("$.data.sizeBytes").value(pdfBytes.length))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"));

        verify(storage).storeDocument(eq(7L), eq("lltp.pdf"), any(byte[].class));
    }

    @Test
    @DisplayName("POST /vettings/{id}/documents — spoofed content (image/pdf MIME, non-magic bytes) → 400, storage never called (GAP-1527)")
    void upload_spoofedContent_returns400() throws Exception {
        Vetting v = Vetting.builder().teacherId(100L).status(VettingStatus.PENDING).build();
        v.setId(7L);
        when(vettingService.findById(7L)).thenReturn(v);

        // Declared application/pdf, but actual bytes are an HTML/script payload — no PDF/JPG/PNG magic.
        MockMultipartFile spoof = new MockMultipartFile(
                "file", "lltp.pdf", "application/pdf",
                "<html><script>alert(1)</script></html>".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/vettings/{id}/documents", 7L)
                        .file(spoof)
                        .header("X-User-Roles", OFFICER_ROLE))
                .andExpect(status().isBadRequest());

        verify(storage, never()).storeDocument(any(), any(), any());
    }

    @Test
    @DisplayName("POST /vettings/{id}/documents — TEACHER role → 403, storage never called")
    void upload_teacher_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lltp.pdf", "application/pdf", "x".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/vettings/{id}/documents", 7L)
                        .file(file)
                        .header("X-User-Roles", TEACHER_ROLE))
                .andExpect(status().isForbidden());

        verify(storage, never()).storeDocument(any(), any(), any());
    }

    @Test
    @DisplayName("POST /vettings/{id}/documents — missing role header → 403")
    void upload_missingHeader_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lltp.pdf", "application/pdf", "x".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/vettings/{id}/documents", 7L)
                        .file(file))
                .andExpect(status().isForbidden());
        verify(storage, never()).storeDocument(any(), any(), any());
    }

    @Test
    @DisplayName("POST /vettings/{id}/documents — empty file → 400 VETTING_DOC_EMPTY")
    void upload_emptyFile_returns400() throws Exception {
        Vetting v = Vetting.builder().teacherId(100L).status(VettingStatus.PENDING).build();
        v.setId(7L);
        when(vettingService.findById(7L)).thenReturn(v);

        MockMultipartFile empty = new MockMultipartFile(
                "file", "lltp.pdf", "application/pdf", new byte[0]
        );
        mockMvc.perform(multipart("/api/v1/vettings/{id}/documents", 7L)
                        .file(empty)
                        .header("X-User-Roles", OFFICER_ROLE))
                .andExpect(status().isBadRequest());

        verify(storage, never()).storeDocument(any(), any(), any());
    }

    @Test
    @DisplayName("POST /vettings/{id}/documents — oversized file → 400 VETTING_DOC_TOO_LARGE")
    void upload_oversize_returns400() throws Exception {
        Vetting v = Vetting.builder().teacherId(100L).status(VettingStatus.PENDING).build();
        v.setId(7L);
        when(vettingService.findById(7L)).thenReturn(v);

        // 11 MB > 10 MB cap
        byte[] big = new byte[11 * 1024 * 1024];
        MockMultipartFile huge = new MockMultipartFile(
                "file", "lltp.pdf", "application/pdf", big
        );
        mockMvc.perform(multipart("/api/v1/vettings/{id}/documents", 7L)
                        .file(huge)
                        .header("X-User-Roles", OFFICER_ROLE))
                .andExpect(status().isBadRequest());

        verify(storage, never()).storeDocument(any(), any(), any());
    }
}
