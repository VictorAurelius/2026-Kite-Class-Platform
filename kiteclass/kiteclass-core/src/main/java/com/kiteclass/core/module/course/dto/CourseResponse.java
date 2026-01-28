package com.kiteclass.core.module.course.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Course entity.
 *
 * <p>Contains complete course information for API responses including:
 * <ul>
 *   <li>Basic information (id, name, code, description)</li>
 *   <li>Educational content (syllabus, objectives, prerequisites, targetAudience)</li>
 *   <li>Course details (teacherId, durationWeeks, totalSessions)</li>
 *   <li>Pricing and cover image</li>
 *   <li>Status and timestamps</li>
 * </ul>
 *
 * @param id Course ID
 * @param name Course name
 * @param code Course code (unique)
 * @param description Course description
 * @param syllabus Course syllabus
 * @param objectives Learning objectives
 * @param prerequisites Course prerequisites
 * @param targetAudience Target audience description
 * @param teacherId Teacher ID who created the course
 * @param durationWeeks Course duration in weeks
 * @param totalSessions Total number of sessions
 * @param price Course price in VND
 * @param status Current status (DRAFT, PUBLISHED, ARCHIVED)
 * @param coverImageUrl URL to course cover image
 * @param createdAt Timestamp when course was created
 * @param updatedAt Timestamp when course was last updated
 * @author KiteClass Team
 * @since 2.4.0
 */
public record CourseResponse(
        Long id,
        String name,
        String code,
        String description,
        String syllabus,
        String objectives,
        String prerequisites,
        String targetAudience,
        Long teacherId,
        Integer durationWeeks,
        Integer totalSessions,
        BigDecimal price,
        String status,
        String coverImageUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
