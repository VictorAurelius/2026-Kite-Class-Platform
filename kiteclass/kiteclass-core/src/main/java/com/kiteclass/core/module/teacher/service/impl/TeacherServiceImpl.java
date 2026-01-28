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

    /**
     * Tạo giáo viên mới.
     *
     * <p>Validates email uniqueness before creating teacher (BR-TEACHER-001).
     *
     * @param request Thông tin giáo viên cần tạo (name, email, phone, specialization, bio, qualification, experienceYears)
     * @return TeacherResponse chứa thông tin giáo viên đã tạo
     * @throws DuplicateResourceException nếu email đã tồn tại trong hệ thống
     */
    @Override
    @Transactional
    @CacheEvict(value = "teachers", allEntries = true)
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        log.info("Creating teacher with email: {}", request.email());

        // BR-TEACHER-001: Validate email uniqueness
        if (teacherRepository.existsByEmailAndDeletedFalse(request.email())) {
            log.warn("Duplicate teacher email: {}", request.email());
            throw new DuplicateResourceException("TEACHER_EMAIL_EXISTS", request.email());
        }

        Teacher teacher = teacherMapper.toEntity(request);
        Teacher saved = teacherRepository.save(teacher);

        log.info("Created teacher with ID: {}", saved.getId());
        return teacherMapper.toResponse(saved);
    }

    /**
     * Lấy thông tin chi tiết giáo viên theo ID.
     *
     * <p>Result is cached in Redis with key "teachers::{id}".
     *
     * @param id ID của giáo viên cần lấy thông tin
     * @return TeacherResponse chứa thông tin chi tiết giáo viên
     * @throws EntityNotFoundException nếu không tìm thấy giáo viên với ID này
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "teachers", key = "#id")
    public TeacherResponse getTeacherById(Long id) {
        log.debug("Fetching teacher with ID: {}", id);

        Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", id);
                    return new EntityNotFoundException("TEACHER_NOT_FOUND", id);
                });

        return teacherMapper.toResponse(teacher);
    }

    /**
     * Tìm kiếm danh sách giáo viên với phân trang.
     *
     * <p>Supports full-text search by name, email, specialization and filtering by status.
     *
     * @param search Từ khóa tìm kiếm (name, email, hoặc specialization), có thể null
     * @param status Trạng thái giáo viên (ACTIVE, INACTIVE, ON_LEAVE), có thể null
     * @param pageable Thông tin phân trang và sắp xếp
     * @return PageResponse chứa danh sách giáo viên và thông tin phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> getTeachers(String search, String status, Pageable pageable) {
        log.debug("Searching teachers with search='{}', status='{}', page={}", search, status, pageable.getPageNumber());

        TeacherStatus teacherStatus = status != null ? TeacherStatus.valueOf(status) : null;

        Page<Teacher> teacherPage = teacherRepository.findBySearchCriteria(search, teacherStatus, pageable);

        Page<TeacherResponse> responsePage = teacherPage.map(teacherMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /**
     * Cập nhật thông tin giáo viên.
     *
     * <p>Email cannot be changed after creation and will be ignored if provided.
     * Only non-null fields in request will be updated.
     *
     * @param id ID của giáo viên cần cập nhật
     * @param request Thông tin cần cập nhật (partial update, các field null sẽ được bỏ qua)
     * @return TeacherResponse chứa thông tin giáo viên sau khi cập nhật
     * @throws EntityNotFoundException nếu không tìm thấy giáo viên với ID này
     */
    @Override
    @Transactional
    @CacheEvict(value = "teachers", key = "#id")
    public TeacherResponse updateTeacher(Long id, UpdateTeacherRequest request) {
        log.info("Updating teacher with ID: {}", id);

        Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", id);
                    return new EntityNotFoundException("TEACHER_NOT_FOUND", id);
                });

        // Note: Email is ignored in mapper and cannot be changed after creation

        teacherMapper.updateEntity(teacher, request);
        Teacher updated = teacherRepository.save(teacher);

        log.info("Updated teacher with ID: {}", id);
        return teacherMapper.toResponse(updated);
    }

    /**
     * Xóa giáo viên (soft delete).
     *
     * <p>Marks the teacher as deleted without physically removing from database.
     * The teacher will be excluded from all queries using deletedFalse filters.
     *
     * @param id ID của giáo viên cần xóa
     * @throws EntityNotFoundException nếu không tìm thấy giáo viên với ID này
     */
    @Override
    @Transactional
    @CacheEvict(value = "teachers", key = "#id")
    public void deleteTeacher(Long id) {
        log.info("Deleting teacher with ID: {}", id);

        Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", id);
                    return new EntityNotFoundException("TEACHER_NOT_FOUND", id);
                });

        teacher.markAsDeleted();
        teacherRepository.save(teacher);

        log.info("Deleted teacher with ID: {}", id);
    }
}
