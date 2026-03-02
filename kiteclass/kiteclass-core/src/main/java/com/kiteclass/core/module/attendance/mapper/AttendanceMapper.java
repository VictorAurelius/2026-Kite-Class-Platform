package com.kiteclass.core.module.attendance.mapper;

import com.kiteclass.core.module.attendance.dto.AttendanceResponse;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.entity.Attendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Attendance entity.
 *
 * <p>Handles conversions between entity and DTOs.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AttendanceMapper {

    /**
     * Convert CreateAttendanceRequest to Attendance entity.
     *
     * @param request create request DTO
     * @return attendance entity
     */
    @Mapping(target = "markedDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "pointsAwarded", ignore = true) // Calculated in @PrePersist
    @Mapping(target = "markedBy", ignore = true) // Set by service from security context
    Attendance toEntity(CreateAttendanceRequest request);

    /**
     * Convert Attendance entity to AttendanceResponse DTO.
     * Note: studentName, sessionNumber, and markedByName should be populated by service.
     *
     * @param attendance attendance entity
     * @return response DTO
     */
    @Mapping(target = "studentName", ignore = true) // Populated by service
    @Mapping(target = "sessionNumber", ignore = true) // Populated by service
    @Mapping(target = "markedByName", ignore = true) // Populated by service
    AttendanceResponse toResponse(Attendance attendance);
}
