package com.kiteclass.core.module.parent.entity;

import com.kiteclass.core.common.constant.ParentLinkType;
import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Join entity materialising the many-to-many between {@link Parent} and
 * {@link Student}.
 *
 * <p>Carries per-edge metadata ({@link #linkType}) rather than being a plain
 * join table, so that "notify PRIMARY parent only" semantics can be
 * implemented without schema changes later. A unique constraint on
 * {@code (parent_id, student_id)} prevents duplicate edges.
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
@Entity
@Table(
        name = "parent_student_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_parent_student",
                        columnNames = {"parent_id", "student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_psl_parent", columnList = "parent_id"),
                @Index(name = "idx_psl_student", columnList = "student_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentStudentLink extends BaseEntity {

    /** Parent side of the link. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    /** Student side of the link. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Whether this parent is the PRIMARY contact (default when created from an
     * invitation) or a SECONDARY contact (additional parent added later).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    @Builder.Default
    private ParentLinkType linkType = ParentLinkType.PRIMARY;

    /**
     * PDPL Decree 13/2023 Art 16 granular consent (Wave 19 — GAP-321c Phase 1C).
     *
     * <p>Per-field-per-parent visibility flag bag + version + updated-at
     * timestamp. Default value comes from V56 migration
     * (<code>{"fields":{}, "version":1, "updatedAt":null}</code>); existing
     * Wave 2 rows migrate without manual fill.
     *
     * <p>Both annotations are required for Hibernate to bind the JSON via
     * the JSONB driver (per
     * <code>feedback_jpa_jsonb_jdbctypecode.md</code>).
     *
     * <p>Read by {@link com.kiteclass.core.module.parent.service.ConsentService}
     * before facet APIs return data; updated through
     * {@code PUT /api/v1/parent/consent}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parental_consent", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private ParentalConsent parentalConsent = ParentalConsent.defaultValue();
}
