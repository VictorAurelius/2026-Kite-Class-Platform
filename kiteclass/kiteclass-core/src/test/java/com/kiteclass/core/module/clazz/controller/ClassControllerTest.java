package com.kiteclass.core.module.clazz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CancelClassRequest;
import com.kiteclass.core.module.clazz.dto.ClassCodeResponse;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.service.ClassService;
import com.kiteclass.core.testutil.ClassTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link ClassController}.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@WebMvcTest(ClassController.class)
@ActiveProfiles("test")
@Import({ ClassControllerTest.MockConfig.class, TestSecurityConfig.class, TestTenantContextFilter.class })
@DisplayName("ClassController Tests")
class ClassControllerTest {

        @TestConfiguration
        static class MockConfig {
                @Bean
                @Primary
                public ClassService classService() {
                        return Mockito.mock(ClassService.class);
                }
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private ClassService classService;

        private ClassResponse defaultResponse;

        @BeforeEach
        void setUp() {
                Mockito.reset(classService);
                com.kiteclass.core.module.clazz.entity.Class clazz = ClassTestDataBuilder.createDefaultClass();
                defaultResponse = new ClassResponse(
                                1L, 1L, "English B1 - Evening Class", "Description",
                                "Mon-Wed-Fri 18:00-20:00", Class.LocationType.IN_PERSON, "Room 101",
                                clazz.getStartDate(), clazz.getEndDate(), 20, 0, null, null,
                                ClassStatus.SCHEDULED, null, null, null, Instant.now(), null);
        }

        // =========================================================================
        // POST /api/v1/courses/{courseId}/classes
        // =========================================================================

        @Test
        @DisplayName("createClass should return 201 with valid request")
        void createClass_shouldReturn201_withValidRequest() throws Exception {
                when(classService.createClass(eq(1L), any())).thenReturn(defaultResponse);

                mockMvc.perform(post("/api/v1/courses/1/classes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                ClassTestDataBuilder.createDefaultCreateRequest())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.name").value("English B1 - Evening Class"))
                                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
        }

