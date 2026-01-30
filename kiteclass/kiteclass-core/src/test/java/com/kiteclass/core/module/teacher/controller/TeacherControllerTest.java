package com.kiteclass.core.module.teacher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.service.TeacherService;
import com.kiteclass.core.testutil.TeacherTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link TeacherController}.
 *
 * <p>Uses @TestConfiguration to provide mock beans instead of deprecated @MockBean.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@WebMvcTest(TeacherController.class)
@ActiveProfiles("test")
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeacherService teacherService;

    /**
     * Test configuration providing mock beans.
     * Replaces deprecated @MockBean annotation.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public TeacherService teacherService() {
            return mock(TeacherService.class);
        }
    }

    @Test
    void createTeacher_shouldReturnCreated() throws Exception {
        // Given
        CreateTeacherRequest request = TeacherTestDataBuilder.createDefaultCreateRequest();
        TeacherResponse response = new TeacherResponse(
                1L, request.name(), request.email(), request.phoneNumber(),
                request.specialization(), request.bio(), request.qualification(),
                request.experienceYears(), null, "ACTIVE"
        );

        when(teacherService.createTeacher(any(CreateTeacherRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(request.name()));

        verify(teacherService).createTeacher(any(CreateTeacherRequest.class));
    }

    @Test
    void getTeacherById_shouldReturnTeacher() throws Exception {
        // Given
        TeacherResponse response = new TeacherResponse(
                1L, "John Smith", "john@example.com", "0123456789",
                "English", "Bio", "Qualification", 10, null, "ACTIVE"
        );

        when(teacherService.getTeacherById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(teacherService).getTeacherById(1L);
    }

    @Test
    void updateTeacher_shouldReturnUpdated() throws Exception {
        // Given
        UpdateTeacherRequest request = TeacherTestDataBuilder.createDefaultUpdateRequest();
        TeacherResponse response = new TeacherResponse(
                1L, request.name(), "john@example.com", request.phoneNumber(),
                request.specialization(), request.bio(), request.qualification(),
                request.experienceYears(), null, request.status().name()
        );

        when(teacherService.updateTeacher(eq(1L), any(UpdateTeacherRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/teachers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(request.name()));

        verify(teacherService).updateTeacher(eq(1L), any(UpdateTeacherRequest.class));
    }

    @Test
    void deleteTeacher_shouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(teacherService).deleteTeacher(1L);
    }
}
