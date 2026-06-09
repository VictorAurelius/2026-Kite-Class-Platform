package com.kitehub.subscription.saleslead.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.saleslead.entity.SalesLead;
import com.kitehub.subscription.saleslead.service.SalesLeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link SalesLeadController} (GAP-1101).
 *
 * <p>Verifies the public endpoint returns 201 anonymously, rejects honeypot +
 * validation violations with 400, and reaches the service for valid payloads.</p>
 *
 * @since GAP-1101
 */
@WebMvcTest(controllers = SalesLeadController.class)
@Import(SecurityConfig.class)
@DisplayName("SalesLeadController")
class SalesLeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SalesLeadService service;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    private SalesLead seededSaved() {
        return SalesLead.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .fullName("Nguyễn Văn An")
                .email("an.nguyen@skyedu.vn")
                .phone("0901234567")
                .organizationName("Trung tâm Anh ngữ Sky Education")
                .planInterest("ENTERPRISE")
                .status("NEW")
                .createdAt(OffsetDateTime.parse("2026-06-09T07:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-06-09T07:00:00Z"))
                .build();
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST — 201 anonymous valid lead")
    void anonymousValidReturns201() throws Exception {
        when(service.submit(any(), any())).thenReturn(seededSaved());

        Map<String, Object> body = Map.of(
                "fullName", "Nguyễn Văn An",
                "email", "an.nguyen@skyedu.vn",
                "phone", "0901 234 567",
                "organizationName", "Trung tâm Anh ngữ Sky Education",
                "message", "Cần tư vấn gói Enterprise",
                "planInterest", "ENTERPRISE",
                "honeypot", "");

        mockMvc.perform(post("/api/platform/sales-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.planInterest").value("ENTERPRISE"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST — 400 honeypot non-empty (bot trap)")
    void honeypotNonEmptyReturns400() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Bot Submitter",
                "email", "bot@spam.com",
                "phone", "0901234567",
                "organizationName", "Spam Org",
                "honeypot", "i-am-a-bot");

        mockMvc.perform(post("/api/platform/sales-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST — 400 missing required fields")
    void missingRequiredFieldsReturns400() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "",
                "email", "not-an-email",
                "phone", "",
                "organizationName", "",
                "honeypot", "");

        mockMvc.perform(post("/api/platform/sales-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST — 400 HTML structural chars in organizationName (XSS guard)")
    void htmlCharsRejectedReturns400() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Nguyễn Văn An",
                "email", "an@skyedu.vn",
                "phone", "0901234567",
                "organizationName", "<script>alert(1)</script>",
                "honeypot", "");

        mockMvc.perform(post("/api/platform/sales-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
