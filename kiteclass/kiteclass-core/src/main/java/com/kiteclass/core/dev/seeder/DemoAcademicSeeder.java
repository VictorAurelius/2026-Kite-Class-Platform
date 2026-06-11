package com.kiteclass.core.dev.seeder;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.GradeComponentType;
import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.service.AttendanceService;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class.LocationType;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.clazz.service.ClassService;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.course.service.CourseService;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.service.EnrollmentService;
import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.response.GradeResponse;
import com.kiteclass.core.module.grade.entity.Grade;
import com.kiteclass.core.module.grade.repository.GradeRepository;
import com.kiteclass.core.module.grade.service.GradeService;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.invoice.service.InvoiceService;
import com.kiteclass.core.module.payment.record.dto.RecordPaymentRequest;
import com.kiteclass.core.module.payment.record.entity.PaymentRecordMethod;
import com.kiteclass.core.module.payment.record.service.PaymentRecordService;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.service.StudentService;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import com.kiteclass.core.module.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Idempotently seeds a full academic dataset (teacher → course → class → session →
 * student → enrollment → attendance → grade → invoice → payment) for the two demo-trio
 * tenants ({@code co-ha-toan} + {@code thay-nhi-hoa}) when the {@code dev} profile is
 * active. Mirrors thesis §4.3/4.4 — cô Hà (gói Miễn phí, quy mô nhỏ, Toán tiểu học) và
 * thầy Nhì (gói Trả phí, quy mô lớn hơn, Hóa THCS, chuyên cần + báo cáo nâng cao).
 *
 * <p>Runs AFTER {@link BrandingDataSeeder} (which seeds the FrontendInstance + Branding +
 * LandingPage) via {@link Order} so the public homepage and the academic data share the
 * same tenant. The two seeders are otherwise independent — academic entities only need a
 * valid {@link TenantContext}, not a pre-existing instance row.
 *
 * <p><b>Idempotency:</b> gated per-tenant on a deterministic marker course code
 * (mirrors {@code OnboardingServiceImpl.SAMPLE_COURSE_CODE}). If the marker course already
 * exists for the tenant, the whole academic seed is skipped — safe to re-run every boot.
 *
 * <p>Tracking: GAP-1190..1193.
 */
