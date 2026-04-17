package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.common.constant.ParentLinkType;
import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.parent.dto.ChildSummaryResponse;
import com.kiteclass.core.module.parent.dto.ParentInternalResponse;
import com.kiteclass.core.module.parent.dto.ParentResponse;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.impl.ParentServiceImpl;
import com.kiteclass.core.module.student.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ParentServiceImpl}.
 *
 * <p>Focus: authorization boundaries (404 on unknown parent ids, empty list
 * when no children) and response-shape correctness. Tenant isolation itself
 * is enforced by the Hibernate {@code tenantFilter} — covered by integration
 * tests rather than here.
 *
 * @since 2.14.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentService")
class ParentServiceTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock private ParentRepository parentRepository;
    @Mock private ParentStudentLinkRepository linkRepository;

    @InjectMocks
    private ParentServiceImpl service;

    // ——— getParentById ————————————————————————————————————————————

    @Nested
    @DisplayName("getParentById")
    class GetParentById {

        @Test
        @DisplayName("returns flattened ParentResponse")
        void happyPath() {
            Parent parent = parent(10L);
            when(parentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(parent));

            ParentResponse response = service.getParentById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.email()).isEqualTo("p@example.com");
            assertThat(response.relationship()).isEqualTo("MOTHER");
            assertThat(response.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("throws EntityNotFoundException when parent missing")
        void notFound() {
            when(parentRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getParentById(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ——— getChildrenOfParent ——————————————————————————————————————

    @Nested
    @DisplayName("getChildrenOfParent")
    class GetChildrenOfParent {

        @Test
        @DisplayName("maps parent-student links into dashboard summaries")
        void returnsChildList() {
            Parent parent = parent(10L);
            when(parentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(parent));
            when(linkRepository.findByParentIdWithStudent(10L))
                    .thenReturn(List.of(link(parent, student(1L, "Con A"), ParentLinkType.PRIMARY),
                            link(parent, student(2L, "Con B"), ParentLinkType.SECONDARY)));

            List<ChildSummaryResponse> children = service.getChildrenOfParent(10L);

            assertThat(children).hasSize(2);
            assertThat(children.get(0).studentId()).isEqualTo(1L);
            assertThat(children.get(0).studentName()).isEqualTo("Con A");
            assertThat(children.get(0).linkType()).isEqualTo("PRIMARY");
            assertThat(children.get(1).linkType()).isEqualTo("SECONDARY");
        }

        @Test
        @DisplayName("blocks probing unknown parent ids with 404 (prevents cross-tenant snooping)")
        void unknownParent() {
            when(parentRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getChildrenOfParent(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("returns empty list when parent exists but has no links")
        void emptyList() {
            Parent parent = parent(10L);
            when(parentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(parent));
            when(linkRepository.findByParentIdWithStudent(10L)).thenReturn(List.of());

            assertThat(service.getChildrenOfParent(10L)).isEmpty();
        }
    }

    // ——— getInternalParentView ————————————————————————————————————

    @Nested
    @DisplayName("getInternalParentView")
    class GetInternalParentView {

        @Test
        @DisplayName("returns flattened profile + linkedStudentIds for JWT claim")
        void happyPath() {
            Parent parent = parent(10L);
            when(parentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(parent));
            when(linkRepository.findStudentIdsByParentId(10L)).thenReturn(List.of(1L, 2L));

            ParentInternalResponse response = service.getInternalParentView(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.linkedStudentIds()).containsExactly(1L, 2L);
            assertThat(response.status()).isEqualTo("ACTIVE");
            assertThat(response.email()).isEqualTo("p@example.com");
        }
    }

    // ——— fixtures ————————————————————————————————————————————————

    private Parent parent(Long id) {
        Parent p = Parent.builder()
                .email("p@example.com")
                .fullName("Nguyễn Thị B")
                .phoneNumber("0912345678")
                .relationship(ParentRelationship.MOTHER)
                .status(ParentStatus.ACTIVE)
                .build();
        p.setId(id);
        p.setInstanceId(TENANT);
        return p;
    }

    private Student student(Long id, String name) {
        Student s = Student.builder().name(name).build();
        s.setId(id);
        s.setInstanceId(TENANT);
        return s;
    }

    private ParentStudentLink link(Parent parent, Student student, ParentLinkType type) {
        ParentStudentLink link = ParentStudentLink.builder()
                .parent(parent)
                .student(student)
                .linkType(type)
                .build();
        link.setInstanceId(TENANT);
        return link;
    }
}