        @Test
        @DisplayName("createClass should return 400 when name too short")
        void createClass_shouldReturn400_whenNameTooShort() throws Exception {
                CreateClassRequest badRequest = new CreateClassRequest(
                                "AB", null, null, null, null, null, null, 10);

                mockMvc.perform(post("/api/v1/courses/1/classes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("createClass should return 400 when maxStudents is zero")
        void createClass_shouldReturn400_whenMaxStudentsZero() throws Exception {
                CreateClassRequest badRequest = new CreateClassRequest(
                                "Valid Name", null, null, null, null, null, null, 0);

                mockMvc.perform(post("/api/v1/courses/1/classes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badRequest)))
                                .andExpect(status().isBadRequest());
        }

        // =========================================================================
        // GET /api/v1/classes (flat tenant-scoped list)
        // =========================================================================

        @Test
        @DisplayName("listAllClasses should return 200 with paginated tenant-scoped list")
        void listAllClasses_shouldReturn200_withPagingShape() throws Exception {
                PageResponse<ClassResponse> page = PageResponse.of(List.of(defaultResponse), 0, 1, 1);
                when(classService.listAllClasses(any())).thenReturn(page);

                mockMvc.perform(get("/api/v1/classes?page=0&size=1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content[0].id").value(1))
                                .andExpect(jsonPath("$.data.page").value(0))
                                .andExpect(jsonPath("$.data.size").value(1))
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.totalPages").value(1))
                                .andExpect(jsonPath("$.data.first").value(true))
                                .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("listAllClasses should default to empty list when tenant has no classes")
        void listAllClasses_shouldReturnEmpty_whenNoClasses() throws Exception {
                PageResponse<ClassResponse> empty = PageResponse.of(List.of(), 0, 20, 0);
                when(classService.listAllClasses(any())).thenReturn(empty);

                mockMvc.perform(get("/api/v1/classes"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // =========================================================================
        // GET /api/v1/classes/{classId}
        // =========================================================================

        @Test
        @DisplayName("getClass should return 200 when class found")
        void getClass_shouldReturn200_whenFound() throws Exception {
                when(classService.getClass(1L)).thenReturn(defaultResponse);

                mockMvc.perform(get("/api/v1/classes/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.courseId").value(1));
        }

        @Test
        @DisplayName("getClass should return 404 when class not found")
        void getClass_shouldReturn404_whenNotFound() throws Exception {
                when(classService.getClass(999L))
                                .thenThrow(new EntityNotFoundException("CLASS_NOT_FOUND", (Object) 999L));

                mockMvc.perform(get("/api/v1/classes/999"))
                                .andExpect(status().isNotFound());
        }

        // =========================================================================
        // Lifecycle transitions
        // =========================================================================

        @Test
        @DisplayName("startClass should return 200 when class is SCHEDULED")
        void startClass_shouldReturn200_whenScheduled() throws Exception {
                ClassResponse inProgressResponse = new ClassResponse(
                                1L, 1L, "English B1", null, null, Class.LocationType.IN_PERSON, null,
                                null, null, 20, 0, null, null,
                                ClassStatus.IN_PROGRESS, Instant.now(), null, null, Instant.now(), null);
                when(classService.startClass(1L)).thenReturn(inProgressResponse);

                mockMvc.perform(post("/api/v1/classes/1/start"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("startClass should return 400 when class already started")
        void startClass_shouldReturn400_whenAlreadyStarted() throws Exception {
                when(classService.startClass(1L))
                                .thenThrow(new BusinessException("CLASS_CANNOT_START",
                                                org.springframework.http.HttpStatus.BAD_REQUEST));

                mockMvc.perform(post("/api/v1/classes/1/start"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("completeClass should return 200 with COMPLETED status")
        void completeClass_shouldReturn200_whenInProgress() throws Exception {
                ClassResponse completedResponse = new ClassResponse(
                                1L, 1L, "English B1", null, null, Class.LocationType.IN_PERSON, null,
                                null, null, 20, 5, null, null,
                                ClassStatus.COMPLETED, Instant.now(), Instant.now(), null, Instant.now(), null);
                when(classService.completeClass(1L)).thenReturn(completedResponse);

                mockMvc.perform(post("/api/v1/classes/1/complete"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("cancelClass should return 200 when reason provided")
        void cancelClass_shouldReturn200_withReason() throws Exception {
                ClassResponse cancelledResponse = new ClassResponse(
                                1L, 1L, "English B1", null, null, Class.LocationType.IN_PERSON, null,
                                null, null, 20, 0, null, null,
                                ClassStatus.CANCELLED, null, null, Instant.now(), Instant.now(), null);
                when(classService.cancelClass(eq(1L), any())).thenReturn(cancelledResponse);

                mockMvc.perform(post("/api/v1/classes/1/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                ClassTestDataBuilder.createCancelRequest())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("cancelClass should return 400 when reason is blank")
        void cancelClass_shouldReturn400_whenReasonBlank() throws Exception {
                CancelClassRequest badRequest = new CancelClassRequest("");

                mockMvc.perform(post("/api/v1/classes/1/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badRequest)))
                                .andExpect(status().isBadRequest());
        }

        // =========================================================================
        // DELETE
        // =========================================================================

        @Test
        @DisplayName("deleteClass should return 204 when deleted")
        void deleteClass_shouldReturn204_whenDeleted() throws Exception {
                doNothing().when(classService).deleteClass(1L);

                mockMvc.perform(delete("/api/v1/classes/1"))
                                .andExpect(status().isNoContent());
        }

        // =========================================================================
        // Generate code
        // =========================================================================

        @Test
        @DisplayName("generateCode should return 200 with generated code")
        void generateCode_shouldReturn200_withCode() throws Exception {
                when(classService.generateClassCode(eq(1L), any()))
                                .thenReturn(new ClassCodeResponse("ABC12345", null));

                mockMvc.perform(post("/api/v1/classes/1/generate-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                ClassTestDataBuilder.createCodeRequest())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.classCode").value("ABC12345"));
        }

        // =========================================================================
        // Schedule & Sessions
        // =========================================================================

        @Test
        @DisplayName("createSchedule should return 201 with generated sessions")
        void createSchedule_shouldReturn201_withSessions() throws Exception {
                List<ClassSessionResponse> sessions = List.of(
                                new ClassSessionResponse(1L, 1L, 1, null, null, null, null, null, null, false),
                                new ClassSessionResponse(2L, 1L, 2, null, null, null, null, null, null, false));
                when(classService.createSchedule(eq(1L), any())).thenReturn(sessions);

                mockMvc.perform(post("/api/v1/classes/1/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                ClassTestDataBuilder.createScheduleRequest())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("listSessions should return 200 with session list")
        void listSessions_shouldReturn200_withSessionList() throws Exception {
                when(classService.listSessions(1L)).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/classes/1/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray());
        }
}
