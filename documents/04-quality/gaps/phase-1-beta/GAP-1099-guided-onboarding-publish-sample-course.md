---
audience: dev
---

# GAP-1099: Guided onboarding — tạo + đăng 1 khóa học mẫu/thử (KH onboarding → KC course publish)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (KH onboarding + KC course)
**Found:** 2026-06-09 (discussion §3a AI branding wizard — user hỏi "có hướng dẫn đăng khóa học thử trong quy trình chưa")
**Affects:** `kitehub-frontend` onboarding checklist + `kiteclass-frontend` course create/publish + `kiteclass-core` course-class

## Problem

Quy trình onboarding (`kitehub/onboarding/rules.md` — 5 step: `PROFILE_SETUP → INVITE_TEAM → IMPORT_DATA → CREATE_FIRST_CLASS → EXPLORE_FEATURES`) **chưa có bước guided end-to-end để chủ trung tâm tự tạo + đăng (publish) 1 khóa học mẫu/thử** và thấy sản phẩm chạy thật.

Hiện chỉ có các mảnh rời rạc:
- `CREATE_FIRST_CLASS` = checklist tracking "tạo lớp" (không phải đăng khóa học; UI thật ở KiteClass).
- `IMPORT_DATA` Path B sample-seed (GAP-538/950 DONE) = **data mẫu** seed sẵn (1 GV + 1 lớp + 3 HS), không phải tutorial dạy người dùng tự đăng.
- KiteClass `UC-CRS-03 Publish Course` (DRAFT→PUBLISHED, validate name/level/duration) = **feature đứng độc lập**, KHÔNG wire thành bước guided trong onboarding.

Boundary (per `kitehub-kiteclass-boundary`): onboarding checklist = **KiteHub** (`kitehub-frontend`, :3001); publish khóa học = **KiteClass** (`kiteclass-core` course-class + `kiteclass-frontend`, :3000). Một luồng guided phải nối KH onboarding → deep-link sang KC course create/publish.

Hệ quả: beta tenant (P2 center owner) khó tự trải nghiệm "đăng khóa học → enroll → thấy sản phẩm chạy" trong lần đầu → activation thấp.

## Proposed Fix

Thêm guided onboarding step "Đăng khóa học mẫu" deep-link từ KH onboarding checklist sang KC course create/publish (DRAFT→PUBLISHED), với khóa mẫu có sẵn template/giá gợi ý. Chi tiết design (in-product walkthrough vs deep-link vs sample-course template) để lại fix PR — gap này là filing, chưa design.

## Acceptance Criteria

- [ ] Onboarding có 1 bước guided dẫn chủ trung tâm tạo + publish 1 khóa học (end-to-end, không chỉ tạo lớp).
- [ ] Bước hoàn tất khi có ≥1 course status=PUBLISHED trong tenant (wire vào checklist completion).
- [ ] Walkthrough hoạt động xuyên KH onboarding → KC course publish (boundary-aware, JWT/tenant context giữ đúng).

## Related

- Discovered in: discussion session 2026-06-09 (branch `feature/tier-ui-fix-g2-browser-2026-06-09`)
- Design source: `documents/01-business/kitehub/onboarding/rules.md` (5-step checklist) + `kiteclass/course-class/use-cases.md` UC-CRS-03
- Sister gaps: GAP-288 (onboarding tour PARTIAL), GAP-280 (onboarding wizard kit OPEN), GAP-538 (sample-data seed DONE)
- Rule: `kitehub-kiteclass-boundary` (KH onboarding ↔ KC course publish)
