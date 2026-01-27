package com.kiteclass.core.module.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.service.StudentService;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link StudentController}.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    @Test
    void createStudent_shouldReturn201() throws Exception {
        // Given
        CreateStudentRequest request = StudentTestDataBuilder.createDefaultCreateRequest();
        StudentResponse response = new StudentResponse(1L, "Test", "test@example.com",
                null, null, null, null, null, null, null);
        when(studentService.createStudent(any())).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getStudentById_shouldReturn200() throws Exception {
        // Given
        StudentResponse response = new StudentResponse(1L, "Test", "test@example.com",
                null, null, null, null, null, null, null);
        when(studentService.getStudentById(anyLong())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getStudents_shouldReturn200WithPagedResults() throws Exception {
        // Given
        StudentResponse response = new StudentResponse(1L, "Test", "test@example.com",
                null, null, null, null, null, null, null);
        PageResponse<StudentResponse> pageResponse = PageResponse.of(List.of(response), 0, 20, 1);
        when(studentService.getStudents(any(), any(), any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void updateStudent_shouldReturn200() throws Exception {
        // Given
        UpdateStudentRequest request = StudentTestDataBuilder.createDefaultUpdateRequest();
        StudentResponse response = new StudentResponse(1L, "Updated", "updated@example.com",
                null, null, null, null, null, null, null);
        when(studentService.updateStudent(anyLong(), any())).thenReturn(response);

        // When / Then
        mockMvc.perform(put("/api/v1/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteStudent_shouldReturn200() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/v1/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
