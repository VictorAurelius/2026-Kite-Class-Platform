package com.kitehub.subscription.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.controller.AdminEmailController;
import com.kitehub.subscription.dto.EmailConfigResponse;
import com.kitehub.subscription.dto.EmailStatsResponse;
import com.kitehub.subscription.dto.TriggerEmailRequest;
import com.kitehub.subscription.service.EmailAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Contract Tests for AdminEmailController.
 * <p>
 * Verifies that email admin API responses conform to the documented schema.
 * Tests JSON structure (field names, types), NOT business logic values.
 * <p>
 * Breaking a test here means a frontend-visible API change was introduced.
 *
 * @since 1.0.0
 */
@WebMvcTest(AdminEmailController.class)
@DisplayName("AdminEmail API Contract Tests")
class AdminEmailApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailAdminService emailAdminService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private com.kitehub.subscription.config.AdminApiKeyInterceptor adminApiKeyInterceptor;

    @Nested
    @DisplayName("GET /api/platform/admin/emails/stats")
    class GetEmailStats {

        @Test
        @DisplayName("Response schema: has all required aggregate fields")
        void responseSchema_hasAllFields() throws Exception {
            EmailStatsResponse stats = EmailStatsResponse.builder()
                    .totalSentToday(42)
                    .totalSentThisWeek(200)
                    .failedToday(3)
                    .countByType(Map.of("trial-warning", 15L, "renewal-reminder", 8L))
                    .build();

            when(emailAdminService.getEmailStats()).thenReturn(stats);

            mockMvc.perform(get("/api/platform/admin/emails/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSentToday").isNumber())
                    .andExpect(jsonPath("$.totalSentThisWeek").isNumber())
                    .andExpect(jsonPath("$.failedToday").isNumber())
                    .andExpect(jsonPath("$.countByType").isMap());
        }
    }

    @Nested
    @DisplayName("GET /api/platform/admin/emails/config")
    class GetEmailConfig {

        @Test
        @DisplayName("Response schema: has queueEnabled and emailTypeToggles")
        void responseSchema_hasConfigFields() throws Exception {
            EmailConfigResponse config = EmailConfigResponse.builder()
                    .queueEnabled(true)
                    .emailTypeToggles(Map.of(
                            "trial-expiration-warning", true,
                            "suspension-notification", false))
                    .build();

            when(emailAdminService.getEmailConfig()).thenReturn(config);

            mockMvc.perform(get("/api/platform/admin/emails/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.queueEnabled").isBoolean())
                    .andExpect(jsonPath("$.emailTypeToggles").isMap())
                    .andExpect(jsonPath("$.emailTypeToggles['trial-expiration-warning']").isBoolean());
        }
    }

    @Nested
    @DisplayName("POST /api/platform/admin/emails/trigger")
    class TriggerEmail {

        @Test
        @DisplayName("Success: 200 OK with empty body")
        void success_returns200() throws Exception {
            doNothing().when(emailAdminService)
                    .triggerEmail(any(UUID.class), anyString());

            TriggerEmailRequest request = new TriggerEmailRequest(
                    UUID.randomUUID(), "trial-expiration-warning");

            mockMvc.perform(post("/api/platform/admin/emails/trigger")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Already sent today: service throws IllegalStateException")
        void alreadySentToday_returnsError() throws Exception {
            doThrow(new IllegalStateException("Email already sent today"))
                    .when(emailAdminService).triggerEmail(any(UUID.class), anyString());

            TriggerEmailRequest request = new TriggerEmailRequest(
                    UUID.randomUUID(), "trial-expiration-warning");

            // IllegalStateException mapped to 500 (no dedicated handler yet)
            mockMvc.perform(post("/api/platform/admin/emails/trigger")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

}
