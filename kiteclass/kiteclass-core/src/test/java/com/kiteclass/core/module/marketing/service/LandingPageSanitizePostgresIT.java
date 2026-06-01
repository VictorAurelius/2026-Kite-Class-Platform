package com.kiteclass.core.module.marketing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test verifying GAP-827 sanitize-on-write end-to-end through the real
 * persistence layer on PostgreSQL (Testcontainers) — covers the JSONB columns
 * ({@code teachers}/{@code testimonials}/...) per {@code postgres-specific-type-testcontainers.md}
 * + Vietnamese diacritic roundtrip per {@code vn-localization-audit-checklist.md} §5.
 *
 * <p>Exercises the full {@code updateLandingPage} → MapStruct → sanitizer → save → reload path,
 * which Mockito unit tests bypass.
 *
 * @since wave-thesis-5 (GAP-827)
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
class LandingPageSanitizePostgresIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private LandingPageService landingPageService;

    @Autowired
    private LandingPageRepository landingPageRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void updateLandingPage_stripsXssAndPersistsClean() {
        ArrayNode testimonials = MAPPER.createArrayNode();
        ObjectNode t = MAPPER.createObjectNode();
        t.put("author", "Phụ huynh Trần Thị Hồng<script>steal()</script>");
        t.put("content", "Học phí hợp lý<img src=x onerror=alert(1)>");
        t.put("rating", 5);
        testimonials.add(t);

        UpdateLandingPageRequest request = UpdateLandingPageRequest.builder()
                .heroTitle("Trung tâm Anh ngữ Sky Education<script>alert(1)</script>")
                .teacherBio("<b>Cô Hồng</b> 10 năm kinh nghiệm<script>x()</script>")
                .aboutText("Giới thiệu trung tâm<iframe src=evil></iframe>")
                .testimonials(testimonials)
                .build();

        landingPageService.updateLandingPage(tenantId, request);

        // Reload from DB to confirm persisted state (not just in-memory entity)
        LandingPage reloaded = landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .orElseThrow();

        assertThat(reloaded.getHeroTitle())
                .doesNotContain("<script>")
                .contains("Trung tâm Anh ngữ Sky Education"); // VN diacritics intact
        assertThat(reloaded.getTeacherBio())
                .doesNotContain("<script>").doesNotContain("<b>")
                .contains("Cô Hồng");
        assertThat(reloaded.getAboutText())
                .doesNotContain("<iframe").contains("Giới thiệu trung tâm");

        // JSONB roundtrip: XSS stripped, VN diacritics + numeric preserved
        JsonNode persistedTestimonials = reloaded.getTestimonials();
        assertThat(persistedTestimonials.get(0).get("author").asText())
                .doesNotContain("<script>").contains("Phụ huynh Trần Thị Hồng");
        assertThat(persistedTestimonials.get(0).get("content").asText())
                .doesNotContainIgnoringCase("onerror").contains("Học phí hợp lý");
        assertThat(persistedTestimonials.get(0).get("rating").asInt()).isEqualTo(5);
    }

    @Test
    void updateLandingPage_rejectsMaliciousHeroImageUrl() {
        UpdateLandingPageRequest request = UpdateLandingPageRequest.builder()
                .heroImageUrl("https://attacker.example.com/evil.svg")
                .build();

        assertThatThrownBy(() -> landingPageService.updateLandingPage(tenantId, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateLandingPage_vnDiacriticRoundtripFullyPreserved() {
        // Wave 106 GAP-764 class — must NOT corrupt VN diacritics to HTML entities
        String orgName = "Trung tâm Tin học Bách Khoa — Cô Nguyễn Thị Ánh Tuyết";
        UpdateLandingPageRequest request = UpdateLandingPageRequest.builder()
                .heroTitle(orgName)
                .teacherBio("â ê ô ữ ồ ằ ấ đ ươ")
                .build();

        LandingPageResponse resp = landingPageService.updateLandingPage(tenantId, request);
        assertThat(resp.getHeroTitle()).isEqualTo(orgName);

        LandingPage reloaded = landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .orElseThrow();
        assertThat(reloaded.getHeroTitle()).isEqualTo(orgName);
        assertThat(reloaded.getTeacherBio()).isEqualTo("â ê ô ữ ồ ằ ấ đ ươ");
        assertThat(reloaded.getHeroTitle()).doesNotContain("&"); // no entity corruption
    }
}
