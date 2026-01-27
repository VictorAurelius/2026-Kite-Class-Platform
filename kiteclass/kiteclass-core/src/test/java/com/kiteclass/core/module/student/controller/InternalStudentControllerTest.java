package com.kiteclass.core.module.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.service.StudentService;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link InternalStudentController}.
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@WebMvcTest(InternalStudentController.class)
@AutoConfigureMockMvc
class InternalStudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    /**
     * Test security configuration that disables security for controller tests.
     */
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void getStudent_shouldReturn200_whenValidHeaderProvided() throws Exception {
        // Given
        StudentResponse response = new StudentResponse(1L, "John Doe", "john@example.com",
                null, null, null, null, null, null, null);
        when(studentService.getStudentById(anyLong())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/internal/students/1")
                        .header("X-Internal-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("John Doe"));

        verify(studentService).getStudentById(1L);
    }

    @Test
    void createStudent_shouldReturn201_whenValidRequest() throws Exception {
        // Given
        CreateStudentRequest request = StudentTestDataBuilder.createDefaultCreateRequest();
        StudentResponse response = new StudentResponse(1L, "Test Student", "test@example.com",
                null, null, null, null, null, null, null);
        when(studentService.createStudent(any())).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Request", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(studentService).createStudent(any(CreateStudentRequest.class));
    }

    @Test
    void createStudent_shouldReturn400_whenInvalidRequest() throws Exception {
        // Given
        CreateStudentRequest invalidRequest = new CreateStudentRequest(
                null,  // name is required
                "invalid-email",  // invalid email format
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When / Then
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Request", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteStudent_shouldReturn200_whenStudentExists() throws Exception {
        // When / Then
        mockMvc.perform(delete("/internal/students/1")
                        .header("X-Internal-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(studentService).deleteStudent(1L);
    }

    @Test
    void getStudent_shouldReturn404_whenStudentNotFound() throws Exception {
        // Given
        when(studentService.getStudentById(anyLong()))
                .thenThrow(new com.kiteclass.core.common.exception.EntityNotFoundException("Student", 999L));

        // When / Then
        mockMvc.perform(get("/internal/students/999")
                        .header("X-Internal-Request", "true"))
                .andExpect(status().isNotFound());
    }
}
