package com.kiteclass.core.module.role.constant;

import lombok.Getter;

import java.util.Optional;

/**
 * The 5 fixed-curated system role templates seeded per tenant (GAP-1119 decision 1).
 *
 * <p>RBAC depth for Phase 1 BETA is <b>fixed-curated</b>: an owner only assigns
 * users to one of these 5 templates — there is NO owner-edit-permission UI (deferred
 * Phase 3). The {@code Role}-hierarchy table remains dynamic-capable; these are the
 * canonical seeded rows {@code RoleSeederService} provisions for each tenant.
 *
 * <p>Note on the two authorization layers:
 * <ul>
 *   <li><b>This template / {@code user_roles} layer</b> = assignment metadata (who has which role).</li>
 *   <li><b>Gateway {@code X-User-Roles} → {@code @PreAuthorize} layer</b> = the actual
 *       request-time enforcement (see {@code GatewayHeaderAuthenticationFilter}).</li>
 * </ul>
 * The {@code level} values (1-10) follow the ADR-003 role-hierarchy convention.
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
@Getter
public enum SystemRoleTemplate {

    /** School owner — full authority within the tenant. */
    OWNER(10, "Chủ trung tâm — toàn quyền trong tenant (course/class, billing, settings, role-assign)"),

    /** Staff — subset of owner per permission bundle (enrollment, attendance, invoice, staff). */
    STAFF(8, "Nhân viên — subset quyền owner (enrollment, attendance, invoice, staff)"),

    /** Teacher — own courses/classes, LMS authoring, attendance, grade entry, completion roster. */
    TEACHER(5, "Giáo viên — lớp/khóa của mình, LMS authoring, điểm danh, nhập điểm, completion roster"),

    /** Parent — read-only child progress/grades/attendance/fees + notifications. */
    PARENT(3, "Phụ huynh — read-only tiến độ/điểm/điểm danh/học phí của con + thông báo"),

    /** Student — learning surface: classes, lesson player, assignments, grades, progress. */
    STUDENT(1, "Học sinh — học tập: lớp, bài học, bài tập, điểm, tiến độ, điểm danh");

    private final int level;
    private final String description;

    SystemRoleTemplate(int level, String description) {
        this.level = level;
        this.description = description;
    }

    /**
     * Resolves a template by its name, case-sensitive.
     *
     * @param name the role template name (e.g. "TEACHER")
     * @return the matching template, or empty if {@code name} is not one of the 5 templates
     */
    public static Optional<SystemRoleTemplate> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (SystemRoleTemplate template : values()) {
            if (template.name().equals(name)) {
                return Optional.of(template);
            }
        }
        return Optional.empty();
    }
}
