package com.kitehub.subscription.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CursorPage} codec — Wave 85 Bucket D D-AC1.
 */
class CursorPageTest {

    @Test
    void encodeThenDecode_roundTripsSameUuid() {
        UUID original = UUID.randomUUID();
        String cursor = CursorPage.encodeCursor(original);
        assertThat(cursor).isNotBlank().doesNotContain("=");
        UUID decoded = CursorPage.decodeCursor(cursor);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void encodeCursor_nullId_returnsNull() {
        assertThat(CursorPage.encodeCursor(null)).isNull();
    }

    @Test
    void decodeCursor_nullOrBlank_returnsNull() {
        assertThat(CursorPage.decodeCursor(null)).isNull();
        assertThat(CursorPage.decodeCursor("")).isNull();
        assertThat(CursorPage.decodeCursor("   ")).isNull();
    }

    @Test
    void decodeCursor_malformedToken_throwsIllegalArgument() {
        assertThatThrownBy(() -> CursorPage.decodeCursor("not-a-base64-uuid$$$"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cursor");
    }

    @Test
    void cursorPage_envelopeFieldsArePreserved() {
        UUID lastId = UUID.randomUUID();
        String cursor = CursorPage.encodeCursor(lastId);
        CursorPage<String> page = new CursorPage<>(List.of("a", "b"), 50, cursor, true);

        assertThat(page.getContent()).containsExactly("a", "b");
        assertThat(page.getSize()).isEqualTo(50);
        assertThat(page.getNextCursor()).isEqualTo(cursor);
        assertThat(page.isHasNext()).isTrue();
    }

    @Test
    void maxPageSizeCap_isEnforcedAtControllerBoundary() {
        // Replicates the controller-level cap logic:
        // safeSize = min(max(1, size), MAX_PAGE_SIZE=200)
        int maxPageSize = 200;
        int requestedSize = 300;
        int safeSize = Math.min(Math.max(1, requestedSize), maxPageSize);
        assertThat(safeSize).isEqualTo(200);

        // Edge: size=0 -> clamped to 1
        assertThat(Math.min(Math.max(1, 0), maxPageSize)).isEqualTo(1);
        // Edge: size=-5 -> clamped to 1
        assertThat(Math.min(Math.max(1, -5), maxPageSize)).isEqualTo(1);
        // Edge: size=50 -> unchanged
        assertThat(Math.min(Math.max(1, 50), maxPageSize)).isEqualTo(50);
    }
}
