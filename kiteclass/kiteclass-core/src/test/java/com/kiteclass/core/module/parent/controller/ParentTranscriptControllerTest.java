package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.TranscriptResponse;
import com.kiteclass.core.module.parent.service.ParentTranscriptService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link ParentTranscriptController}.
 *
 * <p>Verifies HTTP behaviour:
 * <ul>
 *   <li>200 + payload when service returns transcripts.</li>
 *   <li>401 when {@code X-User-Reference-Id} header missing.</li>
 *   <li>403 propagated when service throws {@code PARENT_NOT_LINKED}.</li>
 * </ul>
 *
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */
@WebMvcTest(ParentTranscriptController.class)
@AutoConfigureMockMvc
@Import({ParentTranscriptControllerTest.TestSecurityConfig.class,
        ParentTranscriptControllerTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentTranscriptController")
class ParentTranscriptControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        ParentTranscriptService parentTranscriptService() {
            return Mockito.mock(ParentTranscriptService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ParentTranscriptService parentTranscriptService;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("GET /api/v1/parent/children/{childId}/transcript → 200 with linked parent header")
    void linked_returns200() throws Exception {
        TranscriptResponse t = new TranscriptResponse(
                1L, CHILD_ID, "Spring 2026", 2026,
                new BigDecimal("12.00"), new BigDecimal("3.45"), new BigDecimal("3.52"),
                4, 4, 0
        );
        when(parentTranscriptService.getTranscriptsForChild(eq(PARENT_ID), eq(CHILD_ID)))
                .thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/transcript", CHILD_ID)
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].semester").value("Spring 2026"))
                .andExpect(jsonPath("$.data[0].academicYear").value(2026))
                .andExpect(jsonPath("$.data[0].semesterGpa").value(3.45));
    }

    @Test
    @DisplayName("returns 401 when X-User-Reference-Id header is missing")
    void missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/{childId}/transcript", CHILD_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 403 when service rejects unlinked parent (scope guard)")
    void unlinkedParent_returns403() throws Exception {
        when(parentTranscriptService.getTranscriptsForChild(eq(PARENT_ID), eq(CHILD_ID)))
                .thenThrow(new BusinessException("PARENT_NOT_LINKED", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/transcript", CHILD_ID)
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 200 + empty list when child has no transcripts yet")
    void emptyList() throws Exception {
        when(parentTranscriptService.getTranscriptsForChild(eq(PARENT_ID), eq(CHILD_ID)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parent/children/{childId}/transcript", CHILD_ID)
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
