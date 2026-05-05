package com.kiteclass.core.module.parent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.FileComplaintRequest;
import com.kiteclass.core.module.parent.dto.ParentComplaintResponse;
import com.kiteclass.core.module.parent.service.ParentComplaintService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentComplaintController.class)
@AutoConfigureMockMvc
@Import({ParentComplaintControllerIT.TestSecurityConfig.class,
        ParentComplaintControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentComplaintController IT")
class ParentComplaintControllerIT {

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
        ParentComplaintService service() {
            return Mockito.mock(ParentComplaintService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ParentComplaintService service;
    @Autowired private ObjectMapper objectMapper;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("POST /api/v1/parent/complaints — 201 returns ticket id")
    void fileComplaint_returns201() throws Exception {
        ParentComplaintResponse resp = new ParentComplaintResponse(
                42L, CHILD_ID, "PENDING", Instant.parse("2026-05-05T00:00:00Z"));
        when(service.fileComplaint(eq(PARENT_ID), any())).thenReturn(resp);

        FileComplaintRequest body = new FileComplaintRequest(
                CHILD_ID, "Tôi muốn khiếu nại về điểm thi giữa kỳ của con.");

        mockMvc.perform(post("/api/v1/parent/complaints")
                        .header("X-User-Reference-Id", PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST — 401 without X-User-Reference-Id")
    void missingHeader_returns401() throws Exception {
        FileComplaintRequest body = new FileComplaintRequest(
                CHILD_ID, "Tôi muốn khiếu nại về điểm thi giữa kỳ.");

        mockMvc.perform(post("/api/v1/parent/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST — 403 PARENT_FACET_FORBIDDEN when no link")
    void unlinkedParent_returns403() throws Exception {
        when(service.fileComplaint(eq(PARENT_ID), any()))
                .thenThrow(new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN));

        FileComplaintRequest body = new FileComplaintRequest(
                CHILD_ID, "Tôi muốn khiếu nại về điểm thi giữa kỳ.");

        mockMvc.perform(post("/api/v1/parent/complaints")
                        .header("X-User-Reference-Id", PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST — 400 when complaintText is too short")
    void shortBody_returns400() throws Exception {
        FileComplaintRequest body = new FileComplaintRequest(CHILD_ID, "x");

        mockMvc.perform(post("/api/v1/parent/complaints")
                        .header("X-User-Reference-Id", PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
