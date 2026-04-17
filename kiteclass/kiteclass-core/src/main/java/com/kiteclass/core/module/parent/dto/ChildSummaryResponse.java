package com.kiteclass.core.module.parent.dto;

/**
 * Minimal summary of a linked child for the parent dashboard MVP.
 *
 * <p>Grade / class enrichment is intentionally left nullable — Wave 2 only
 * wires the relationship; Wave 5 will join to {@code classes} /
 * {@code subject_grades} to populate these fields.
 *
 * @param studentId student id
 * @param studentName display name
 * @param className  current class name (nullable until Wave 5)
 * @param grade      current grade level (nullable until Wave 5)
 * @param linkType   PRIMARY / SECONDARY for this parent-child edge
 * @since 2.14.0
 */
public record ChildSummaryResponse(
        Long studentId,
        String studentName,
        String className,
        String grade,
        String linkType
) {
}
