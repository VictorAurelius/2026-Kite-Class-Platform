package com.kiteclass.core.module.k12.service;

import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import com.kiteclass.core.module.academicyear.repository.SemesterRepository;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeType;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GradeFormulaServiceImpl} — TT 22/2021 grading formulas.
 *
 * <p>Edge cases covered (BR-GRADEBOOK-001..005):
 * <ol>
 *   <li>Full case — TX + GK + CK all present, weighted average correct</li>
 *   <li>Multiple TX scores — arithmetic mean across TX records</li>
 *   <li>Missing GK — null result (BR-GRADEBOOK-001 partial state)</li>
 *   <li>Missing CK — null result</li>
 *   <li>No TX records — null result</li>
 *   <li>Decimal precision boundary — HALF_EVEN rounding scale=1 (BR-GRADEBOOK-005)</li>
 *   <li>ĐTBmCN annual — both semesters present</li>
 *   <li>ĐTBmCN missing HK1 → null</li>
 *   <li>Null inputs (defensive) — null result</li>
 *   <li>Zero scores (real "scored zero" not "unscored")</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class GradeFormulaServiceImplTest {

    @Mock
    private SubjectGradeRepository subjectGradeRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @InjectMocks
    private GradeFormulaServiceImpl service;

    private static final Long STUDENT_ID = 100L;
    private static final Long SUBJECT_SECTION_ID = 200L;
    private static final Long SEMESTER_ID = 300L;
    private static final Long ACADEMIC_YEAR_ID = 400L;
    private static final Long HK1_ID = 301L;
    private static final Long HK2_ID = 302L;

    @BeforeEach
    void resetMocks() {
        // No global stubbing — each test stubs explicitly to make missing-data
        // edge cases visible.
    }

    // ------------------------- ĐTBmHK -------------------------

    @Test
    void computeDTBmHK_fullCase_returnsWeightedAverage() {
        // TX = 8.0, GK = 7.5, CK = 9.0 → (8 + 7.5*2 + 9*3) / 6 = 50/6 ≈ 8.3
        stubType(SubjectGradeType.TX, txGrade("8.0"));
        stubType(SubjectGradeType.GK, gkGrade("7.5"));
        stubType(SubjectGradeType.CK, ckGrade("9.0"));

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        // 50/6 = 8.333... → HALF_EVEN scale=1 → 8.3
        assertThat(result).isEqualByComparingTo("8.3");
    }

    @Test
    void computeDTBmHK_multipleTX_averagesArithmetically() {
        // TX scores 7.0 + 9.0 → mean 8.0; GK 7.5; CK 9.0 → same as above
        stubType(SubjectGradeType.TX, txGrade("7.0"), txGrade("9.0"));
        stubType(SubjectGradeType.GK, gkGrade("7.5"));
        stubType(SubjectGradeType.CK, ckGrade("9.0"));

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        assertThat(result).isEqualByComparingTo("8.3");
    }

    @Test
    void computeDTBmHK_missingGK_returnsNull() {
        stubType(SubjectGradeType.TX, txGrade("8.0"));
        stubType(SubjectGradeType.GK); // empty
        stubType(SubjectGradeType.CK, ckGrade("9.0"));

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        assertThat(result).isNull();
    }

    @Test
    void computeDTBmHK_missingCK_returnsNull() {
        stubType(SubjectGradeType.TX, txGrade("8.0"));
        stubType(SubjectGradeType.GK, gkGrade("7.5"));
        stubType(SubjectGradeType.CK); // empty

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        assertThat(result).isNull();
    }

    @Test
    void computeDTBmHK_noTXRecords_returnsNull() {
        stubType(SubjectGradeType.TX); // empty
        // No need to stub GK/CK — short-circuit on TX-null.

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        assertThat(result).isNull();
    }

    @Test
    void computeDTBmHK_decimalPrecisionBoundary_HALF_EVEN_roundsToEven() {
        // HALF_EVEN: 5.85 → 5.8 (8 is even); 5.75 → 5.8 (8 is even).
        // Construct (TX + GK*2 + CK*3) / 6 = 5.85.
        // 5.85 * 6 = 35.10. Choose TX=4.5, GK=5.5, CK=6.0 → 4.5 + 11 + 18 = 33.5 → 33.5/6 = 5.583... → 5.6
        // Use TX=5.85 explicit single record + GK=5.85 + CK=5.85 → all 5.85 → result 5.85 → HALF_EVEN scale=1 → 5.8
        stubType(SubjectGradeType.TX, txGrade("5.85"));
        stubType(SubjectGradeType.GK, gkGrade("5.85"));
        stubType(SubjectGradeType.CK, ckGrade("5.85"));

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        // (5.85 + 11.70 + 17.55) / 6 = 35.10 / 6 = 5.85 → HALF_EVEN scale=1 → 5.8 (8 is even)
        assertThat(result).isEqualByComparingTo("5.8");
    }

    @Test
    void computeDTBmHK_zeroScores_returnsZero_notNull() {
        // "Scored zero" is real and distinct from "unscored".
        stubType(SubjectGradeType.TX, txGrade("0.0"));
        stubType(SubjectGradeType.GK, gkGrade("0.0"));
        stubType(SubjectGradeType.CK, ckGrade("0.0"));

        BigDecimal result = service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID);

        assertThat(result).isEqualByComparingTo("0.0");
    }

    @Test
    void computeDTBmHK_nullInputs_returnsNull() {
        assertThat(service.computeDTBmHK(null, SUBJECT_SECTION_ID, SEMESTER_ID)).isNull();
        assertThat(service.computeDTBmHK(STUDENT_ID, null, SEMESTER_ID)).isNull();
        assertThat(service.computeDTBmHK(STUDENT_ID, SUBJECT_SECTION_ID, null)).isNull();
    }

    // ------------------------- ĐTBmCN -------------------------

    @Test
    void computeDTBmCN_bothSemestersPresent_returnsAnnualAverage() {
        // HK1 → 7.0, HK2 → 8.0 → (7 + 2*8) / 3 = 23/3 = 7.66... → HALF_EVEN scale=1 → 7.7
        stubAcademicYearSemesters();

        // Stub HK1 calc: TX=7.0, GK=7.0, CK=7.0 → (7 + 14 + 21)/6 = 42/6 = 7.0
        stubGradesForSemester(HK1_ID, "7.0", "7.0", "7.0");
        // Stub HK2 calc: TX=8.0, GK=8.0, CK=8.0 → 8.0
        stubGradesForSemester(HK2_ID, "8.0", "8.0", "8.0");

        BigDecimal result = service.computeDTBmCN(STUDENT_ID, SUBJECT_SECTION_ID, ACADEMIC_YEAR_ID);

        // (7 + 16) / 3 = 23/3 = 7.6666 → HALF_EVEN scale=1 → 7.7
        assertThat(result).isEqualByComparingTo("7.7");
    }

    @Test
    void computeDTBmCN_missingHK1Semester_returnsNull() {
        when(semesterRepository.findByAcademicYearIdAndTypeAndDeletedFalse(ACADEMIC_YEAR_ID, SemesterType.HK1))
                .thenReturn(Optional.empty());
        lenient().when(semesterRepository.findByAcademicYearIdAndTypeAndDeletedFalse(ACADEMIC_YEAR_ID, SemesterType.HK2))
                .thenReturn(Optional.of(semester(HK2_ID, SemesterType.HK2)));

        BigDecimal result = service.computeDTBmCN(STUDENT_ID, SUBJECT_SECTION_ID, ACADEMIC_YEAR_ID);

        assertThat(result).isNull();
    }

    @Test
    void computeDTBmCN_HK2DataMissing_returnsNull() {
        stubAcademicYearSemesters();
        // HK1 has full data
        stubGradesForSemester(HK1_ID, "7.0", "7.0", "7.0");
        // HK2 missing CK → ĐTBmHK(HK2) null → ĐTBmCN null
        stubMissingCKForSemester(HK2_ID, "8.0", "8.0");

        BigDecimal result = service.computeDTBmCN(STUDENT_ID, SUBJECT_SECTION_ID, ACADEMIC_YEAR_ID);

        assertThat(result).isNull();
    }

    @Test
    void computeDTBmCN_nullInputs_returnsNull() {
        assertThat(service.computeDTBmCN(null, SUBJECT_SECTION_ID, ACADEMIC_YEAR_ID)).isNull();
        assertThat(service.computeDTBmCN(STUDENT_ID, null, ACADEMIC_YEAR_ID)).isNull();
        assertThat(service.computeDTBmCN(STUDENT_ID, SUBJECT_SECTION_ID, null)).isNull();
    }

    // ------------------------- helpers -------------------------

    private void stubType(SubjectGradeType type, SubjectGrade... grades) {
        when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        STUDENT_ID, SUBJECT_SECTION_ID, SEMESTER_ID, type))
                .thenReturn(grades.length == 0 ? Collections.emptyList() : List.of(grades));
    }

    private void stubAcademicYearSemesters() {
        when(semesterRepository.findByAcademicYearIdAndTypeAndDeletedFalse(ACADEMIC_YEAR_ID, SemesterType.HK1))
                .thenReturn(Optional.of(semester(HK1_ID, SemesterType.HK1)));
        when(semesterRepository.findByAcademicYearIdAndTypeAndDeletedFalse(ACADEMIC_YEAR_ID, SemesterType.HK2))
                .thenReturn(Optional.of(semester(HK2_ID, SemesterType.HK2)));
    }

    private void stubGradesForSemester(Long semId, String tx, String gk, String ck) {
        lenient().when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        eq(STUDENT_ID), eq(SUBJECT_SECTION_ID), eq(semId), eq(SubjectGradeType.TX)))
                .thenReturn(List.of(txGrade(tx)));
        lenient().when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        eq(STUDENT_ID), eq(SUBJECT_SECTION_ID), eq(semId), eq(SubjectGradeType.GK)))
                .thenReturn(List.of(gkGrade(gk)));
        lenient().when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        eq(STUDENT_ID), eq(SUBJECT_SECTION_ID), eq(semId), eq(SubjectGradeType.CK)))
                .thenReturn(List.of(ckGrade(ck)));
    }

    private void stubMissingCKForSemester(Long semId, String tx, String gk) {
        lenient().when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        eq(STUDENT_ID), eq(SUBJECT_SECTION_ID), eq(semId), eq(SubjectGradeType.TX)))
                .thenReturn(List.of(txGrade(tx)));
        lenient().when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        eq(STUDENT_ID), eq(SUBJECT_SECTION_ID), eq(semId), eq(SubjectGradeType.GK)))
                .thenReturn(List.of(gkGrade(gk)));
        lenient().when(subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        eq(STUDENT_ID), eq(SUBJECT_SECTION_ID), eq(semId), eq(SubjectGradeType.CK)))
                .thenReturn(Collections.emptyList());
    }

    private static SubjectGrade txGrade(String score) {
        return SubjectGrade.builder()
                .type(SubjectGradeType.TX)
                .regularScore(new BigDecimal(score))
                .build();
    }

    private static SubjectGrade gkGrade(String score) {
        return SubjectGrade.builder()
                .type(SubjectGradeType.GK)
                .midtermScore(new BigDecimal(score))
                .build();
    }

    private static SubjectGrade ckGrade(String score) {
        return SubjectGrade.builder()
                .type(SubjectGradeType.CK)
                .finalScore(new BigDecimal(score))
                .build();
    }

    /**
     * Build a Semester with a forced id (BaseEntity id is normally
     * auto-generated; tests need to inject for mock matching).
     */
    private static Semester semester(Long id, SemesterType type) {
        Semester s = Semester.builder().type(type).build();
        try {
            // Walk superclass chain for the id field (BaseEntity).
            Class<?> clazz = s.getClass();
            Field idField = null;
            while (clazz != null && idField == null) {
                try {
                    idField = clazz.getDeclaredField("id");
                } catch (NoSuchFieldException nsfe) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (idField == null) {
                throw new IllegalStateException("BaseEntity.id field not found");
            }
            idField.setAccessible(true);
            idField.set(s, id);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set Semester.id via reflection", e);
        }
        return s;
    }
}
