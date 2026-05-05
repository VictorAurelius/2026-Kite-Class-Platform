package com.kiteclass.core.module.k12.enums;

/**
 * SubjectGrade approval lifecycle — Tổ trưởng workflow per TT 22/2021.
 *
 * <p>Three-state machine:
 * <ul>
 *   <li>{@link #DRAFT} — GV bộ môn enters score; editable</li>
 *   <li>{@link #REVIEWED} — Tổ trưởng reviews; locked from GV edits</li>
 *   <li>{@link #PUBLISHED} — Hiệu trưởng publishes; permanent in học bạ</li>
 * </ul>
 *
 * <p>Phase 1C v1 (this wave) introduces the enum + persistence column only.
 * Full state machine enforcement (transition validation, Tổ trưởng workflow,
 * notification, audit log) deferred to Phase 1C remainder follow-up gap per
 * {@code gap-done-discipline.md} §3 PARTIAL exit ramp.
 *
 * <p>Reference: BR-GRADEBOOK-003 trong
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}.
 *
 * @since 5.x (Wave 19 Bucket B — GAP-323c Phase 1C v1)
 */
public enum SubjectGradeStatus {

    /** GV bộ môn entry, editable. */
    DRAFT,

    /** Tổ trưởng reviewed, GV edits locked. */
    REVIEWED,

    /** Hiệu trưởng published, permanent in học bạ. */
    PUBLISHED
}
