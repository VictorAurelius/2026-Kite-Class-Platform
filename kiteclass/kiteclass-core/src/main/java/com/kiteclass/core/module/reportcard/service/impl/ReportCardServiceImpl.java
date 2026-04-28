package com.kiteclass.core.module.reportcard.service.impl;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.repository.SemesterRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.entity.SubjectSection;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import com.kiteclass.core.module.reportcard.dto.ReportCardData;
import com.kiteclass.core.module.reportcard.service.ReportCardService;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @since 3.18.0 (GAP-055 Phase 1)
 */
@Service
@RequiredArgsConstructor
public class ReportCardServiceImpl implements ReportCardService {

    private static final String UNKNOWN_SUBJECT = "(môn không xác định)";

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportCardData generateReportCard(Long studentId, Long semesterId) {
        Student student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new EntityNotFoundException("REPORT_CARD_STUDENT_NOT_FOUND", (Object) studentId));

        Semester semester = semesterRepository.findByIdWithAcademicYear(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("REPORT_CARD_SEMESTER_NOT_FOUND", (Object) semesterId));

        List<SubjectGrade> grades = subjectGradeRepository
                .findByStudentIdAndSemesterIdAndDeletedFalse(studentId, semesterId);
        if (grades.isEmpty()) {
            throw new EntityNotFoundException("REPORT_CARD_NO_GRADES", studentId, semesterId);
        }

        Map<Long, String> courseNameById = resolveCourseNames(grades);
        List<ReportCardData.SubjectRow> rows = grades.stream()
                .map(g -> toSubjectRow(g, courseNameById))
                .toList();

        BigDecimal overall = computeOverallAverage(rows);
        String overallLetter = SubjectGrade.deriveLetterGrade(overall);

        String homeroomLabel = resolveHomeroomLabel(grades.get(0).getSubjectSection());

        return new ReportCardData(
                student.getId(),
                student.getName(),
                student.getDateOfBirth(),
                homeroomLabel,
                semester.getName(),
                rows,
                overall,
                overallLetter,
                null
        );
    }

    private Map<Long, String> resolveCourseNames(List<SubjectGrade> grades) {
        Set<Long> courseIds = grades.stream()
                .map(g -> g.getSubjectSection().getCourseId())
                .collect(Collectors.toSet());
        Iterable<Course> courses = courseRepository.findAllById(courseIds);
        Map<Long, String> map = new HashMap<>();
        StreamSupport.stream(courses.spliterator(), false)
                .filter(c -> !Boolean.TRUE.equals(c.isDeleted()))
                .forEach(c -> map.put(c.getId(), c.getName()));
        return map;
    }

    private ReportCardData.SubjectRow toSubjectRow(SubjectGrade g, Map<Long, String> courseNameById) {
        Long courseId = g.getSubjectSection().getCourseId();
        String name = courseNameById.getOrDefault(courseId, UNKNOWN_SUBJECT);
        return new ReportCardData.SubjectRow(
                name,
                g.getRegularScore(),
                g.getMidtermScore(),
                g.getFinalScore(),
                g.getAverage(),
                g.getLetterGrade()
        );
    }

    private BigDecimal computeOverallAverage(List<ReportCardData.SubjectRow> rows) {
        List<BigDecimal> averages = rows.stream()
                .map(ReportCardData.SubjectRow::average)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (averages.isEmpty()) {
            return null;
        }
        BigDecimal sum = averages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(averages.size()), 2, RoundingMode.HALF_UP);
    }

    private String resolveHomeroomLabel(SubjectSection section) {
        HomeroomClass homeroom = section.getHomeroomClass();
        return homeroom == null ? "" : homeroom.getFullName();
    }
}
