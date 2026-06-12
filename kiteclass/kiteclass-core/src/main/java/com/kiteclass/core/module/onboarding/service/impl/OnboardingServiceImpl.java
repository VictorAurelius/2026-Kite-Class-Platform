package com.kiteclass.core.module.onboarding.service.impl;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class.LocationType;
import com.kiteclass.core.module.clazz.service.ClassService;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.course.service.CourseService;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.service.EnrollmentService;
import com.kiteclass.core.module.onboarding.dto.SampleDataResponse;
import com.kiteclass.core.module.onboarding.service.OnboardingService;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.service.StudentService;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Default {@link OnboardingService} implementation.
 *
 * <p>Facade Pattern — orchestrates the existing teacher / course / class / student /
 * enrollment module services to seed a believable Vietnamese-edu demo set. It does NOT
 * touch repositories directly for entity creation (so business validation + tenant scoping
 * are honored); it only reads {@link CourseRepository} for the idempotency marker check.
 *
 * @author KiteClass Team
 * @since 3.17.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    /**
     * Deterministic course code used as the idempotency marker. If a non-deleted course with
     * this code already exists for the tenant, sample data is considered already imported.
     */
    static final String SAMPLE_COURSE_CODE = "MAU-DEMO";

    /** Default tuition for demo enrollments (1.500.000đ). */
    private static final BigDecimal DEMO_TUITION = new BigDecimal("1500000");

    private final TeacherService teacherService;
    private final CourseService courseService;
    private final ClassService classService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public SampleDataResponse importSampleData() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("Importing onboarding sample data for tenant {}", tenantId);

        // Idempotency: the marker course is created on first import; its presence means
        // sample data already exists, so we no-op instead of duplicating the demo set.
        if (courseRepository.existsByCodeAndInstanceIdAndDeletedFalse(SAMPLE_COURSE_CODE, tenantId)) {
            log.info("Sample data already present for tenant {} — skipping import", tenantId);
            return SampleDataResponse.noOp();
        }

        // 1 teacher — Trần Thị Hồng (Toán).
        TeacherResponse teacher = teacherService.createTeacher(new CreateTeacherRequest(
                "Trần Thị Hồng",
                sampleEmail(tenantId, "gv.hong"),
                "0901234501",
                "Toán",
                "Giáo viên mẫu được tạo tự động để bạn trải nghiệm hệ thống.",
                "Cử nhân Sư phạm Toán",
                5
        ));

        // 1 course — parent of the demo class. Code is the idempotency marker.
        CourseResponse course = courseService.createCourse(new CreateCourseRequest(
                "Khóa học mẫu - Toán 10",
                SAMPLE_COURSE_CODE,
                "Khóa học mẫu được tạo tự động để minh họa dữ liệu trên hệ thống.",
                null,
                null,
                null,
                null,
                teacher.id(),
                null,
                null,
                null,
                null,
                null
        ));

        // 1 class — Lớp Toán 10A1 (SCHEDULED on create, so enrollable).
        ClassResponse clazz = classService.createClass(course.id(), new CreateClassRequest(
                "Lớp Toán 10A1",
                "Lớp học mẫu để bạn thử các thao tác điểm danh, học phí, điểm số.",
                "Thứ 2-4-6, 18:00-19:30",
                LocationType.IN_PERSON,
                "Phòng 101",
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                30
        ));

        // 3 students — VN-friendly sample names.
        String[] names = {"Nguyễn Văn An", "Trần Thị Bình", "Lê Hoàng Cường"};
        String[] phones = {"0901234511", "0901234512", "0901234513"};
        String[] emailPrefixes = {"hs.an", "hs.binh", "hs.cuong"};
        for (int i = 0; i < names.length; i++) {
            StudentResponse student = studentService.createStudent(new CreateStudentRequest(
                    names[i],
                    sampleEmail(tenantId, emailPrefixes[i]),
                    phones[i],
                    LocalDate.of(2010, 1, 1).plusMonths(i),
                    null,
                    "Số 123 Lê Lợi, Q.1, TP.HCM",
                    "Học sinh mẫu được tạo tự động."
            ), tenantId);

            // Enroll each demo student into the demo class.
            enrollmentService.enrollStudent(CreateEnrollmentRequest.builder()
                    .studentId(student.id())
                    .classId(clazz.id())
                    .tuitionAmount(DEMO_TUITION)
                    .discountPercent(BigDecimal.ZERO)
                    .notes("Đăng ký mẫu được tạo tự động.")
                    .build());
        }

        log.info("Onboarding sample data imported for tenant {}: teacher={}, course={}, class={}, students=3",
                tenantId, teacher.id(), course.id(), clazz.id());
        return SampleDataResponse.freshImport();
    }

    /**
     * Builds a tenant-stable, demo-namespaced email so re-imports across tenants never collide
     * yet stay deterministic. Idempotency already prevents duplicate imports within a tenant.
     *
     * @param tenantId current tenant
     * @param prefix   local-part prefix (e.g. {@code gv.hong})
     * @return a sample email such as {@code gv.hong.ab12cd34@demo.kitehub.me}
     */
    private static String sampleEmail(UUID tenantId, String prefix) {
        String suffix = tenantId.toString().replace("-", "").substring(0, 8);
        return prefix + "." + suffix + "@demo.kitehub.me";
    }
}
