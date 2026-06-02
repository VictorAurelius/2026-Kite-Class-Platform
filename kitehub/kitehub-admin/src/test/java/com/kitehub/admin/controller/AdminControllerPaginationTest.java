package com.kitehub.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link AdminController#clampPageable(Pageable)} helper
 * (GAP-126). Verifies pagination defaults + max-size cap independently of
 * Spring / DB.
 *
 * <p>{@code AdminController} is {@code @Deprecated(since = "v1", forRemoval = true)} per GAP-654
 * (legacy {@code /api/platform/admin} surface); this test intentionally exercises its still-live
 * pagination helper. {@code forRemoval = true} raises the <em>removal</em> lint category (not
 * <em>deprecation</em>) since Java 9, hence the class-level {@code @SuppressWarnings("removal")}.</p>
 */
@SuppressWarnings("removal")
class AdminControllerPaginationTest {

    @Test
    void clampPageable_nullInput_returnsDefaultSize20() {
        Pageable result = AdminController.clampPageable(null);

        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    void clampPageable_sizeWithinLimit_passesThrough() {
        Pageable input = PageRequest.of(2, 50);
        Pageable result = AdminController.clampPageable(input);

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(50);
    }

    @Test
    void clampPageable_sizeAtMaxBoundary_returnsMax() {
        Pageable input = PageRequest.of(0, 100);
        Pageable result = AdminController.clampPageable(input);

        assertThat(result.getPageSize()).isEqualTo(100);
    }

    @Test
    void clampPageable_oversizedRequest_clampsTo100AndPreservesPageNumber() {
        Pageable input = PageRequest.of(3, 5_000, Sort.by("createdAt"));
        Pageable result = AdminController.clampPageable(input);

        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getPageNumber()).isEqualTo(3);
        assertThat(result.getSort()).isEqualTo(Sort.by("createdAt"));
    }

    @Test
    void constants_documentedDefaults() {
        // Guards against accidental edits drifting away from GAP-126 §AC.
        assertThat(AdminController.DEFAULT_PAGE_SIZE).isEqualTo(20);
        assertThat(AdminController.MAX_PAGE_SIZE).isEqualTo(100);
    }
}
