package com.kiteclass.core.module.k12.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Curriculum (Chương trình học) — định nghĩa danh sách môn học cho mỗi grade.
 *
 * <p>Example (VN THPT grade 10):
 * <pre>
 * Toán: 4 tiết/tuần, weight 1.5
 * Ngữ văn: 4 tiết/tuần, weight 1.5
 * Tiếng Anh: 3 tiết/tuần, weight 1.0
 * Vật lý: 2 tiết/tuần, weight 1.0
 * ... (12 môn total)
 * </pre>
 *
 * <p>Stored as JSONB for flexibility. Each entry: courseId → { weeklyHours, weight }.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-CUR-001: Unique grade per tenant</li>
 *   <li>BR-CUR-002: Contains 1+ subjects</li>
 *   <li>BR-CUR-003: Weights sum used for weighted average grade calculation</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-054)
 */
@Entity
@Table(
        name = "curricula",
        indexes = {
                @Index(name = "idx_curriculum_grade", columnList = "instance_id,grade", unique = true),
                @Index(name = "idx_curriculum_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curriculum extends BaseEntity {

    @Column(name = "grade", nullable = false, length = 10)
    private String grade;

    @Column(name = "name", length = 100)
    private String name;

    /**
     * JSONB map: courseId (String) → SubjectSpec.
     * Example: { "12": { "weeklyHours": 4, "weight": 1.5 } }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subjects", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, SubjectSpec> subjects = new HashMap<>();

    /**
     * Nested spec for each subject.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectSpec {
        private Integer weeklyHours;
        private Double weight;
    }
}
