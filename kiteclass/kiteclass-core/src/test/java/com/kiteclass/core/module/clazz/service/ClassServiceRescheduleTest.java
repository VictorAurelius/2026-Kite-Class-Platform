package com.kiteclass.core.module.clazz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.clazz.RescheduleReasonCategory;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.RescheduleClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.mapper.ClassMapper;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.clazz.service.impl.ClassServiceImpl;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.testutil.ClassTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClassServiceImpl#rescheduleClass(Long, RescheduleClassRequest)}.
 *
 * <p>Wave beta-readiness-4 Bucket D — GAP-291. Validates Cal.com pattern:
 * <ul>
 *   <li>Mutate startDate + endDate IN-PLACE; status PRESERVED (no new RESCHEDULED enum)</li>
 *   <li>Audit columns (5 mandatory + 1 optional notes) populated</li>
 *   <li>Outbox event {@code class.rescheduled} published in same txn</li>
 *   <li>Reject when status != SCHEDULED (preserves attendance history)</li>
 *   <li>Reject when newEndDate <= newStartDate (BR-CLASS-005)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceRescheduleTest {

    @Mock private ClassRepository classRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ClassMapper classMapper;
    @Mock private RecurrenceService recurrenceService;
    @Mock private OutboxEventWriter outboxEventWriter;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules() // JSR-310 — LocalDate / Instant
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @InjectMocks
    private ClassServiceImpl classService;

    private static final UUID TENANT_ID = ClassTestDataBuilder.DEFAULT_TENANT;
    private Class scheduledClass;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID);
        // VN sample data: Lớp Anh ngữ 5A1 thuộc Trung tâm Anh ngữ Sky Education
        scheduledClass = ClassTestDataBuilder.createDefaultClass();
        scheduledClass.setName("Lớp Anh ngữ 5A1");
        scheduledClass.setStartDate(LocalDate.of(2026, 5, 14)); // Thứ Hai, 14/05/2026
        scheduledClass.setEndDate(LocalDate.of(2026, 6, 30));
        scheduledClass.setStatus(ClassStatus.SCHEDULED);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rescheduleClass_shouldMutateDatesAndPreserveStatus() {
        // Given — happy path: reschedule lớp Anh ngữ 5A1 từ 14/05 sang Thứ Hai 21/05/2026
        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(java.util.Optional.of(scheduledClass));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));
        ClassResponse expectedResponse = mockResponse();
        when(classMapper.toResponse(any(Class.class))).thenReturn(expectedResponse);

        RescheduleClassRequest request = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.GV_OM_BAN_DOT_XUAT,
                "Cô giáo phụ trách lớp xin nghỉ ốm 1 tuần."
        );

        // When
        ClassResponse result = classService.rescheduleClass(1L, request);

        // Then — entity mutated correctly
        ArgumentCaptor<Class> savedCaptor = ArgumentCaptor.forClass(Class.class);
        verify(classRepository).save(savedCaptor.capture());
        Class saved = savedCaptor.getValue();

        // Status PRESERVED — Cal.com pattern, no new RESCHEDULED enum
        assertThat(saved.getStatus()).isEqualTo(ClassStatus.SCHEDULED);
        // Dates mutated
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 21));
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        // Audit columns captured
        assertThat(saved.getPreviousStartDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(saved.getPreviousEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(saved.getRescheduledAt()).isNotNull();
        assertThat(saved.getRescheduleReasonCategory()).isEqualTo("GV_OM_BAN_DOT_XUAT");
        assertThat(saved.getRescheduleReasonNotes())
                .isEqualTo("Cô giáo phụ trách lớp xin nghỉ ốm 1 tuần.");

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void rescheduleClass_shouldPublishOutboxEvent() {
        // Given
        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(java.util.Optional.of(scheduledClass));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(Class.class))).thenReturn(mockResponse());

        RescheduleClassRequest request = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.LE_TET_NGHI_CHINH_THUC,
                null
        );

        // When
        classService.rescheduleClass(1L, request);

        // Then — Outbox event published with correct routing key + payload
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventWriter).enqueue(
                eq("class.rescheduled"),
                eq("Class"),
                eq("1"),
                payloadCaptor.capture()
        );
        String payloadJson = payloadCaptor.getValue();
        // Payload contains audit fields
        assertThat(payloadJson).contains("\"classId\":1");
        assertThat(payloadJson).contains("\"reasonCategory\":\"LE_TET_NGHI_CHINH_THUC\"");
        assertThat(payloadJson).contains("\"previousStartDate\":\"2026-05-14\"");
        assertThat(payloadJson).contains("\"newStartDate\":\"2026-05-21\"");
    }

    @Test
    void rescheduleClass_shouldRejectWhenStatusNotScheduled() {
        // Given — IN_PROGRESS class (attendance history must be preserved)
        scheduledClass.setStatus(ClassStatus.IN_PROGRESS);
        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(java.util.Optional.of(scheduledClass));

        RescheduleClassRequest request = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.GV_OM_BAN_DOT_XUAT,
                null
        );

        // When + Then
        assertThatThrownBy(() -> classService.rescheduleClass(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CLASS_CANNOT_RESCHEDULE");

        // Outbox NOT published when validation fails
        verify(outboxEventWriter, never()).enqueue(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rescheduleClass_shouldRejectWhenEndDateBeforeStartDate() {
        // Given
        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(java.util.Optional.of(scheduledClass));

        RescheduleClassRequest invalidRequest = new RescheduleClassRequest(
                LocalDate.of(2026, 7, 7),
                LocalDate.of(2026, 5, 21), // end BEFORE start
                RescheduleReasonCategory.MAT_DIEN_INTERNET,
                null
        );

        // When + Then
        assertThatThrownBy(() -> classService.rescheduleClass(1L, invalidRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CLASS_INVALID_DATES");

        verify(classRepository, never()).save(any());
        verify(outboxEventWriter, never()).enqueue(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rescheduleClass_shouldPreserveEnrollmentCount() {
        // Given — class với 12 học sinh đã enroll (Trần Thị Hồng, Nguyễn Văn An, etc.)
        scheduledClass.setCurrentEnrolled(12);
        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(java.util.Optional.of(scheduledClass));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(Class.class))).thenReturn(mockResponse());

        RescheduleClassRequest request = new RescheduleClassRequest(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 7, 7),
                RescheduleReasonCategory.PHONG_HOC_KHONG_KHA_DUNG,
                null
        );

        // When
        classService.rescheduleClass(1L, request);

        // Then — currentEnrolled count preserved (no delete/recreate)
        ArgumentCaptor<Class> savedCaptor = ArgumentCaptor.forClass(Class.class);
        verify(classRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCurrentEnrolled())
                .as("Enrollment must NOT be wiped — Cal.com update-in-place preserves links")
                .isEqualTo(12);
    }

    private ClassResponse mockResponse() {
        return new ClassResponse(
                1L, ClassTestDataBuilder.DEFAULT_COURSE_ID, "Lớp Anh ngữ 5A1", "", "",
                Class.LocationType.IN_PERSON, "", LocalDate.of(2026, 5, 21), LocalDate.of(2026, 7, 7),
                20, 0, null, null,
                ClassStatus.SCHEDULED, null, null, null,
                java.time.Instant.now(), java.time.Instant.now()
        );
    }
}
