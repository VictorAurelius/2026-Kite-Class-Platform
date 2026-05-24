package com.kiteclass.core.module.clazz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.RescheduleReasonCategory;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.RescheduleClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.service.ClassService;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice integration test for {@link ClassController#rescheduleClass}
 * (Wave beta-readiness-4 Bucket D — GAP-291).
 *
 * <p>Verifies HTTP endpoint behavior:
 * <ul>
 *   <li>200 OK + updated ClassResponse when request valid</li>
 *   <li>400 Bad Request when newStartDate / newEndDate / reasonCategory missing (bean validation)</li>
 *   <li>409 (via ValidationException → handler) when class not SCHEDULED</li>
 * </ul>
 *
 * <p>Note: `@PreAuthorize("@authz.hasAccessToClass(#classId)")` not enforced in
 * `@WebMvcTest` slice (TestSecurityConfig permits all). Authz integration verified
 * separately by AuthorizationBean unit tests.
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
@WebMvcTest(ClassController.class)
@ActiveProfiles("test")
@Import({ ClassControllerRescheduleIT.MockConfig.class, TestSecurityConfig.class, TestTenantContextFilter.class })
@DisplayName("ClassController Reschedule Tests")
class ClassControllerRescheduleIT {

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

    @BeforeEach
    void setUp() {
        Mockito.reset(classService);
    }

    @Test
    @DisplayName("rescheduleClass should return 200 with valid request — VN sample data")
    void rescheduleClass_shouldReturn200_withValidRequest() throws Exception {
        // Given — VN sample: Lớp Anh ngữ 5A1 từ Thứ Hai 14/05 sang Thứ Hai 21/05/2026
        ClassResponse rescheduledResponse = new ClassResponse(
                1L, 1L, "Lớp Anh ngữ 5A1", "Lớp Anh ngữ cho học sinh THCS",
                null, Class.LocationType.IN_PERSON, "Phòng 5A — 123 Lê Lợi, Q.1, TP.HCM",
                LocalDate.of(2026, 5, 21), LocalDate.of(2026, 7, 7),
                20, 12, null, null,
                ClassStatus.SCHEDULED, null, null, null,
                Instant.now(), Instant.now()
        );
        when(classService.rescheduleClass(eq(1L), any(RescheduleClassRequest.class)))
                .thenReturn(rescheduledResponse);

        RescheduleClassRequest request = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.GV_OM_BAN_DOT_XUAT,
                "Cô giáo Trần Thị Hồng xin nghỉ ốm 1 tuần."
        );

        // When + Then
        mockMvc.perform(post("/api/v1/classes/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Lớp Anh ngữ 5A1"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))  // preserved per Cal.com pattern
                .andExpect(jsonPath("$.data.startDate").value("2026-05-21"))
                .andExpect(jsonPath("$.data.endDate").value("2026-07-07"))
                .andExpect(jsonPath("$.message").value("Đã đổi lịch lớp học thành công"));
    }

    @Test
    @DisplayName("rescheduleClass should return 400 when reasonCategory missing")
    void rescheduleClass_shouldReturn400_whenReasonCategoryMissing() throws Exception {
        // Given — missing mandatory reasonCategory
        String invalidJson = """
                {
                  "newStartDate": "2026-05-21",
                  "newEndDate": "2026-07-07",
                  "reasonNotes": "no category provided"
                }
                """;

        // When + Then
        mockMvc.perform(post("/api/v1/classes/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rescheduleClass should return 400 when newStartDate missing")
    void rescheduleClass_shouldReturn400_whenStartDateMissing() throws Exception {
        String invalidJson = """
                {
                  "newEndDate": "2026-07-07",
                  "reasonCategory": "GV_OM_BAN_DOT_XUAT"
                }
                """;

        mockMvc.perform(post("/api/v1/classes/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rescheduleClass should propagate ValidationException when class non-SCHEDULED")
    void rescheduleClass_shouldPropagateValidationException_whenNonScheduledStatus() throws Exception {
        // Given — service throws (simulating IN_PROGRESS class)
        when(classService.rescheduleClass(eq(1L), any(RescheduleClassRequest.class)))
                .thenThrow(new ValidationException("CLASS_CANNOT_RESCHEDULE",
                        (Object) ClassStatus.IN_PROGRESS));

        RescheduleClassRequest request = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.LE_TET_NGHI_CHINH_THUC,
                null
        );

        // When + Then — ValidationException maps to 400 per existing exception handler
        mockMvc.perform(post("/api/v1/classes/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("rescheduleClass should accept request without optional reasonNotes")
    void rescheduleClass_shouldAccept_whenReasonNotesAbsent() throws Exception {
        ClassResponse response = new ClassResponse(
                1L, 1L, "Lớp Toán 9B", null, null, Class.LocationType.IN_PERSON, null,
                LocalDate.of(2026, 5, 21), LocalDate.of(2026, 7, 7),
                15, 8, null, null,
                ClassStatus.SCHEDULED, null, null, null,
                Instant.now(), Instant.now()
        );
        when(classService.rescheduleClass(eq(1L), any(RescheduleClassRequest.class))).thenReturn(response);

        RescheduleClassRequest minimalRequest = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.HOC_SINH_XIN_NGHI_TAP_THE,
                null
        );

        mockMvc.perform(post("/api/v1/classes/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalRequest)))
                .andExpect(status().isOk());
    }
}
