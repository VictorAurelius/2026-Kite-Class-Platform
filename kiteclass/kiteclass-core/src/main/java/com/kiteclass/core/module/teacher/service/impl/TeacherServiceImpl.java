package com.kiteclass.core.module.teacher.service.impl;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.entity.Teacher;
import com.kiteclass.core.module.teacher.mapper.TeacherMapper;
import com.kiteclass.core.module.teacher.repository.TeacherRepository;
import com.kiteclass.core.module.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of TeacherService interface.
 *
 * <p>Handles:
 * <ul>
 *   <li>Business logic validation (email uniqueness - BR-TEACHER-001)</li>
 *   <li>Entity mapping via TeacherMapper</li>
 *   <li>Caching with Redis</li>
 *   <li>Transaction management</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;

    @Override
    @Transactional
    @CacheEvict(value = "teachers", allEntries = true)
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        log.info("Creating teacher with email: {}", request.email());

        // BR-TEACHER-001: Validate email uniqueness
        if (teacherRepository.existsByEmailAndDeletedFalse(request.email())) {
            log.warn("Duplicate email: {}", request.email());
            throw new DuplicateResourceException("email", request.email());
        }

        Teacher teacher = teacherMapper.toEntity(request);
        Teacher saved = teacherRepository.save(teacher);

        log.info("Created teacher with ID: {}", saved.getId());
        return teacherMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "teachers", key = "#id")
    public TeacherResponse getTeacherById(Long id) {
        log.debug("Fetching teacher with ID: {}", id);

        Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", id);
                    return new EntityNotFoundException("Teacher", id);
                });

        return teacherMapper.toResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> getTeachers(String search, String status, Pageable pageable) {
        log.debug("Searching teachers with search='{}', status='{}', page={}", search, status, pageable.getPageNumber());

        TeacherStatus teacherStatus = status != null ? TeacherStatus.valueOf(status) : null;

        Page<Teacher> teacherPage = teacherRepository.findBySearchCriteria(search, teacherStatus, pageable);

        Page<TeacherResponse> responsePage = teacherPage.map(teacherMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    @CacheEvict(value = "teachers", key = "#id")
    public TeacherResponse updateTeacher(Long id, UpdateTeacherRequest request) {
        log.info("Updating teacher with ID: {}", id);

        Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", id);
                    return new EntityNotFoundException("Teacher", id);
                });

        // Note: Email is ignored in mapper and cannot be changed after creation

        teacherMapper.updateEntity(teacher, request);
        Teacher updated = teacherRepository.save(teacher);

        log.info("Updated teacher with ID: {}", id);
        return teacherMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "teachers", key = "#id")
    public void deleteTeacher(Long id) {
        log.info("Deleting teacher with ID: {}", id);

        Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", id);
                    return new EntityNotFoundException("Teacher", id);
                });

        teacher.markAsDeleted();
        teacherRepository.save(teacher);

        log.info("Deleted teacher with ID: {}", id);
    }
}
