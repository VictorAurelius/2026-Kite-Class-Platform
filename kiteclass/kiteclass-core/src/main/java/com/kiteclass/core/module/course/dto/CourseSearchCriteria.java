package com.kiteclass.core.module.course.dto;

/**
 * Search criteria DTO for course queries.
 *
 * <p>Supports filtering by:
 * <ul>
 *   <li>search: keyword search in name and code (case-insensitive, partial match)</li>
 *   <li>status: filter by course status (DRAFT, PUBLISHED, ARCHIVED)</li>
 *   <li>teacherId: filter by teacher who created the course</li>
 *   <li>page: page number (0-based)</li>
 *   <li>size: page size</li>
 *   <li>sort: sort field and direction (e.g., "name,asc", "createdAt,desc")</li>
 * </ul>
 *
 * <p>All criteria are optional. Null values mean no filtering for that criterion.
 *
 * @param search Keyword to search in name and code (optional)
 * @param status Course status filter (optional)
 * @param teacherId Teacher ID filter (optional)
 * @param page Page number (default: 0)
 * @param size Page size (default: 20)
 * @param sort Sort criteria (default: "createdAt,desc")
 * @author KiteClass Team
 * @since 2.4.0
 */
public record CourseSearchCriteria(
        String search,
        String status,
        Long teacherId,
        Integer page,
        Integer size,
        String sort
) {
    /**
     * Constructor with default values.
     */
    public CourseSearchCriteria {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        if (sort == null || sort.isBlank()) {
            sort = "createdAt,desc";
        }
    }
}
