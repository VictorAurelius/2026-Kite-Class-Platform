package com.kiteclass.core.module.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.service.CourseService;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CourseController}.
 *
 * <p>Uses @TestConfiguration to provide mock beans instead of deprecated @MockBean.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@WebMvcTest(CourseController.class)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseService courseService;

    /**
     * Test configuration providing mock beans.
     * Replaces deprecated @MockBean annotation.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public CourseService courseService() {
            return mock(CourseService.class);
        }
    }

    @Test
    void createCourse_shouldReturnCreated() throws Exception {
        // Given
        CreateCourseRequest request = CourseTestDataBuilder.createDefaultCreateRequest();
        CourseResponse response = new CourseResponse(
                1L, request.name(), request.code(), request.description(),
                request.syllabus(), request.objectives(), request.prerequisites(),
                request.targetAudience(), request.teacherId(), request.durationWeeks(),
                request.totalSessions(), request.price(), "DRAFT", null, null, null
        );

        when(courseService.createCourse(any(CreateCourseRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(request.name()))
                .andExpect(jsonPath("$.data.code").value(request.code()));

        verify(courseService).createCourse(any(CreateCourseRequest.class));
    }

    @Test
    void getCourseById_shouldReturnCourse() throws Exception {
        // Given
        CourseResponse response = new CourseResponse(
                1L, "Test Course", "TEST-001", "Description",
                "Syllabus", "Objectives", "Prerequisites", "Target Audience",
                1L, 12, 36, new BigDecimal("5000000.00"), "DRAFT",
                "https://example.com/cover.jpg", null, null
        );

        when(courseService.getCourseById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("TEST-001"));

        verify(courseService).getCourseById(1L);
    }

    @Test
    void updateCourse_shouldReturnUpdated() throws Exception {
        // Given
        UpdateCourseRequest request = CourseTestDataBuilder.createDefaultUpdateRequest();
        CourseResponse response = new CourseResponse(
                1L, request.name(), "TEST-001", request.description(),
                request.syllabus(), request.objectives(), request.prerequisites(),
                request.targetAudience(), 1L, request.durationWeeks(),
                request.totalSessions(), request.price(), "DRAFT",
                request.coverImageUrl(), null, null
        );

        when(courseService.updateCourse(eq(1L), any(UpdateCourseRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/courses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(request.name()));

        verify(courseService).updateCourse(eq(1L), any(UpdateCourseRequest.class));
    }

    @Test
    void deleteCourse_shouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(courseService).deleteCourse(1L);
    }

    @Test
    void publishCourse_shouldReturnPublished() throws Exception {
        // Given
        CourseResponse response = new CourseResponse(
                1L, "Test Course", "TEST-001", "Description",
                "Syllabus", "Objectives", "Prerequisites", "Target Audience",
                1L, 12, 36, new BigDecimal("5000000.00"), "PUBLISHED",
                "https://example.com/cover.jpg", null, null
        );

        when(courseService.publishCourse(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/courses/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(courseService).publishCourse(1L);
    }

    @Test
    void archiveCourse_shouldReturnArchived() throws Exception {
        // Given
        CourseResponse response = new CourseResponse(
                1L, "Test Course", "TEST-001", "Description",
                "Syllabus", "Objectives", "Prerequisites", "Target Audience",
                1L, 12, 36, new BigDecimal("5000000.00"), "ARCHIVED",
                "https://example.com/cover.jpg", null, null
        );

        when(courseService.archiveCourse(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/courses/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        verify(courseService).archiveCourse(1L);
    }
}
