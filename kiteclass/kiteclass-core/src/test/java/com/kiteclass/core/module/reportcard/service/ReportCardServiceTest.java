package com.kiteclass.core.module.reportcard.service;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.repository.SemesterRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.entity.SubjectSection;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import com.kiteclass.core.module.reportcard.dto.ReportCardData;
import com.kiteclass.core.module.reportcard.service.impl.ReportCardServiceImpl;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReportCardServiceImpl} aggregation logic (Task 1+2 of GAP-055 Phase 1).
 *
 * <p>Covers BR-RC-AGG-001..008 from {@code rules.md}. Repositories mocked; tenant filter
 * verified at integration layer (Task 5 ControllerIT — UC-RC-05).
 */
@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    private static final long STUDENT_ID = 100L;
    private static final long SEMESTER_ID = 200L;
    private static final long HOMEROOM_CLASS_ID = 300L;
    private static final long COURSE_TOAN = 400L;
    private static final long COURSE_VAN = 401L;
    private static final long COURSE_ANH = 402L;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private SubjectGradeRepository subjectGradeRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ReportCardServiceImpl service;

    private Student student;
    private Semester semester;
    private HomeroomClass homeroom;

    @BeforeEach
    void setUp() {
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();

        student = Student.builder()
                .name("Nguyễn Phạm Hồng Ánh Tuấn")
                .dateOfBirth(LocalDate.of(2010, 3, 15))
                .status(StudentStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);

        semester = Semester.builder()
                .academicYear(year)
                .name("HK1 năm học 2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 1, 15))
                .build();
        ReflectionTestUtils.setField(semester, "id", SEMESTER_ID);

        homeroom = HomeroomClass.builder()
                .academicYear(year)
                .grade("10")
                .section("A1")
                .capacity(40)
                .currentEnrolled(35)
                .build();
        ReflectionTestUtils.setField(homeroom, "id", HOMEROOM_CLASS_ID);
    }

    @Test
    void generateReportCard_aggregates_grades_with_subject_names_and_class_label() {
        SubjectGrade toan = subjectGrade(COURSE_TOAN, "9.0", "8.5", "8.0", "8.33", "Giỏi");
        SubjectGrade van = subjectGrade(COURSE_VAN, "7.0", "6.5", "7.5", "7.08", "Khá");
        SubjectGrade anh = subjectGrade(COURSE_ANH, "5.5", "6.0", "5.0", "5.42", "Trung bình");

        when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID)).thenReturn(Optional.of(student));
        when(semesterRepository.findByIdWithAcademicYear(SEMESTER_ID)).thenReturn(Optional.of(semester));
        when(subjectGradeRepository.findByStudentIdAndSemesterIdAndDeletedFalse(STUDENT_ID, SEMESTER_ID))
                .thenReturn(List.of(toan, van, anh));
        when(courseRepository.findAllById(any())).thenReturn(List.of(
                course(COURSE_TOAN, "Toán"),
                course(COURSE_VAN, "Ngữ Văn"),
                course(COURSE_ANH, "Tiếng Anh")
        ));

        ReportCardData data = service.generateReportCard(STUDENT_ID, SEMESTER_ID);

        assertThat(data.studentId()).isEqualTo(STUDENT_ID);
        assertThat(data.studentName()).isEqualTo("Nguyễn Phạm Hồng Ánh Tuấn");
        assertThat(data.studentDateOfBirth()).isEqualTo(LocalDate.of(2010, 3, 15));
        assertThat(data.homeroomClassLabel()).isEqualTo("10A1");
        assertThat(data.semesterLabel()).isEqualTo("HK1 năm học 2026-2027");
        assertThat(data.subjects()).hasSize(3);
        assertThat(data.subjects())
                .extracting(ReportCardData.SubjectRow::subjectName)
                .containsExactlyInAnyOrder("Toán", "Ngữ Văn", "Tiếng Anh");
        assertThat(data.conduct()).isNull();
    }

    @Test
    void generateReportCard_overall_average_is_arithmetic_mean_of_subject_averages_phase1() {
        SubjectGrade toan = subjectGrade(COURSE_TOAN, null, null, null, "8.00", "Giỏi");
        SubjectGrade van = subjectGrade(COURSE_VAN, null, null, null, "6.00", "Trung bình");

        mockHappyPath(List.of(toan, van), List.of(course(COURSE_TOAN, "Toán"), course(COURSE_VAN, "Ngữ Văn")));

        ReportCardData data = service.generateReportCard(STUDENT_ID, SEMESTER_ID);

        // (8.00 + 6.00) / 2 = 7.00 → Khá
        assertThat(data.overallAverage()).isEqualByComparingTo("7.00");
        assertThat(data.overallLetterGrade()).isEqualTo("Khá");
    }

    @Test
    void generateReportCard_skips_null_averages_in_overall_mean() {
        SubjectGrade complete = subjectGrade(COURSE_TOAN, "9.0", "8.5", "8.0", "8.33", "Giỏi");
        SubjectGrade incomplete = subjectGrade(COURSE_VAN, "5.5", null, null, null, null);

        mockHappyPath(List.of(complete, incomplete), List.of(course(COURSE_TOAN, "Toán"), course(COURSE_VAN, "Ngữ Văn")));

        ReportCardData data = service.generateReportCard(STUDENT_ID, SEMESTER_ID);

        // Only the complete row counts toward overall: 8.33 / 1 = 8.33 → Giỏi
        assertThat(data.overallAverage()).isEqualByComparingTo("8.33");
        assertThat(data.overallLetterGrade()).isEqualTo("Giỏi");
        assertThat(data.subjects()).hasSize(2);
        assertThat(data.subjects())
                .filteredOn(r -> r.subjectName().equals("Ngữ Văn"))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.average()).isNull();
                    assertThat(r.letterGrade()).isNull();
                    assertThat(r.midtermScore()).isNull();
                    assertThat(r.finalScore()).isNull();
                });
    }

    @Test
    void generateReportCard_overall_average_null_when_no_subject_has_average() {
        SubjectGrade incomplete1 = subjectGrade(COURSE_TOAN, "5.5", null, null, null, null);
        SubjectGrade incomplete2 = subjectGrade(COURSE_VAN, null, null, null, null, null);

        mockHappyPath(List.of(incomplete1, incomplete2), List.of(course(COURSE_TOAN, "Toán"), course(COURSE_VAN, "Ngữ Văn")));

        ReportCardData data = service.generateReportCard(STUDENT_ID, SEMESTER_ID);

        assertThat(data.overallAverage()).isNull();
        assertThat(data.overallLetterGrade()).isNull();
    }

    @Test
    void generateReportCard_renders_unknown_subject_when_course_missing() {
        SubjectGrade orphan = subjectGrade(COURSE_TOAN, "9.0", "8.5", "8.0", "8.33", "Giỏi");

        when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID)).thenReturn(Optional.of(student));
        when(semesterRepository.findByIdWithAcademicYear(SEMESTER_ID)).thenReturn(Optional.of(semester));
        when(subjectGradeRepository.findByStudentIdAndSemesterIdAndDeletedFalse(STUDENT_ID, SEMESTER_ID))
                .thenReturn(List.of(orphan));
        when(courseRepository.findAllById(any())).thenReturn(List.of()); // course soft-deleted or missing

        ReportCardData data = service.generateReportCard(STUDENT_ID, SEMESTER_ID);

        assertThat(data.subjects()).singleElement()
                .satisfies(r -> assertThat(r.subjectName()).isEqualTo("(môn không xác định)"));
    }

    @Test
    void generateReportCard_throws_404_when_student_missing() {
        when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateReportCard(STUDENT_ID, SEMESTER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("REPORT_CARD_STUDENT_NOT_FOUND");
    }

    @Test
    void generateReportCard_throws_404_when_semester_missing() {
        when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID)).thenReturn(Optional.of(student));
        when(semesterRepository.findByIdWithAcademicYear(SEMESTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateReportCard(STUDENT_ID, SEMESTER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("REPORT_CARD_SEMESTER_NOT_FOUND");
    }

    @Test
    void generateReportCard_throws_404_when_no_grades_found() {
        when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID)).thenReturn(Optional.of(student));
        when(semesterRepository.findByIdWithAcademicYear(SEMESTER_ID)).thenReturn(Optional.of(semester));
        when(subjectGradeRepository.findByStudentIdAndSemesterIdAndDeletedFalse(STUDENT_ID, SEMESTER_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.generateReportCard(STUDENT_ID, SEMESTER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("REPORT_CARD_NO_GRADES");
    }

    // --- helpers ---

    private void mockHappyPath(List<SubjectGrade> grades, List<Course> courses) {
        lenient().when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID)).thenReturn(Optional.of(student));
        lenient().when(semesterRepository.findByIdWithAcademicYear(SEMESTER_ID)).thenReturn(Optional.of(semester));
        lenient().when(subjectGradeRepository.findByStudentIdAndSemesterIdAndDeletedFalse(STUDENT_ID, SEMESTER_ID))
                .thenReturn(grades);
        lenient().when(courseRepository.findAllById(any())).thenReturn(courses);
    }

    private SubjectGrade subjectGrade(long courseId, String regular, String midterm, String fin, String average, String letterGrade) {
        SubjectSection section = SubjectSection.builder()
                .homeroomClass(homeroom)
                .courseId(courseId)
                .build();
        return SubjectGrade.builder()
                .studentId(STUDENT_ID)
                .subjectSection(section)
                .semester(semester)
                .regularScore(regular == null ? null : new BigDecimal(regular))
                .midtermScore(midterm == null ? null : new BigDecimal(midterm))
                .finalScore(fin == null ? null : new BigDecimal(fin))
                .average(average == null ? null : new BigDecimal(average))
                .letterGrade(letterGrade)
                .build();
    }

    private Course course(long id, String name) {
        Course c = Course.builder().name(name).build();
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
