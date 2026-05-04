package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.grade.entity.Transcript;
import com.kiteclass.core.module.grade.repository.TranscriptRepository;
import com.kiteclass.core.module.parent.dto.TranscriptResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.impl.ParentTranscriptServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ParentTranscriptServiceImpl}.
 *
 * <p>Critical scope guard: {@link ParentTranscriptServiceImpl#getTranscriptsForChild(Long, Long)}
 * MUST verify the {@code ParentStudentLink} edge between authenticated parent
 * and requested child BEFORE returning data — return 403 PARENT_NOT_LINKED
 * otherwise. Without this guard, any authenticated parent could read any
 * student's transcript by guessing IDs.
 *
 * <p>Phase 1A scope (GAP-321 K-12 LEGAL Phase 1A): single facet (transcript
 * read-only) proves end-to-end. Tests:
 * <ul>
 *   <li>Happy path — linked parent gets transcripts list</li>
 *   <li>Scope guard — unlinked parent gets 403 (NEVER returns data)</li>
 *   <li>Empty list — linked parent, child has no transcripts yet</li>
 *   <li>Soft-deleted edge — once-linked parent loses access on link delete</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentTranscriptService")
class ParentTranscriptServiceTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private TranscriptRepository transcriptRepository;

    @InjectMocks
    private ParentTranscriptServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;
    private static final Long OTHER_CHILD_ID = 999L;

    @Nested
    @DisplayName("getTranscriptsForChild")
    class GetTranscriptsForChild {

        @Test
        @DisplayName("returns transcripts when parent is linked to child (PRIMARY)")
        void happyPath_linked() {
            when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                    .thenReturn(true);
            when(transcriptRepository.findByStudentIdAndDeletedFalseOrderBySemesterDesc(CHILD_ID))
                    .thenReturn(List.of(transcript(1L, "Spring 2026", 2026, "3.45", "3.52"),
                            transcript(2L, "Fall 2025", 2025, "3.30", "3.40")));

            List<TranscriptResponse> result = service.getTranscriptsForChild(PARENT_ID, CHILD_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).semester()).isEqualTo("Spring 2026");
            assertThat(result.get(0).academicYear()).isEqualTo(2026);
            assertThat(result.get(0).semesterGpa()).isEqualByComparingTo("3.45");
            assertThat(result.get(0).cumulativeGpa()).isEqualByComparingTo("3.52");
            assertThat(result.get(1).semester()).isEqualTo("Fall 2025");
        }

        @Test
        @DisplayName("BLOCKS unlinked parent with 403 PARENT_NOT_LINKED — NEVER touches transcripts")
        void scopeGuard_unlinkedParent_throws403() {
            when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, OTHER_CHILD_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.getTranscriptsForChild(PARENT_ID, OTHER_CHILD_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "PARENT_NOT_LINKED")
                    .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

            // CRITICAL: must short-circuit before any data access — leaking even
            // existence of the transcript count would be PDPL violation.
            verify(transcriptRepository, never())
                    .findByStudentIdAndDeletedFalseOrderBySemesterDesc(OTHER_CHILD_ID);
        }

        @Test
        @DisplayName("returns empty list when child exists, parent linked, but no transcripts yet")
        void emptyList_noTranscripts() {
            when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                    .thenReturn(true);
            when(transcriptRepository.findByStudentIdAndDeletedFalseOrderBySemesterDesc(CHILD_ID))
                    .thenReturn(List.of());

            List<TranscriptResponse> result = service.getTranscriptsForChild(PARENT_ID, CHILD_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("rejects null parentId or childId with BusinessException")
        void rejectsNullArgs() {
            assertThatThrownBy(() -> service.getTranscriptsForChild(null, CHILD_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

            assertThatThrownBy(() -> service.getTranscriptsForChild(PARENT_ID, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ——— fixtures ————————————————————————————————————————————————

    private Transcript transcript(Long id, String semester, Integer year, String semGpa, String cumGpa) {
        Transcript t = Transcript.builder()
                .studentId(CHILD_ID)
                .semester(semester)
                .academicYear(year)
                .totalCredits(BigDecimal.valueOf(12.0))
                .semesterGpa(new BigDecimal(semGpa))
                .cumulativeGpa(new BigDecimal(cumGpa))
                .totalCourses(4)
                .passedCourses(4)
                .failedCourses(0)
                .build();
        t.setId(id);
        return t;
    }
}
