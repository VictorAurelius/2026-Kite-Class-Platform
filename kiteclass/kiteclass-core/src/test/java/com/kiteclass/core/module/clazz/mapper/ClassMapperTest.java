package com.kiteclass.core.module.clazz.mapper;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.testutil.ClassTestDataBuilder;
import com.kiteclass.core.config.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ClassMapper}.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
class ClassMapperTest {

    @Autowired
    private ClassMapper classMapper;

    @Test
    void toEntity_shouldMapAllFields_fromCreateRequest() {
        CreateClassRequest request = ClassTestDataBuilder.createDefaultCreateRequest();

        Class entity = classMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getDescription()).isEqualTo(request.description());
        assertThat(entity.getSchedule()).isEqualTo(request.schedule());
        assertThat(entity.getLocationType()).isEqualTo(request.locationType());
        assertThat(entity.getLocationDetail()).isEqualTo(request.locationDetail());
        assertThat(entity.getStartDate()).isEqualTo(request.startDate());
        assertThat(entity.getEndDate()).isEqualTo(request.endDate());
        assertThat(entity.getMaxStudents()).isEqualTo(request.maxStudents());
    }

    @Test
    void toEntity_shouldNotMapId_orStatus_orCourseId() {
        CreateClassRequest request = ClassTestDataBuilder.createDefaultCreateRequest();

        Class entity = classMapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCourseId()).isNull();
        assertThat(entity.getStatus()).isNull(); // set by service
    }

    @Test
    void toResponse_shouldMapAllFields_fromEntity() {
        Class entity = ClassTestDataBuilder.createDefaultClass();

        ClassResponse response = classMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.courseId()).isEqualTo(entity.getCourseId());
        assertThat(response.name()).isEqualTo(entity.getName());
        assertThat(response.description()).isEqualTo(entity.getDescription());
        assertThat(response.schedule()).isEqualTo(entity.getSchedule());
        assertThat(response.locationType()).isEqualTo(entity.getLocationType());
        assertThat(response.locationDetail()).isEqualTo(entity.getLocationDetail());
        assertThat(response.startDate()).isEqualTo(entity.getStartDate());
        assertThat(response.endDate()).isEqualTo(entity.getEndDate());
        assertThat(response.maxStudents()).isEqualTo(entity.getMaxStudents());
        assertThat(response.currentEnrolled()).isEqualTo(entity.getCurrentEnrolled());
        assertThat(response.status()).isEqualTo(ClassStatus.SCHEDULED);
    }

    @Test
    void toSessionResponse_shouldMapAllFields_fromSession() {
        ClassSession session = ClassTestDataBuilder.createDefaultSession(
                1L, 1, java.time.LocalDate.of(2026, 3, 2));

        ClassSessionResponse response = classMapper.toSessionResponse(session);

        assertThat(response).isNotNull();
        assertThat(response.classId()).isEqualTo(session.getClassId());
        assertThat(response.sessionNumber()).isEqualTo(session.getSessionNumber());
        assertThat(response.sessionDate()).isEqualTo(session.getSessionDate());
        assertThat(response.startTime()).isEqualTo(session.getStartTime());
        assertThat(response.endTime()).isEqualTo(session.getEndTime());
        assertThat(response.status()).isEqualTo(session.getStatus());
        assertThat(response.attendanceTaken()).isFalse();
    }
}
