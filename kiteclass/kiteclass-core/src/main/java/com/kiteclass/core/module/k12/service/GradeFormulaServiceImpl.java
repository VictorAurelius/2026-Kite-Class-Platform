package com.kiteclass.core.module.k12.service;

import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import com.kiteclass.core.module.academicyear.repository.SemesterRepository;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeType;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Default {@link GradeFormulaService} implementation — TT 22/2021/TT-BGDĐT.
 *
 * <p>Strategy Pattern (per {@code design-patterns.md} §1.3) — single default
 * implementation today. Future TT amendments can add a sibling
 * {@code GradeFormulaServiceTT26Impl} and a {@code @Profile} / @ConditionalOnProperty
 * selector; callers stay coupled only to {@link GradeFormulaService}.
 *
 * <p>Formula constants (BR-GRADEBOOK-001, BR-GRADEBOOK-004):
 * <ul>
 *   <li>TX weight = 1 (averaged first via arithmetic mean of all TX records)</li>
 *   <li>GK weight = 2</li>
 *   <li>CK weight = 3</li>
 *   <li>Divisor = 1 + 2 + 3 = 6 (semester); 1 + 2 = 3 (annual: HK1 + 2*HK2)</li>
 * </ul>
 *
 * <p>Decimal precision: every intermediate AND final result uses
 * {@link RoundingMode#HALF_EVEN} with {@code scale=1} per
 * BR-GRADEBOOK-005 (MOET reporting convention).
 *
 * @since 5.x (Wave 19 Bucket B — GAP-323c Phase 1C v1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeFormulaServiceImpl implements GradeFormulaService {

    /** MOET reporting precision: 1 decimal place, banker's rounding. */
    private static final int SCALE = 1;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private static final BigDecimal WEIGHT_GK = BigDecimal.valueOf(2);
    private static final BigDecimal WEIGHT_CK = BigDecimal.valueOf(3);
    private static final BigDecimal DIVISOR_HK = BigDecimal.valueOf(6);
    private static final BigDecimal WEIGHT_HK2 = BigDecimal.valueOf(2);
    private static final BigDecimal DIVISOR_CN = BigDecimal.valueOf(3);

    private final SubjectGradeRepository subjectGradeRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal computeDTBmHK(Long studentId, Long subjectSectionId, Long semesterId) {
        if (studentId == null || subjectSectionId == null || semesterId == null) {
            log.debug("computeDTBmHK called with null id — returning null");
            return null;
        }

        BigDecimal txMean = arithmeticMeanOfType(
                studentId, subjectSectionId, semesterId, SubjectGradeType.TX);
        BigDecimal gk = singleScoreOfType(
                studentId, subjectSectionId, semesterId, SubjectGradeType.GK);
        BigDecimal ck = singleScoreOfType(
                studentId, subjectSectionId, semesterId, SubjectGradeType.CK);

        // BR-GRADEBOOK-001: ĐTBmHK requires TX mean + at least one GK + at least one CK.
        // Missing any component → null (not yet computable; UI shows "—" not "0").
        if (txMean == null || gk == null || ck == null) {
            return null;
        }

        BigDecimal weighted = txMean
                .add(gk.multiply(WEIGHT_GK))
                .add(ck.multiply(WEIGHT_CK));

        return weighted.divide(DIVISOR_HK, SCALE, ROUNDING);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal computeDTBmCN(Long studentId, Long subjectSectionId, Long academicYearId) {
        if (studentId == null || subjectSectionId == null || academicYearId == null) {
            log.debug("computeDTBmCN called with null id — returning null");
            return null;
        }

        Long hk1Id = findSemesterId(academicYearId, SemesterType.HK1);
        Long hk2Id = findSemesterId(academicYearId, SemesterType.HK2);
        if (hk1Id == null || hk2Id == null) {
            return null;
        }

        BigDecimal hk1 = computeDTBmHK(studentId, subjectSectionId, hk1Id);
        BigDecimal hk2 = computeDTBmHK(studentId, subjectSectionId, hk2Id);
        if (hk1 == null || hk2 == null) {
            return null;
        }

        BigDecimal weighted = hk1.add(hk2.multiply(WEIGHT_HK2));
        return weighted.divide(DIVISOR_CN, SCALE, ROUNDING);
    }

    /**
     * Arithmetic mean of all TX scores for one student/section/semester.
     *
     * <p>Returns {@code null} when:
     * <ul>
     *   <li>No TX records exist (never assessed) — caller treats as "not yet computable"</li>
     *   <li>All TX records have null score (data integrity issue)</li>
     * </ul>
     * Returns {@code BigDecimal.ZERO} only when there ARE records but their
     * mean rounds to zero — preserves "scored zero" vs "unscored" distinction.
     */
    private BigDecimal arithmeticMeanOfType(Long studentId, Long subjectSectionId,
                                            Long semesterId, SubjectGradeType type) {
        List<SubjectGrade> records = subjectGradeRepository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        studentId, subjectSectionId, semesterId, type);

        if (records.isEmpty()) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (SubjectGrade g : records) {
            BigDecimal score = pickScore(g);
            if (score != null) {
                sum = sum.add(score);
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return sum.divide(BigDecimal.valueOf(count), SCALE, ROUNDING);
    }

    /**
     * Single most-recent score of given type (used for GK/CK which are typically
     * 1-per-semester per BR-GRADEBOOK-001). If multiple records exist, average
     * them (defensive — TT 22 caps but database doesn't).
     */
    private BigDecimal singleScoreOfType(Long studentId, Long subjectSectionId,
                                         Long semesterId, SubjectGradeType type) {
        return arithmeticMeanOfType(studentId, subjectSectionId, semesterId, type);
    }

    /**
     * Pick the score for a SubjectGrade record based on its assessment type.
     *
     * <p>Phase 1C v1 backward-compat: {@link SubjectGrade} carries 4 score
     * columns (regularScore, midtermScore, finalScore, average). For new
     * Phase 1C records typed TX/GK/CK we read the matching column. The pre-
     * Phase-1C row layout (one row carrying all 3 components for backward
     * compat) is honored by V55 migration which marks legacy rows with
     * {@code type=TX} — those rows' regularScore is the TX value, callers
     * compute GK/CK by reading dedicated rows when present.
     */
    private BigDecimal pickScore(SubjectGrade g) {
        if (g.getType() == null) {
            // Legacy row pre-Phase-1C — should be backfilled to TX by V55, but
            // be defensive: treat as TX.
            return g.getRegularScore();
        }
        return switch (g.getType()) {
            case TX -> g.getRegularScore();
            case GK -> g.getMidtermScore() != null ? g.getMidtermScore() : g.getRegularScore();
            case CK -> g.getFinalScore() != null ? g.getFinalScore() : g.getRegularScore();
        };
    }

    private Long findSemesterId(Long academicYearId, SemesterType type) {
        return semesterRepository
                .findByAcademicYearIdAndTypeAndDeletedFalse(academicYearId, type)
                .map(Semester::getId)
                .orElse(null);
    }
}
