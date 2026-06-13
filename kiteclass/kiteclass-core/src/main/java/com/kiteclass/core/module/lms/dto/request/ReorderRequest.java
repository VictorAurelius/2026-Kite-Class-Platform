package com.kiteclass.core.module.lms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Batch reorder request for modules or lessons (drag-drop FE).
 *
 * <p>The FE sends the FULL ordered set of sibling items (all modules of a course,
 * or all lessons of a module). Each item carries its target {@code orderNumber}.
 * The server applies them atomically — see {@code LmsService.reorderModules} /
 * {@code LmsService.reorderLessons}.
 *
 * @param items the full set of sibling items with their new order numbers
 * @author KiteClass Team
 * @since 2.9.0
 */
public record ReorderRequest(
        @NotEmpty(message = "items là bắt buộc")
        List<@Valid @NotNull ReorderItem> items
) {

    /**
     * Single reorder entry.
     *
     * @param id          the module/lesson ID
     * @param orderNumber the new order number (1-based)
     */
    public record ReorderItem(
            @NotNull(message = "id là bắt buộc")
            Long id,

            @NotNull(message = "orderNumber là bắt buộc")
            @Min(value = 1, message = "orderNumber phải >= 1")
            Integer orderNumber
    ) {
    }
}
