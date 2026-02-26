package com.kiteclass.core.module.enrollment.mapper;

import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Enrollment entity.
 *
 * <p>Handles conversions between entity and DTOs.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EnrollmentMapper {

    /**
     * Convert CreateEnrollmentRequest to Enrollment entity.
     *
     * @param request create request DTO
     * @return enrollment entity
     */
    @Mapping(target = "enrollmentDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "status", expression = "java(com.kiteclass.core.common.constant.EnrollmentStatus.PENDING_PAYMENT)")
    @Mapping(target = "finalAmount", ignore = true) // Calculated in @PrePersist
    Enrollment toEntity(CreateEnrollmentRequest request);

    /**
     * Convert Enrollment entity to EnrollmentResponse DTO.
     *
     * @param enrollment enrollment entity
     * @return response DTO
     */
    EnrollmentResponse toResponse(Enrollment enrollment);
}
