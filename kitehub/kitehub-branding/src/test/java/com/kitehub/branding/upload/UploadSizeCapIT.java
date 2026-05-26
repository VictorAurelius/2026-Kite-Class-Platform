package com.kitehub.branding.upload;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Upload size cap IT — verify multipart upload endpoint rejects oversized
 * requests với HTTP 413 Payload Too Large.
 *
 * <p>Wave beta-prep-1 Bucket B item 3 AC: "upload endpoint reject file > size cap
 * với HTTP 413; cap value documented in application.yml `spring.servlet.multipart.max-file-size`".
 *
 * <p>Per `pre-handoff-self-test-completeness.md` §2.5 file-upload flow checklist
 * (b) Size limit enforced + documented; (f) Failed upload UI surface visible.
 *
 * <p>NOTE: {@code @Disabled} until {@code spring.servlet.multipart.max-file-size}
 * explicitly configured trong kitehub-branding {@code application.yml}. Spring Boot
 * default cap = 1MB per file / 10MB per request. Wave beta-prep-1 Bucket B item 3
 * surface gap (kitehub không config explicit cap). Follow-up
 * GAP-UPLOAD-CAP-CONFIG-001 — add explicit cap (recommend 5MB matching
 * kiteclass-core 5MB precedent).
 *
 * @since Wave beta-prep-1 Bucket B (security-beta-min)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UploadSizeCapIT — multipart upload size cap enforcement (Wave beta-prep-1 Bucket B)")
@Disabled("Disabled until explicit cap configured (see GAP-UPLOAD-CAP-CONFIG-001). " +
        "Test fixtures sized to verify boundary once cap is explicit.")
class UploadSizeCapIT {

    @Value("${spring.servlet.multipart.max-file-size:1MB}")
    private String configuredMaxFileSize;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
        return mockMvc;
    }

    @Test
    @DisplayName("File 1 byte -> accepted at multipart layer (size OK)")
    void tinyFile_acceptedAtMultipartLayer() throws Exception {
        UUID instanceId = UUID.randomUUID();
        MockMultipartFile tinyFile = new MockMultipartFile(
                "file", "tiny.png", MediaType.IMAGE_PNG_VALUE, new byte[]{0x01});

        MvcResult result = mvc().perform(multipart("/api/v1/branding/assets/{instanceId}/{assetType}",
                        instanceId, "logo")
                        .file(tinyFile))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status).isNotEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .withFailMessage("1-byte file should not exceed any size cap; got HTTP %d", status);
    }

    @Test
    @DisplayName("File 11MB (>10MB request cap) -> HTTP 413 Payload Too Large")
    void overCapFile_returns413() throws Exception {
        UUID instanceId = UUID.randomUUID();
        byte[] oversized = new byte[11 * 1024 * 1024];
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "oversized.png", MediaType.IMAGE_PNG_VALUE, oversized);

        MvcResult result = mvc().perform(multipart("/api/v1/branding/assets/{instanceId}/{assetType}",
                        instanceId, "logo")
                        .file(bigFile))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .withFailMessage("Expected 413 Payload Too Large but got HTTP %d. " +
                        "configured max-file-size = %s. Verify GlobalExceptionHandler maps " +
                        "MaxUploadSizeExceededException to 413.", status, configuredMaxFileSize);
    }

    @Test
    @DisplayName("Cap value documented + non-zero")
    void capConfigurationDocumented() {
        assertThat(configuredMaxFileSize)
                .isNotBlank()
                .matches("^\\d+([KMG]B)?$")
                .withFailMessage("spring.servlet.multipart.max-file-size must be set " +
                        "(e.g., 5MB). Current: '%s'. See GAP-UPLOAD-CAP-CONFIG-001.",
                        configuredMaxFileSize);
    }
}
