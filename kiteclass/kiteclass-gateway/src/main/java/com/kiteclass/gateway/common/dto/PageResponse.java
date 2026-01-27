package com.kiteclass.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper for list endpoints.
 *
 * <p>Contains:
 * <ul>
 *   <li>List of items for current page</li>
 *   <li>Pagination metadata (page, size, total)</li>
 * </ul>
 *
 * @param <T> Type of items in the list
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    /**
     * Creates a PageResponse from content and pagination info.
     *
     * @param content       list of items
     * @param page          current page number (0-indexed)
     * @param size          page size
     * @param totalElements total number of elements
     * @param <T>           type of items
     * @return PageResponse with calculated metadata
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}
