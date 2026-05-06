package com.kiteclass.core.module.k12.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.k12.dto.request.BulkPublishRequest;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.exception.IllegalGradeTransitionException;
import com.kiteclass.core.module.k12.service.SubjectGradeService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link SubjectGradeController} — verifies the §360.4 bulk
 * publish action correctly aggregates per-grade outcomes.
 */
@WebMvcTest(SubjectGradeController.class)
@AutoConfigureMockMvc
@Import({SubjectGradeControllerTest.TestSecurityConfig.class,
        SubjectGradeControllerTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("SubjectGradeController slice tests")
class SubjectGradeControllerTest {

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
        SubjectGradeService service() {
            return Mockito.mock(SubjectGradeService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private SubjectGradeService service;
    @Autowired private ObjectMapper objectMapper;

    private static final Long PUBLISHER = 11L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    @Test
    @DisplayName("POST /bulk-publish — skips DRAFT + PUBLISHED, publishes REVIEWED")
    void bulkPublish_skipsAlreadyPublished() throws Exception {
        Long draftId = 1L;
        Long reviewedId = 2L;
        Long publishedId = 3L;

        // DRAFT → throws (cannot skip REVIEWED)
        when(service.publish(eq(draftId), any())).thenThrow(
                new IllegalGradeTransitionException(SubjectGradeStatus.DRAFT, SubjectGradeStatus.PUBLISHED));
        // REVIEWED → succeeds
        SubjectGrade good = SubjectGrade.builder()
                .status(SubjectGradeStatus.PUBLISHED)
                .build();
        good.setId(reviewedId);
        when(service.publish(eq(reviewedId), any())).thenReturn(good);
        // PUBLISHED → throws (terminal)
        when(service.publish(eq(publishedId), any())).thenThrow(
                new IllegalGradeTransitionException(SubjectGradeStatus.PUBLISHED, SubjectGradeStatus.PUBLISHED));

        BulkPublishRequest body = new BulkPublishRequest(List.of(draftId, reviewedId, publishedId));

        mockMvc.perform(post("/api/v1/grades/subjects/bulk-publish")
                        .header("X-User-Reference-Id", PUBLISHER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publishedCount").value(1))
                .andExpect(jsonPath("$.data.skippedCount").value(2))
                .andExpect(jsonPath("$.data.errors.length()").value(2));

        verify(service, times(3)).publish(any(), eq(PUBLISHER));
    }

    @Test
    @DisplayName("POST /{id}/review — 200 returns reviewed grade")
    void review_returnsReviewedGrade() throws Exception {
        Long gradeId = 42L;
        SubjectGrade reviewed = SubjectGrade.builder()
                .status(SubjectGradeStatus.REVIEWED)
                .reviewedBy(7L)
                .build();
        reviewed.setId(gradeId);
        when(service.review(eq(gradeId), eq(7L))).thenReturn(reviewed);

        mockMvc.perform(post("/api/v1/grades/subjects/{id}/review", gradeId)
                        .header("X-User-Reference-Id", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REVIEWED"));
    }

    @Test
    @DisplayName("POST /{id}/publish — missing user header returns 401 AUTH_REQUIRED")
    void publish_missingUserHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/grades/subjects/{id}/publish", 99L))
                .andExpect(status().isUnauthorized());
    }
}
