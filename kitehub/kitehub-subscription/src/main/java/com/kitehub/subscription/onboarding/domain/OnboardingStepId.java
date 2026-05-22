package com.kitehub.subscription.onboarding.domain;

/**
 * Whitelisted Day-1 onboarding step identifiers (Wave 78 GAP-538; reordered Wave 105 Bucket B).
 *
 * <p>Schema source-of-truth:
 * {@code documents/01-business/kitehub/onboarding/api-contract.md}.</p>
 *
 * <p>5 hardcoded steps for Phase 1 BETA. Client MUST send {@code stepId} that
 * matches one of these enum values; any other value yields HTTP 400
 * {@code ONBOARDING_INVALID_STEP_ID}.</p>
 *
 * <p>Step order rationale (Wave 105 Bucket B persona walk per
 * {@code documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md}):
 * chị Hằng (P2 Center Owner, 160 học viên sẵn có) wave-in với data có sẵn từ
 * Misa/Excel. Bulk-import-first PHẢI đứng TRƯỚC create-first-class — Owner sẽ
 * không tạo class trống rồi enroll thủ công 160 lượt. Order = PROFILE → INVITE
 * → IMPORT_DATA (bulk-import OR demo seed) → CREATE_FIRST_CLASS → EXPLORE.</p>
 *
 * <p>Step semantics:</p>
 * <ul>
 *   <li>{@link #PROFILE_SETUP} — tenant logo + name + persona confirmed.</li>
 *   <li>{@link #INVITE_TEAM} — add ≥1 other user (GVCN / Quản lý), hoặc skip.</li>
 *   <li>{@link #IMPORT_DATA} — dual-mode (Wave 105 Bucket B):
 *       <ol>
 *         <li><b>Owner real-data path:</b> bulk-import xlsx qua KiteClass core
 *         endpoint {@code POST /api/v1/students/bulk-import/commit} (per
 *         {@code BulkImportController} — GAP-051). Cap 200/batch, async job.</li>
 *         <li><b>Solo / curious path:</b> opt-in sample demo seed gated bởi
 *         {@code tenant.metadata.is_beta_demo_data} flag.</li>
 *       </ol>
 *       FE checklist hiển thị 2 CTA tách biệt: "Nhập danh sách học viên (xlsx)"
 *       VÀ "Bật dữ liệu mẫu" — Owner pick path phù hợp.</li>
 *   <li>{@link #CREATE_FIRST_CLASS} — tạo lớp đầu tiên SAU KHI đã có students
 *       sẵn (Hằng) hoặc demo data (Solo). Eliminate orphan empty-class state.</li>
 *   <li>{@link #EXPLORE_FEATURES} — tour ngắn các tính năng chính hoặc skip.</li>
 * </ul>
 *
 * @since Wave 78 — GAP-538
 * @since Wave 105 — Bucket B (Owner persona reorder + dual-mode IMPORT_DATA semantics)
 */
public enum OnboardingStepId {
    PROFILE_SETUP,
    INVITE_TEAM,
    IMPORT_DATA,
    CREATE_FIRST_CLASS,
    EXPLORE_FEATURES
}
