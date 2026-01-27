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

    @Override
    @Transactional
    @CacheEvict(value = "students", allEntries = true)
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("Creating student with email: {}", request.email());

        // Validate email uniqueness
        if (request.email() != null && studentRepository.existsByEmailAndDeletedFalse(request.email())) {
            log.warn("Duplicate email: {}", request.email());
            throw new DuplicateResourceException("email", request.email());
        }

        // Validate phone uniqueness
        if (request.phone() != null && studentRepository.existsByPhoneAndDeletedFalse(request.phone())) {
            log.warn("Duplicate phone: {}", request.phone());
            throw new DuplicateResourceException("phone", request.phone());
        }

        Student student = studentMapper.toEntity(request);
        Student saved = studentRepository.save(student);

        log.info("Created student with ID: {}", saved.getId());
        return studentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "students", key = "#id")
    public StudentResponse getStudentById(Long id) {
        log.debug("Fetching student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("Student", id);
                });

        return studentMapper.toResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(String search, String status, Pageable pageable) {
        log.debug("Searching students with search='{}', status='{}', page={}", search, status, pageable.getPageNumber());

        StudentStatus studentStatus = status != null ? StudentStatus.valueOf(status) : null;

        Page<Student> studentPage = studentRepository.findBySearchCriteria(search, studentStatus, pageable);

        Page<StudentResponse> responsePage = studentPage.map(studentMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        log.info("Updating student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("Student", id);
                });

        // Validate email uniqueness if changed
        if (request.email() != null && !request.email().equals(student.getEmail())) {
            if (studentRepository.existsByEmailAndDeletedFalse(request.email())) {
                log.warn("Duplicate email: {}", request.email());
                throw new DuplicateResourceException("email", request.email());
            }
        }

        // Validate phone uniqueness if changed
        if (request.phone() != null && !request.phone().equals(student.getPhone())) {
            if (studentRepository.existsByPhoneAndDeletedFalse(request.phone())) {
                log.warn("Duplicate phone: {}", request.phone());
                throw new DuplicateResourceException("phone", request.phone());
            }
        }

        studentMapper.updateEntity(student, request);
        Student updated = studentRepository.save(student);

        log.info("Updated student with ID: {}", id);
        return studentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public void deleteStudent(Long id) {
        log.info("Deleting student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("Student", id);
                });

        student.markAsDeleted();
        studentRepository.save(student);

        log.info("Deleted student with ID: {}", id);
    }
}
