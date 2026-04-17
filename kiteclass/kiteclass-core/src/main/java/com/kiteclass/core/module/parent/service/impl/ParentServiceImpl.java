package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.parent.dto.ChildSummaryResponse;
import com.kiteclass.core.module.parent.dto.ParentInternalResponse;
import com.kiteclass.core.module.parent.dto.ParentResponse;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ParentService;
import com.kiteclass.core.module.student.entity.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed implementation of {@link ParentService}.
 *
 * <p>All reads are {@code @Transactional(readOnly = true)} so that the
 * Hibernate {@code tenantFilter} is applied automatically (it's activated by
 * the interceptor on each HTTP request).
 *
 * @since 2.14.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentServiceImpl implements ParentService {

    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository linkRepository;

    @Override
    @Transactional(readOnly = true)
    public ParentResponse getParentById(Long parentId) {
        Parent parent = loadParent(parentId);
        return toResponse(parent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChildSummaryResponse> getChildrenOfParent(Long parentId) {
        // Assert the parent exists in the current tenant before listing — stops
        // a caller from probing other tenants by iterating ids.
        loadParent(parentId);

        List<ParentStudentLink> links = linkRepository.findByParentIdWithStudent(parentId);
        return links.stream()
                .map(this::toChildSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParentInternalResponse getInternalParentView(Long parentId) {
        Parent parent = loadParent(parentId);
        List<Long> studentIds = linkRepository.findStudentIdsByParentId(parentId);
        return new ParentInternalResponse(
                parent.getId(),
                parent.getEmail(),
                parent.getFullName(),
                parent.getPhoneNumber(),
                parent.getRelationship().name(),
                parent.getStatus().name(),
                studentIds
        );
    }

    // ——— helpers ————————————————————————————————————————————————

    private Parent loadParent(Long parentId) {
        return parentRepository.findByIdAndDeletedFalse(parentId)
                .orElseThrow(() -> {
                    log.warn("Parent not found: id={}", parentId);
                    return new EntityNotFoundException("PARENT_NOT_FOUND", (Object) parentId);
                });
    }

    private ParentResponse toResponse(Parent parent) {
        return new ParentResponse(
                parent.getId(),
                parent.getFullName(),
                parent.getEmail(),
                parent.getPhoneNumber(),
                parent.getRelationship().name(),
                parent.getStatus().name()
        );
    }

    private ChildSummaryResponse toChildSummary(ParentStudentLink link) {
        Student student = link.getStudent();
        // className + grade are intentionally null in MVP — Wave 5 will join
        // with homeroom_classes / subject_grades once parent-visible projections
        // are settled.
        return new ChildSummaryResponse(
                student.getId(),
                student.getName(),
                null,
                null,
                link.getLinkType().name()
        );
    }
}
