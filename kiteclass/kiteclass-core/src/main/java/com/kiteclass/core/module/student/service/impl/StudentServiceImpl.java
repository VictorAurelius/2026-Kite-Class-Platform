package com.kiteclass.core.module.student.service.impl;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.mapper.StudentMapper;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of StudentService interface.
 *
 * <p>Handles:
 * <ul>
 *   <li>Business logic validation (email/phone uniqueness)</li>
 *   <li>Entity mapping via StudentMapper</li>
 *   <li>Caching with Redis</li>
 *   <li>Transaction management</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    /**
     * Tạo học viên mới.
     *
     * <p>Validates email and phone uniqueness before creating student.
     *
     * @param request Thông tin học viên cần tạo (name, email, phone, dateOfBirth, gender, address)
     * @return StudentResponse chứa thông tin học viên đã tạo
     * @throws DuplicateResourceException nếu email hoặc phone đã tồn tại trong hệ thống
     */
    @Override
    @Transactional
    @CacheEvict(value = "students", allEntries = true)
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("Creating student with email: {}", request.email());

        // Validate email uniqueness
        if (request.email() != null && studentRepository.existsByEmailAndDeletedFalse(request.email())) {
            log.warn("Duplicate student email: {}", request.email());
            throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", request.email());
        }

        // Validate phone uniqueness
        if (request.phone() != null && studentRepository.existsByPhoneAndDeletedFalse(request.phone())) {
            log.warn("Duplicate student phone: {}", request.phone());
            throw new DuplicateResourceException("STUDENT_PHONE_EXISTS", request.phone());
        }

        Student student = studentMapper.toEntity(request);
        Student saved = studentRepository.save(student);

        log.info("Created student with ID: {}", saved.getId());
        return studentMapper.toResponse(saved);
    }

    /**
     * Lấy thông tin chi tiết học viên theo ID.
     *
     * <p>Result is cached in Redis with key "students::{id}".
     *
     * @param id ID của học viên cần lấy thông tin
     * @return StudentResponse chứa thông tin chi tiết học viên
     * @throws EntityNotFoundException nếu không tìm thấy học viên với ID này
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "students", key = "#id")
    public StudentResponse getStudentById(Long id) {
        log.debug("Fetching student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", id);
                });

        return studentMapper.toResponse(student);
    }

    /**
     * Tìm kiếm danh sách học viên với phân trang.
     *
     * <p>Supports full-text search by name, email, phone and filtering by status.
     *
     * @param search Từ khóa tìm kiếm (name, email, hoặc phone), có thể null
     * @param status Trạng thái học viên (ACTIVE, INACTIVE, GRADUATED), có thể null
     * @param pageable Thông tin phân trang và sắp xếp
     * @return PageResponse chứa danh sách học viên và thông tin phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(String search, String status, Pageable pageable) {
        log.debug("Searching students with search='{}', status='{}', page={}", search, status, pageable.getPageNumber());

        StudentStatus studentStatus = status != null ? StudentStatus.valueOf(status) : null;

        Page<Student> studentPage = studentRepository.findBySearchCriteria(search, studentStatus, pageable);

        Page<StudentResponse> responsePage = studentPage.map(studentMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /**
     * Cập nhật thông tin học viên.
     *
     * <p>Validates email and phone uniqueness if they are changed.
     * Only non-null fields in request will be updated.
     *
     * @param id ID của học viên cần cập nhật
     * @param request Thông tin cần cập nhật (partial update, các field null sẽ được bỏ qua)
     * @return StudentResponse chứa thông tin học viên sau khi cập nhật
     * @throws EntityNotFoundException nếu không tìm thấy học viên với ID này
     * @throws DuplicateResourceException nếu email hoặc phone mới đã tồn tại
     */
    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        log.info("Updating student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", id);
                });

        // Validate email uniqueness if changed
        if (request.email() != null && !request.email().equals(student.getEmail())) {
            if (studentRepository.existsByEmailAndDeletedFalse(request.email())) {
                log.warn("Duplicate student email: {}", request.email());
                throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", request.email());
            }
        }

        // Validate phone uniqueness if changed
        if (request.phone() != null && !request.phone().equals(student.getPhone())) {
            if (studentRepository.existsByPhoneAndDeletedFalse(request.phone())) {
                log.warn("Duplicate student phone: {}", request.phone());
                throw new DuplicateResourceException("STUDENT_PHONE_EXISTS", request.phone());
            }
        }

        studentMapper.updateEntity(student, request);
        Student updated = studentRepository.save(student);

        log.info("Updated student with ID: {}", id);
        return studentMapper.toResponse(updated);
    }

    /**
     * Xóa học viên (soft delete).
     *
     * <p>Marks the student as deleted without physically removing from database.
     * The student will be excluded from all queries using deletedFalse filters.
     *
     * @param id ID của học viên cần xóa
     * @throws EntityNotFoundException nếu không tìm thấy học viên với ID này
     */
    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public void deleteStudent(Long id) {
        log.info("Deleting student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", id);
                });

        student.markAsDeleted();
        studentRepository.save(student);

        log.info("Deleted student with ID: {}", id);
    }
}
