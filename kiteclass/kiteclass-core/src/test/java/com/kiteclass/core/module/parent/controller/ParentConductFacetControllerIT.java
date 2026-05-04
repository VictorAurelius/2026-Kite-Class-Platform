package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;
import com.kiteclass.core.module.parent.service.ParentConductFacetService;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentConductFacetController.class)
@AutoConfigureMockMvc
@Import({ParentConductFacetControllerIT.TestSecurityConfig.class,
        ParentConductFacetControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentConductFacetController IT")
class ParentConductFacetControllerIT {

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
        ParentConductFacetService service() {
            return Mockito.mock(ParentConductFacetService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ParentConductFacetService service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("GET /api/v1/parent/children/{id}/conduct — 200 with linked parent (empty list v1 stub)")
    void linked_returns200() throws Exception {
        when(service.getConductForChild(eq(PARENT_ID), eq(CHILD_ID), any()))
                .thenReturn(List.of(new ParentConductFacetResponse(CHILD_ID, "HK1-2025-2026", "TỐT", null)));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/conduct", CHILD_ID)
                        .param("period", "HK1-2025-2026")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rating").value("TỐT"));
    }

    @Test
    @DisplayName("returns 403 when ParentStudentLink missing")
    void unlinkedParent_returns403() throws Exception {
        when(service.getConductForChild(eq(PARENT_ID), eq(CHILD_ID), any()))
                .thenThrow(new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/conduct", CHILD_ID)
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isForbidden());
    }
}
