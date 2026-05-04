package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentFeeFacetResponse;
import com.kiteclass.core.module.parent.service.ParentFeesFacetService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentFeesFacetController.class)
@AutoConfigureMockMvc
@Import({ParentFeesFacetControllerIT.TestSecurityConfig.class,
        ParentFeesFacetControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentFeesFacetController IT")
class ParentFeesFacetControllerIT {

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
        ParentFeesFacetService service() {
            return Mockito.mock(ParentFeesFacetService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ParentFeesFacetService service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("GET /api/v1/parent/children/{id}/fees — 200 with linked parent")
    void linked_returns200() throws Exception {
        ParentFeeFacetResponse f = new ParentFeeFacetResponse(
                1L, CHILD_ID, "INV-2026-0001", "SENT",
                new BigDecimal("1500000.00"), new BigDecimal("1500000.00"),
                LocalDate.parse("2026-04-30"));
        Page<ParentFeeFacetResponse> page = new PageImpl<>(List.of(f), Pageable.unpaged(), 1);
        when(service.getFeesForChild(eq(PARENT_ID), eq(CHILD_ID), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/parent/children/{childId}/fees", CHILD_ID)
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-2026-0001"))
                .andExpect(jsonPath("$.data.content[0].status").value("SENT"));
    }

    @Test
    @DisplayName("returns 403 when ParentStudentLink missing")
    void unlinkedParent_returns403() throws Exception {
        when(service.getFeesForChild(eq(PARENT_ID), eq(CHILD_ID), any(), any(), any()))
                .thenThrow(new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/fees", CHILD_ID)
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isForbidden());
    }
}
