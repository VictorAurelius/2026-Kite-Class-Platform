package com.kiteclass.core.module.parent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.parent.service.ConsentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentConsentController.class)
@AutoConfigureMockMvc
@Import({ParentConsentControllerIT.TestSecurityConfig.class,
        ParentConsentControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentConsentController IT")
class ParentConsentControllerIT {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        ConsentService consentService() {
            return Mockito.mock(ConsentService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ConsentService consentService;
    @Autowired private ObjectMapper objectMapper;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("GET /api/v1/parent/consent — 200 with default consent for unlinked child")
    void getConsent_default() throws Exception {
        when(consentService.getConsent(eq(PARENT_ID), eq(CHILD_ID)))
                .thenReturn(ParentalConsent.defaultValue());

        mockMvc.perform(get("/api/v1/parent/consent")
                        .param("childId", CHILD_ID.toString())
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.fields").isMap());
    }

    @Test
    @DisplayName("GET /api/v1/parent/consent — 401 without X-User-Reference-Id")
    void getConsent_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/parent/consent")
                        .param("childId", CHILD_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/parent/consent — 200 returns bumped consent")
    void putConsent_bumpsVersion() throws Exception {
        ParentalConsent next = new ParentalConsent(
                Map.of("fees", true), 2, Instant.parse("2026-05-05T00:00:00Z"));
        when(consentService.bumpConsent(eq(PARENT_ID), eq(CHILD_ID), any()))
                .thenReturn(next);

        String body = objectMapper.writeValueAsString(
                new ParentConsentController.UpdateRequest(Map.of("fees", true)));

        mockMvc.perform(put("/api/v1/parent/consent")
                        .param("childId", CHILD_ID.toString())
                        .header("X-User-Reference-Id", PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.fields.fees").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/parent/consent — 404 when no link exists")
    void putConsent_noLink_returns404() throws Exception {
        when(consentService.bumpConsent(eq(PARENT_ID), eq(CHILD_ID), any()))
                .thenThrow(new BusinessException(
                        "PARENT_CONSENT_LINK_NOT_FOUND", HttpStatus.NOT_FOUND));

        String body = objectMapper.writeValueAsString(
                new ParentConsentController.UpdateRequest(Map.of("fees", true)));

        mockMvc.perform(put("/api/v1/parent/consent")
                        .param("childId", CHILD_ID.toString())
                        .header("X-User-Reference-Id", PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/parent/consent — 401 without header")
    void putConsent_missingHeader_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ParentConsentController.UpdateRequest(Map.of("fees", true)));

        mockMvc.perform(put("/api/v1/parent/consent")
                        .param("childId", CHILD_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
