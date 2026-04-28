package com.kitehub.branding.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GAP-258 input prompt token cap.
 *
 * <p>Validates the controller-level guard rejects oversized prompts before any
 * AI provider call (cost-attack defense).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Branding Controller — Input Cap (GAP-258)")
class BrandingControllerInputCapIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("FREE tier oversize organizationName → 400 AI_INPUT_TOO_LONG")
    void freeTierOversizePromptRejected() throws Exception {
        // FREE cap = 2000 tokens. A 9000-char organization name yields ~2250 tokens.
        String oversize = "x".repeat(9000);
        Map<String, String> body = Map.of(
                "logoUrl", "https://example.com/logo.png",
                "organizationName", oversize
        );

        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/ai/analyze-logo")
                .header("X-Subscription-Tier", "FREE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("AI_INPUT_TOO_LONG"))
            .andExpect(jsonPath("$.maxTokens").value(2000))
            .andExpect(jsonPath("$.tier").value("FREE"));
    }

    @Test
    @DisplayName("FREE tier within cap → request proceeds (mock AI returns brand colors)")
    void freeTierWithinCapAllowed() throws Exception {
        Map<String, String> body = Map.of(
                "logoUrl", "https://example.com/logo.png",
                "organizationName", "Test School"
        );

        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/ai/analyze-logo")
                .header("X-Subscription-Tier", "FREE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PREMIUM tier accepts prompts that FREE rejects")
    void premiumTierAcceptsLargerPrompt() throws Exception {
        // 12000 chars = ~3000 tokens. FREE cap 2000 (reject), PREMIUM cap 8000 (allow).
        String mediumLarge = "x".repeat(12000);
        Map<String, String> body = Map.of(
                "logoUrl", "https://example.com/logo.png",
                "organizationName", mediumLarge
        );

        // FREE rejects.
        MvcResult freeResult = mockMvc.perform(post("/api/platform/branding/ai/analyze-logo")
                .header("X-Subscription-Tier", "FREE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(freeResult))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("AI_INPUT_TOO_LONG"));

        // PREMIUM accepts.
        MvcResult premiumResult = mockMvc.perform(post("/api/platform/branding/ai/analyze-logo")
                .header("X-Subscription-Tier", "PREMIUM")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(premiumResult))
            .andExpect(status().isOk());
    }
}