@Component
@Profile("dev")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DemoAcademicSeeder {

    private final TeacherService teacherService;
    private final CourseService courseService;
    private final ClassService classService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;
    private final GradeService gradeService;
    private final InvoiceService invoiceService;
    private final PaymentRecordService paymentRecordService;

    private final CourseRepository courseRepository;
    private final ClassSessionRepository classSessionRepository;
    private final GradeRepository gradeRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionTemplate transactionTemplate;

    /** Per-seed running counter so emails + phones never collide across tenants/classes. */
    private int seq = 10_000_000;

    // ───────────────────────── Demo specs ─────────────────────────

    /** One class to seed: its own course + roster + lesson plan. */
    private record ClassSpec(String courseName, String courseCode, String category,
                             String className, String schedule, String room,
                             BigDecimal tuition, int sessionCount, int maxStudents,
                             List<String> studentNames) {
    }

    /** A whole tenant's academic dataset. */
    private record TenantSpec(UUID tenantId, String teacherName, String specialization,
                              String qualification, String bio, String markerCourseCode,
                              int baseBirthYear, int presentPct, int latePct, int excusedPct,
                              int paidPct, PaymentRecordMethod[] paymentMethods,
                              LocalTime startTime, LocalTime endTime, List<ClassSpec> classes) {
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        seedTenant(haSpec());
        seedTenant(nhiSpec());
    }

    // ───────────────────────── Tenant 1: cô Hà (FREE, Toán tiểu học) ─────────────────────────

    private TenantSpec haSpec() {
        return new TenantSpec(
                BrandingDataSeeder.HA_TENANT_ID,
                "Nguyễn Thị Hà",
                "Toán",
                "Cử nhân Sư phạm Toán — Đại học Sư phạm Hà Nội",
                "Cô Hà có hơn 6 năm kèm Toán tiểu học, lớp nhỏ, bám sát từng học viên.",
                "TOAN-HA-L4",          // marker course code
                2016,                   // tiểu học ~ 9-11 tuổi
                75, 10, 8,              // present / late / excused %  (absent = phần còn lại)
                100,                    // paidPct — gói miễn phí, phụ huynh đã đóng đủ học phí
                new PaymentRecordMethod[]{PaymentRecordMethod.BANK_TRANSFER},
                LocalTime.of(18, 0), LocalTime.of(19, 30),
                List.of(
                        new ClassSpec("Toán nâng cao lớp 4", "TOAN-HA-L4", "Toán tiểu học",
                                "Lớp Toán 4 - Cô Hà", "Thứ 3-5, 18:00-19:30", "Phòng A1",
                                new BigDecimal("800000"), 4, 10,
                                List.of("Trần Minh Khôi", "Nguyễn Bảo An", "Lê Thảo Vy",
                                        "Phạm Gia Hân", "Hoàng Nhật Minh", "Đặng Khánh Linh")),
                        new ClassSpec("Toán nâng cao lớp 5", "TOAN-HA-L5", "Toán tiểu học",
                                "Lớp Toán 5 - Cô Hà", "Thứ 4-6, 18:00-19:30", "Phòng A2",
                                new BigDecimal("900000"), 4, 10,
                                List.of("Vũ Đức Anh", "Bùi Thanh Mai", "Ngô Quốc Bảo",
                                        "Dương Tuệ Nhi", "Đỗ Hải Đăng", "Trịnh Yến Nhi"))));
    }

    // ───────────────────────── Tenant 2: thầy Nhì (PAID, Hóa THCS) ─────────────────────────

    private TenantSpec nhiSpec() {
        return new TenantSpec(
                BrandingDataSeeder.NHI_TENANT_ID,
                "Nguyễn Đình Nhì",
                "Hóa học",
                "Thạc sĩ Hóa học — Đại học Khoa học Tự nhiên",
                "Thầy Nhì luyện Hóa THCS theo lộ trình bài bản, chuyên cần cao, báo cáo chi tiết.",
                "HOA-NHI-8A",          // marker course code
                2011,                   // THCS ~ 12-15 tuổi
                88, 6, 4,               // chuyên cần cao hơn cô Hà
                80,                     // paidPct — lớp lớn, vài hóa đơn còn tồn để demo công nợ
                new PaymentRecordMethod[]{PaymentRecordMethod.CASH, PaymentRecordMethod.BANK_TRANSFER,
                        PaymentRecordMethod.VIETQR, PaymentRecordMethod.MOMO},
                LocalTime.of(19, 30), LocalTime.of(21, 0),
                List.of(
                        new ClassSpec("Hóa học lớp 8", "HOA-NHI-8A", "Hóa học THCS",
                                "Lớp Hóa 8A - Thầy Nhì", "Thứ 2-4-6, 19:30-21:00", "Phòng B1",
                                new BigDecimal("1200000"), 6, 15,
                                List.of("Lý Gia Bảo", "Trần Phương Thảo", "Nguyễn Minh Quân",
                                        "Phạm Thu Hà", "Hoàng Anh Tuấn", "Lê Diệu Linh",
                                        "Vũ Đăng Khoa", "Đặng Phương Anh", "Bùi Tiến Dũng")),
                        new ClassSpec("Hóa học lớp 8", "HOA-NHI-8B", "Hóa học THCS",
                                "Lớp Hóa 8B - Thầy Nhì", "Thứ 3-5-7, 19:30-21:00", "Phòng B2",
                                new BigDecimal("1200000"), 6, 15,
                                List.of("Ngô Bảo Châu", "Dương Khánh Hòa", "Trịnh Nhật Nam",
                                        "Đỗ Mai Phương", "Phan Quang Huy", "Cao Thùy Dung",
                                        "Đinh Gia Bảo", "Tạ Khánh Vy", "Hồ Minh Đức")),
                        new ClassSpec("Hóa học lớp 9", "HOA-NHI-9A", "Hóa học THCS",
                                "Lớp Hóa 9A - Thầy Nhì", "Thứ 2-4-6, 19:30-21:00", "Phòng B3",
                                new BigDecimal("1500000"), 6, 15,
                                List.of("Nguyễn Hữu Phước", "Lê Quỳnh Như", "Trần Đình Long",
                                        "Phạm Bảo Ngọc", "Vũ Hoàng Sơn", "Bùi Khánh Chi",
                                        "Đặng Quốc Việt", "Hoàng Thảo Nguyên", "Mai Tuấn Kiệt")),
                        new ClassSpec("Hóa học lớp 9", "HOA-NHI-9B", "Hóa học THCS",
                                "Lớp Hóa 9B - Thầy Nhì", "Thứ 3-5-7, 19:30-21:00", "Phòng B4",
                                new BigDecimal("1800000"), 6, 15,
                                List.of("Trương Gia Huy", "Lương Bích Ngọc", "Tô Đức Thành",
                                        "Hà Phương Linh", "Kiều Anh Khoa", "Đoàn Mỹ Duyên",
                                        "Phùng Nhật Hào", "Quách Yến Vy"))));
    }

    // ───────────────────────── Core seed routine ─────────────────────────

    private void seedTenant(TenantSpec spec) {
        try {
            TenantContext.setCurrentTenant(spec.tenantId());

            if (courseRepository.existsByCodeAndInstanceIdAndDeletedFalse(
                    spec.markerCourseCode(), spec.tenantId())) {
                log.info("Academic demo data already present for tenant {} (marker {}). Skipping.",
                        spec.tenantId(), spec.markerCourseCode());
                return;
            }

            String tenantSuffix = spec.tenantId().toString().replace("-", "").substring(0, 8);

            // 1 teacher per tenant.
            TeacherResponse teacher = teacherService.createTeacher(new CreateTeacherRequest(
                    spec.teacherName(),
                    "gv." + tenantSuffix + "@demo.kiteclass.vn",
                    nextPhone(),
                    spec.specialization(),
                    spec.bio(),
                    spec.qualification(),
                    6));

            int classesSeeded = 0;
            int studentsSeeded = 0;
            for (ClassSpec cs : spec.classes()) {
                int n = seedClass(spec, teacher, cs, tenantSuffix);
                classesSeeded++;
                studentsSeeded += n;
            }

            log.info("Seeded academic demo for tenant {} ({}): teacher={}, classes={}, students={}",
                    spec.tenantId(), spec.teacherName(), teacher.id(), classesSeeded, studentsSeeded);
        } catch (Exception e) {
            // Never let a seed failure crash dev startup — log and continue.
            log.error("Academic demo seed failed for tenant {}: {}", spec.tenantId(), e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    /** Seeds one class end-to-end. Returns the number of students enrolled. */
    private int seedClass(TenantSpec spec, TeacherResponse teacher, ClassSpec cs, String tenantSuffix) {
        LocalDate today = LocalDate.now();

        // Course (code is the per-tenant marker for the first class).
        CourseResponse course = courseService.createCourse(new CreateCourseRequest(
                cs.courseName(),
                cs.courseCode(),
                "Khóa " + cs.courseName() + " — dữ liệu demo cho luận văn (thesis §4.3/4.4).",
                null, null, null, null,
                teacher.id(),
                null,
                cs.sessionCount(),
                cs.tuition(),
                null,
                cs.category()));

        // Class — SCHEDULED on create (enrollable).
        ClassResponse clazz = classService.createClass(course.id(), new CreateClassRequest(
                cs.className(),
                "Lớp demo để minh họa điểm danh, học phí, điểm số.",
                cs.schedule(),
                LocationType.IN_PERSON,
                cs.room(),
                today.minusMonths(2),
                today.plusMonths(2),
                cs.maxStudents()));

        // Assign the teacher as MAIN_TEACHER of the class (BR-TEACHER-008).
        teacherClassRepository.save(TeacherClass.builder()
                .teacherId(teacher.id())
                .classId(clazz.id())
                .role(TeacherClassRole.MAIN_TEACHER)
                .assignedBy(teacher.id())
                .build());

        // Lesson sessions (buổi học) — recent past, ~every 5 days, SCHEDULED for now.
        List<Long> sessionIds = createSessions(clazz.id(), cs, spec, today);

        // Students → enroll → activate → grade → invoice/payment.
        List<Long> enrollmentIds = new ArrayList<>();
        int idx = 0;
        for (String name : cs.studentNames()) {
            StudentResponse student = studentService.createStudent(new CreateStudentRequest(
                    name,
                    "hs." + tenantSuffix + "." + (seq) + "@demo.kiteclass.vn",
                    nextPhone(),
                    birthDate(spec.baseBirthYear(), idx),
                    (idx % 2 == 0) ? Gender.MALE : Gender.FEMALE,
                    "Số " + (10 + idx) + " đường Demo, Quận 1, TP.HCM",
                    "Học viên demo."),
                    spec.tenantId());

            EnrollmentResponse enrollment = enrollmentService.enrollStudent(CreateEnrollmentRequest.builder()
                    .studentId(student.id())
                    .classId(clazz.id())
                    .tuitionAmount(cs.tuition())
                    .discountPercent(BigDecimal.ZERO)
                    .notes("Đăng ký demo.")
                    .build());

            // Activate so attendance can be marked (BR-ATTEND-001 requires ACTIVE).
            enrollmentService.updateEnrollmentStatus(enrollment.getId(),
                    UpdateEnrollmentStatusRequest.builder().status(EnrollmentStatus.ACTIVE).build());
            enrollmentIds.add(enrollment.getId());

            // Grades: midterm 40% + final 60% → calculate → finalize.
            seedGrade(student.id(), clazz.id(), teacher.id(), idx);

            // Invoice (auto-created by ENROLLMENT_CREATED listener) + payment record.
            boolean paid = (Math.floorMod(idx * 37 + 13, 100) < spec.paidPct());
            PaymentRecordMethod method = spec.paymentMethods()[idx % spec.paymentMethods().length];
            seedInvoiceAndPayment(enrollment.getId(), teacher.id(), method, paid);

            idx++;
        }

        // Attendance per session for every enrollment, then close the session.
        int sessionSeq = 0;
        for (Long sessionId : sessionIds) {
            for (int i = 0; i < enrollmentIds.size(); i++) {
                AttendanceStatus status = pickStatus(spec, i, sessionSeq);
                markAttendanceSafe(enrollmentIds.get(i), sessionId, status, teacher.id());
            }
            completeSession(sessionId);
            sessionSeq++;
        }

        return cs.studentNames().size();
    }

    // ───────────────────────── Sub-steps ─────────────────────────

    private List<Long> createSessions(Long classId, ClassSpec cs, TenantSpec spec, LocalDate today) {
        List<Long> ids = new ArrayList<>();
        String[] topics = sessionTopics(cs.category());
        for (int k = 1; k <= cs.sessionCount(); k++) {
            ClassSession session = ClassSession.builder()
                    .classId(classId)
                    .sessionNumber(k)
                    .sessionDate(today.minusDays((long) (cs.sessionCount() - k) * 5))
                    .startTime(spec.startTime())
                    .endTime(spec.endTime())
                    .location(cs.room())
                    .topic(topics[(k - 1) % topics.length])
                    .status(SessionStatus.SCHEDULED)
                    .attendanceTaken(false)
                    .build();
            ids.add(classSessionRepository.save(session).getId());
        }
        return ids;
    }

    private void seedGrade(Long studentId, Long classId, Long teacherId, int idx) {
        try {
            GradeResponse grade = gradeService.initializeGrade(studentId, classId); // idempotent
            Long gradeId = grade.getId();

            gradeService.addOrUpdateComponent(CreateGradeComponentRequest.builder()
                    .gradeId(gradeId)
                    .componentType(GradeComponentType.MIDTERM)
                    .componentName("Kiểm tra giữa kỳ")
                    .score(score(idx, 0))
                    .maxScore(BigDecimal.TEN)
                    .weightPercent(new BigDecimal("40"))
                    .build());

            gradeService.addOrUpdateComponent(CreateGradeComponentRequest.builder()
                    .gradeId(gradeId)
                    .componentType(GradeComponentType.FINAL)
                    .componentName("Kiểm tra cuối kỳ")
                    .score(score(idx, 1))
                    .maxScore(BigDecimal.TEN)
                    .weightPercent(new BigDecimal("60"))
                    .build());

            gradeService.calculateFinalScore(gradeId);
            finalizeGradeDirect(gradeId, teacherId);
        } catch (Exception e) {
            log.warn("Seed grade failed for student {} class {}: {}", studentId, classId, e.getMessage());
        }
    }

    /**
     * Finalize the grade directly on the entity. {@code GradeService.finalizeGrade} derives the
     * acting teacher from the authenticated principal (UserContext / SecurityContext) which is
     * absent in a boot-time seeder; the entity {@code finalize()} only needs valid weights, so we
     * load with components (avoids lazy-init) inside a transaction and persist.
     */
    private void finalizeGradeDirect(Long gradeId, Long teacherId) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Grade grade = gradeRepository.findByIdWithComponents(gradeId).orElse(null);
                if (grade != null && !grade.isFinalized() && grade.isWeightsSumValid()) {
                    grade.finalize(teacherId);
                    gradeRepository.save(grade);
                }
            });
        } catch (Exception e) {
            log.warn("Finalize grade {} failed: {}", gradeId, e.getMessage());
        }
    }

    private void seedInvoiceAndPayment(Long enrollmentId, Long teacherId,
                                       PaymentRecordMethod method, boolean paid) {
        try {
            // The ENROLLMENT_CREATED AFTER_COMMIT listener already created the invoice; fall back
            // to explicit creation if it failed (listener swallows exceptions).
            Invoice invoice = invoiceRepository.findByEnrollmentIdAndDeletedFalse(enrollmentId)
                    .orElseGet(() -> invoiceService.createInvoiceForEnrollment(enrollmentId));

            if (!paid) {
                return; // leave the invoice outstanding (demo công nợ)
            }

            BigDecimal balanceDue = invoice.getBalanceDue();
            if (balanceDue.compareTo(BigDecimal.ZERO) > 0) {
                paymentRecordService.recordPayment(invoice.getId(),
                        RecordPaymentRequest.builder()
                                .method(method)
                                .amount(balanceDue)
                                .paidAt(Instant.now())
                                .note("Phụ huynh thanh toán học phí (" + method + ")")
                                .build(),
                        teacherId, null);
            }
            invoiceService.markInvoiceAsPaid(invoice.getId());
        } catch (Exception e) {
            log.warn("Seed invoice/payment failed for enrollment {}: {}", enrollmentId, e.getMessage());
        }
    }

    private void markAttendanceSafe(Long enrollmentId, Long sessionId,
                                    AttendanceStatus status, Long teacherId) {
        try {
            attendanceService.markAttendance(CreateAttendanceRequest.builder()
                    .enrollmentId(enrollmentId)
                    .sessionId(sessionId)
                    .status(status)
                    // EXCUSED requires a note (BR-ATT-005).
                    .notes(status == AttendanceStatus.EXCUSED ? "Phụ huynh xin phép nghỉ" : null)
                    .markedBy(teacherId)
                    .build());
        } catch (Exception e) {
            log.warn("Mark attendance failed (enrollment={}, session={}): {}",
                    enrollmentId, sessionId, e.getMessage());
        }
    }

    private void completeSession(Long sessionId) {
        try {
            classSessionRepository.findByIdAndDeletedFalse(sessionId).ifPresent(session -> {
                session.setStatus(SessionStatus.COMPLETED);
                session.setAttendanceTaken(true);
                classSessionRepository.save(session);
            });
        } catch (Exception e) {
            log.warn("Complete session {} failed: {}", sessionId, e.getMessage());
        }
    }

    // ───────────────────────── Helpers ─────────────────────────

    /** Deterministic, unique 10-digit VN phone (^0\d{9}$). */
    private String nextPhone() {
        return String.format("09%08d", seq++ % 100_000_000);
    }

    private LocalDate birthDate(int baseYear, int idx) {
        int year = baseYear - (idx % 3);
        int month = (idx % 12) + 1;
        int day = (idx % 27) + 1;
        return LocalDate.of(year, month, day);
    }

    /** Component score on a 0-10 scale; spread so finals land roughly 60-95. */
    private BigDecimal score(int idx, int component) {
        double base = 6.5 + (Math.floorMod(idx * 7 + component * 13, 30)) / 10.0; // 6.5 .. 9.4
        return BigDecimal.valueOf(base).setScale(1, RoundingMode.HALF_UP);
    }

    private AttendanceStatus pickStatus(TenantSpec spec, int studentIdx, int sessionSeq) {
        int r = Math.floorMod(studentIdx * 31 + sessionSeq * 17, 100);
        if (r < spec.presentPct()) {
            return AttendanceStatus.PRESENT;
        }
        if (r < spec.presentPct() + spec.latePct()) {
            return AttendanceStatus.LATE;
        }
        if (r < spec.presentPct() + spec.latePct() + spec.excusedPct()) {
            return AttendanceStatus.EXCUSED;
        }
        return AttendanceStatus.ABSENT;
    }

    private String[] sessionTopics(String category) {
        if (category != null && category.startsWith("Hóa")) {
            return new String[]{
                    "Chất - Nguyên tử - Phân tử",
                    "Phản ứng hóa học",
                    "Mol và tính toán hóa học",
                    "Oxi - Không khí",
                    "Hiđro - Nước",
                    "Dung dịch - Nồng độ"};
        }
        return new String[]{
                "Ôn tập số tự nhiên",
                "Phân số và phép tính",
                "Hình học cơ bản",
                "Giải toán có lời văn"};
    }
}
