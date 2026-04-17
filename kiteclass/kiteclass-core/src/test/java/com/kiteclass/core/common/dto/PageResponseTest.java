package com.kiteclass.core.common.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PageResponse}.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
class PageResponseTest {

    @Test
    void of_shouldCreatePageResponseWithCorrectMetadata() {
        // Given
        List<String> content = List.of("item1", "item2", "item3");
        int page = 0;
        int size = 10;
        long totalElements = 23;

        // When
        PageResponse<String> response = PageResponse.of(content, page, size, totalElements);

        // Then
        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getPage()).isEqualTo(page);
        assertThat(response.getSize()).isEqualTo(size);
        assertThat(response.getTotalElements()).isEqualTo(totalElements);
        assertThat(response.getTotalPages()).isEqualTo(3);  // ceil(23/10) = 3
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void of_shouldMarkLastPageCorrectly() {
        // Given
        List<String> content = List.of("item1", "item2", "item3");
        int page = 2;  // Last page (0-indexed)
        int size = 10;
        long totalElements = 23;

        // When
        PageResponse<String> response = PageResponse.of(content, page, size, totalElements);

        // Then
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void of_shouldHandleEmptyContent() {
        // Given
        List<String> content = List.of();
        int page = 0;
        int size = 10;
        long totalElements = 0;

        // When
        PageResponse<String> response = PageResponse.of(content, page, size, totalElements);

        // Then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void from_shouldCreatePageResponseFromSpringDataPage() {
        // Given
        List<String> content = List.of("item1", "item2", "item3");
        Pageable pageable = PageRequest.of(1, 10);  // Page 1, size 10
        Page<String> page = new PageImpl<>(content, pageable, 23);

        // When
        PageResponse<String> response = PageResponse.from(page);

        // Then
        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(23);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void from_shouldHandleFirstPage() {
        // Given
        List<String> content = List.of("item1", "item2");
        Pageable pageable = PageRequest.of(0, 10);
        Page<String> page = new PageImpl<>(content, pageable, 15);

        // When
        PageResponse<String> response = PageResponse.from(page);

        // Then
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void from_shouldHandleLastPage() {
        // Given
        List<String> content = List.of("item1");
        Pageable pageable = PageRequest.of(2, 10);  // Last page
        Page<String> page = new PageImpl<>(content, pageable, 21);

        // When
        PageResponse<String> response = PageResponse.from(page);

        // Then
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
    }
}
