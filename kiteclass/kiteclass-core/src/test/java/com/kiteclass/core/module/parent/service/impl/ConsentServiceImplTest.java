package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.student.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit-level branch-coverage test for {@link ConsentServiceImpl}.
 *
 * <p>Covers the gate matrix that BR-PARENT-PORTAL-011 mandates:
 * consent=true → permit, consent=false → deny, missing field → deny,
 * missing parent-student link → deny, null inputs → deny.
 *
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentServiceImpl branch coverage")
class ConsentServiceImplTest {

    @Mock private ParentStudentLinkRepository linkRepository;

    private ConsentServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;
    private static final Long OTHER_CHILD_ID = 200L;

    @BeforeEach
    void setUp() {
        service = new ConsentServiceImpl(linkRepository);
    }

    private ParentStudentLink linkWith(Long childId, ParentalConsent consent) {
        Student s = new Student();
        s.setId(childId);
        ParentStudentLink l = new ParentStudentLink();
        l.setStudent(s);
        l.setParentalConsent(consent);
        return l;
    }

    private ParentStudentLink linkWith(Long childId, Map<String, Boolean> fields) {
        return linkWith(childId, new ParentalConsent(fields, 1, null));
    }

    @Nested
    @DisplayName("checkConsent — gate matrix")
    class CheckConsentMatrix {

        @Test
        @DisplayName("null parentId → false (no fail-open)")
        void nullParent_returnsFalse() {
            assertThat(service.checkConsent(null, CHILD_ID, "fees")).isFalse();
        }

        @Test
        @DisplayName("null childId → false")
        void nullChild_returnsFalse() {
            assertThat(service.checkConsent(PARENT_ID, null, "fees")).isFalse();
        }

        @Test
        @DisplayName("null/blank field → false")
        void nullField_returnsFalse() {
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, null)).isFalse();
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "")).isFalse();
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "  ")).isFalse();
        }

        @Test
        @DisplayName("no link rows → false (parent never linked)")
        void noLink_returnsFalse() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of());
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isFalse();
        }

        @Test
        @DisplayName("link exists for OTHER child → false (correct child filter)")
        void linkForOtherChild_returnsFalse() {
            ParentStudentLink other = linkWith(OTHER_CHILD_ID,
                    Map.of("fees", true));
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(other));
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isFalse();
        }

        @Test
        @DisplayName("link exists, fields map empty → false (default consent)")
        void emptyConsentFields_returnsFalse() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, new HashMap<>())));
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isFalse();
        }

        @Test
        @DisplayName("field explicitly false → false")
        void fieldFalse_returnsFalse() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, Map.of("fees", false))));
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isFalse();
        }

        @Test
        @DisplayName("field explicitly true → true (granted)")
        void fieldTrue_returnsTrue() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, Map.of("fees", true))));
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isTrue();
        }

        @Test
        @DisplayName("different field granted, requested field missing → false")
        void differentFieldGranted_returnsFalse() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, Map.of("conduct", true))));
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isFalse();
        }

        @Test
        @DisplayName("link's consent is null → false")
        void nullConsentBlob_returnsFalse() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, (ParentalConsent) null)));
            assertThat(service.checkConsent(PARENT_ID, CHILD_ID, "fees")).isFalse();
        }
    }

    @Nested
    @DisplayName("getConsent — read")
    class GetConsent {

        @Test
        @DisplayName("null inputs → 400 BAD_REQUEST")
        void nullInputs_throws400() {
            assertThatThrownBy(() -> service.getConsent(null, CHILD_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
            assertThatThrownBy(() -> service.getConsent(PARENT_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("no link → returns default consent (not 404 — settings GET is informational)")
        void noLink_returnsDefault() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of());
            ParentalConsent c = service.getConsent(PARENT_ID, CHILD_ID);
            assertThat(c.fields()).isEmpty();
            assertThat(c.version()).isEqualTo(1);
            assertThat(c.updatedAt()).isNull();
        }

        @Test
        @DisplayName("link with consent → returns consent")
        void linkExists_returnsConsent() {
            ParentalConsent c = new ParentalConsent(Map.of("fees", true), 3, null);
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, c)));
            assertThat(service.getConsent(PARENT_ID, CHILD_ID).version()).isEqualTo(3);
            assertThat(service.getConsent(PARENT_ID, CHILD_ID).fields()).containsEntry("fees", true);
        }
    }

    @Nested
    @DisplayName("getConsentVersion")
    class GetVersion {

        @Test
        @DisplayName("null inputs → 1 (default)")
        void nullInputs_returnsOne() {
            assertThat(service.getConsentVersion(null, CHILD_ID)).isEqualTo(1);
            assertThat(service.getConsentVersion(PARENT_ID, null)).isEqualTo(1);
        }

        @Test
        @DisplayName("no link → 1")
        void noLink_returnsOne() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of());
            assertThat(service.getConsentVersion(PARENT_ID, CHILD_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("returns the stored version")
        void linkExists_returnsVersion() {
            ParentalConsent c = new ParentalConsent(Map.of(), 5, null);
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(linkWith(CHILD_ID, c)));
            assertThat(service.getConsentVersion(PARENT_ID, CHILD_ID)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("bumpConsent — write")
    class BumpConsent {

        @Test
        @DisplayName("null inputs → 400 BAD_REQUEST")
        void nullInputs_throws400() {
            assertThatThrownBy(() -> service.bumpConsent(null, CHILD_ID, Map.of()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
            assertThatThrownBy(() -> service.bumpConsent(PARENT_ID, null, Map.of()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
            assertThatThrownBy(() -> service.bumpConsent(PARENT_ID, CHILD_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("no link → 404 PARENT_CONSENT_LINK_NOT_FOUND")
        void noLink_throws404() {
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of());
            assertThatThrownBy(() -> service.bumpConsent(PARENT_ID, CHILD_ID, Map.of("fees", true)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "PARENT_CONSENT_LINK_NOT_FOUND")
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("happy path: merges sparse update + bumps version + sets updatedAt")
        void happyPath_mergesAndBumps() {
            ParentStudentLink link = linkWith(CHILD_ID, new ParentalConsent(
                    new HashMap<>(Map.of("fees", false, "conduct", true)),
                    3,
                    null));
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(link));

            ParentalConsent next = service.bumpConsent(
                    PARENT_ID, CHILD_ID, Map.of("fees", true));

            assertThat(next.version()).isEqualTo(4);
            assertThat(next.fields()).containsEntry("fees", true);
            // existing field preserved
            assertThat(next.fields()).containsEntry("conduct", true);
            assertThat(next.updatedAt()).isNotNull();
            // entity field was mutated (JPA dirty checking flushes on commit)
            assertThat(link.getParentalConsent().version()).isEqualTo(4);
        }

        @Test
        @DisplayName("null existing consent treated as default (no NPE)")
        void nullExistingConsent_handled() {
            ParentStudentLink link = linkWith(CHILD_ID, (ParentalConsent) null);
            when(linkRepository.findByParentIdWithStudent(PARENT_ID))
                    .thenReturn(List.of(link));

            ParentalConsent next = service.bumpConsent(
                    PARENT_ID, CHILD_ID, Map.of("fees", true));

            assertThat(next.version()).isEqualTo(2);
            assertThat(next.fields()).containsEntry("fees", true);
        }
    }
}
