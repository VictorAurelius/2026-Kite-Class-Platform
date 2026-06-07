package com.kiteclass.core.module.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentGenerationService;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.branding.DocumentBrandingAssembler;
import com.kiteclass.core.module.document.dto.DocumentGenerationRequestDto;
import com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.service.BrandingService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sub-PR 5.5 — WebMvc tests for {@link DocumentGenerationController}.
 *
 * <p>Covers: preview vs download disposition, PDF-only preview gate, format parsing, branding
 * enrichment via real {@link DocumentBrandingAssembler}, RFC-5987 filename encoding.
 */
@WebMvcTest(DocumentGenerationController.class)
@Import({TestSecurityConfig.class, DocumentBrandingAssembler.class})
@ActiveProfiles("test")
@DisplayName("DocumentGenerationController Tests")
class DocumentGenerationControllerTest {

    private static final UUID TENANT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private DocumentGenerationService documentGenerationService;

    @TestConfiguration
    static class Mocks {
        @Bean
        BrandingService brandingService() {
            return mock(BrandingService.class);
        }

        @Bean
        DocumentGenerationService documentGenerationService() {
            return mock(DocumentGenerationService.class);
        }

        // GAP-1040: the @Import'd real DocumentBrandingAssembler now requires
        // LandingPageSafetyProperties (logoUrl host allowlist). @WebMvcTest does not
        // auto-bind @ConfigurationProperties, so provide the default-allowlist instance.
        @Bean
        LandingPageSafetyProperties landingPageSafetyProperties() {
            return new LandingPageSafetyProperties();
        }
    }

    @BeforeEach
    void bindTenant() {
        TenantContext.setCurrentTenant(TENANT_UUID);
        reset(brandingService, documentGenerationService);

        when(brandingService.getBranding()).thenReturn(BrandingResponse.builder()
                .primaryColor("#2563EB")
                .secondaryColor("#8B5CF6")
                .accentColor("#10B981")
                .logoUrl("https://cdn.kitehub.me/logo.png")
                .displayName("Kite Education Center")
                .build());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("PDF preview returns inline Content-Disposition with RFC-5987 filename")
    void pdfPreviewReturnsInline() throws Exception {
        when(documentGenerationService.generate(any(DocumentRequest.class)))
                .thenReturn(DocumentResponse.of(new byte[]{1, 2, 3, 4}, DocumentFormat.PDF, "hóa-đơn-001.pdf"));

        mockMvc.perform(post("/api/v1/documents/pdf/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("invoice", Map.of("invoice.number", "INV-001")))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("inline")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }

    @Test
    @DisplayName("PDF download returns attachment Content-Disposition")
    void pdfDownloadReturnsAttachment() throws Exception {
        when(documentGenerationService.generate(any(DocumentRequest.class)))
                .thenReturn(DocumentResponse.of(new byte[]{9, 9, 9}, DocumentFormat.PDF, "invoice.pdf"));

        mockMvc.perform(post("/api/v1/documents/pdf/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("invoice", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("XLSX download returns attachment")
    void xlsxDownloadReturnsAttachment() throws Exception {
        when(documentGenerationService.generate(any(DocumentRequest.class)))
                .thenReturn(DocumentResponse.of(new byte[]{1}, DocumentFormat.XLSX, "attendance.xlsx"));

        mockMvc.perform(post("/api/v1/documents/xlsx/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("attendance", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("DOCX download returns attachment")
    void docxDownloadReturnsAttachment() throws Exception {
        when(documentGenerationService.generate(any(DocumentRequest.class)))
                .thenReturn(DocumentResponse.of(new byte[]{1}, DocumentFormat.DOCX, "contract.docx"));

        mockMvc.perform(post("/api/v1/documents/docx/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("teacher-contract", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("XLSX preview rejected with 400")
    void xlsxPreviewRejected() throws Exception {
        mockMvc.perform(post("/api/v1/documents/xlsx/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("attendance", Map.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DOCX preview rejected with 400")
    void docxPreviewRejected() throws Exception {
        mockMvc.perform(post("/api/v1/documents/docx/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("teacher-contract", Map.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unsupported format returns 400")
    void unsupportedFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/documents/bogus/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("foo", Map.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Missing templateId returns 400")
    void missingTemplateIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/documents/pdf/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\": {}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Branding is resolved server-side and injected before dispatch")
    void brandingResolvedAndInjected() throws Exception {
        when(documentGenerationService.generate(any(DocumentRequest.class)))
                .thenReturn(DocumentResponse.of(new byte[]{1}, DocumentFormat.PDF, "invoice.pdf"));

        mockMvc.perform(post("/api/v1/documents/pdf/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DocumentGenerationRequestDto("invoice", Map.of("invoice.number", "INV-001")))))
                .andExpect(status().isOk());

        ArgumentCaptor<DocumentRequest> captor = ArgumentCaptor.forClass(DocumentRequest.class);
        verify(documentGenerationService).generate(captor.capture());

        DocumentRequest sent = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(sent.tenantId()).isEqualTo(TENANT_UUID.toString());
        org.assertj.core.api.Assertions.assertThat(sent.data())
                .containsEntry("branding.primaryColor", "#2563EB")
                .containsEntry("branding.logoUrl", "https://cdn.kitehub.me/logo.png")
                .containsEntry("branding.displayName", "Kite Education Center")
                .containsEntry("invoice.number", "INV-001");
    }
}
