/**
 * Student-portal read APIs (GAP-269b, Wave 51 Bucket B).
 *
 * <p>Surface for the kc-student production frontend (today schedule, grades,
 * payments, notifications). Identity is carried on
 * {@code X-User-Reference-Id} populated by the Gateway from
 * {@code users.reference_id} when {@code userType = STUDENT} — the same
 * convention used by parent-portal controllers (GAP-321 Phase 1A).
 *
 * <p>Phase 1 v1 ships endpoint contracts + auth scope guard + empty-result
 * responses; full join logic against {@code ClassSchedule} / {@code Assignment}
 * / {@code SubjectGrade} / {@code Invoice} / notification feed follows once
 * the FE consumer (Wave 49 mock fixtures → real fetch swap) lands. The
 * contracts are published now so the FE can wire against stable shapes.
 *
 * @author KiteClass Team
 * @since GAP-269b (Wave 51 Bucket B)
 */
package com.kiteclass.core.module.student.portal;
