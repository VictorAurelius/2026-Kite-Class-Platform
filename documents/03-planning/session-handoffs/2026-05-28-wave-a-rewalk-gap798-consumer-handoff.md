---
audience: dev
date: 2026-05-28
session-theme: Wave A re-walk + merge 3 fix branches + GAP-798 consumer-side authz bridge
status: complete
next-session-focus: GAP-798b producer side (cross-service) + seed script Sky English Center (steps 1-10) + GAP-537 screenshots
context-at-handoff: ~70% (Opus 1M)
---

# Session handoff — Wave A re-walk + GAP-798 consumer bridge (2026-05-28)

## Đã ship lên main session này

| PR | Nội dung | Gap |
|---|---|---|
| #1946 | kiteclass-core 404/405 handlers (NoHandlerFound/MethodNotSupported) | GAP-796 ✅ DONE |
| #1947 | kitehub-email beta-invite/welcome template var-name reconcile | GAP-797 ✅ DONE |
| #1948 | GAP-795 X-User-Id Long→UUID audit chain (V73) + GAP-798 consumer-side authz bridge | GAP-795 ✅ DONE, GAP-798 🟡 PARTIAL |
| #1949 | kitehub-email JsonNode.fields()→properties() deprecation (CI pending at handoff) | — |

**Gaps flipped:** GAP-795/796/797/787 → DONE (mv `phase-1-beta/closed/`). GAP-798 → PARTIAL (consumer). GAP-798b filed OPEN.

## Re-walk verdict (4 luồng PASS + 2 bug caught)
RST re-walk trên local stack (gateway :9000, kiteclass_shared) Owner `owner.test@test.vn`/`Test@1234` tenant `877dff9d` + Admin `admin@kitehub.com`/`Admin@KiteHub123`:
- GAP-796: nonexistent route→404, wrong method→405 (direct :8088; gateway-level 400 là concern riêng)
- GAP-795: teacher+course created via gateway → DB `created_by` = Owner UUID `b9fa3522...` (không NULL), 0 "Invalid X-User-Id"
- GAP-797: beta approve → MailHog email mã thật `206961` + `/signup/beta?code=206961`
- GAP-787: staff invite → MailHog accept link + token thật
- **Bug #1 (caught by re-walk):** `AssignmentRepository.findByCreatedBy(Long)` → UUID — GAP-795 sweep miss, crash-loop boot. Fixed.

## GAP-798 design CORRECTED (đọc kỹ cho GAP-798b)
Gap file gốc đề xuất `user_id UUID` columns trên domain entities = **SAI/thừa**. Design đúng: **X-User-Reference-Id** header (numeric reference-id cho ownership) + X-User-Id UUID (audit). Consumer side (kiteclass-core đọc header) DONE #1948:
- `UserContext.currentReferenceId` (Long) + `TenantFilterInterceptor` reads `X-User-Reference-Id`
- `AuthorizationBean.hasAccessToChild` → numeric reference-id (hasAccessToClass đã đúng UUID post-V73, không đổi)
- `UserPreferencesController.validateUserAccess` → reference-id compare
- Tests: UserPreferencesControllerTest gửi X-User-Reference-Id; Wave02MigrationsTest created_by bigint→uuid

## ⚠️ GAP-798b = producer side (NEXT SESSION, cross-service, security-sensitive)
**Architectural floor:** `users.reference_id` column **KHÔNG tồn tại** — link auth-user↔domain-row vắng cả 2 chiều. Producer side cần:
1. Migration users.reference_id (subscription)
2. Cross-service population: parent redeem returns parentId / staff-accept mints UUID ở subscription (không phải core) → cần event/internal-API
3. AuthService + TwoFactorController add referenceId JWT claim
4. Gateway JwtAuthenticationGatewayFilter forward X-User-Reference-Id
5. 4 controllers (Storage/Assignment/LessonProgress/Lms) header swap + **integration test sweep** (đã revert khỏi #1948 vì phá 20 test — chúng gửi X-User-Id)
6. Decision: students có login/reference_id không?
Parent flow **fail-closed at runtime** tới khi GAP-798b ship.

## NEXT SESSION — thứ tự
1. **GAP-798b** producer side (clean context, investigate-first, cross-service). Đọc GAP-798b file + GAP-798 corrected design.
2. **Seed script Sky English Center** (Step 3, user chốt: fresh tenant via signup + standard depth). Bước 1-10 build được NGAY (signup→approve→exchange-claim-code→beta-signup→owner login→teachers/courses/classes/students/enrollments). Endpoint map đầy đủ trong session log. **CC0 images (bước 11) block trên GAP-798b** (storage controller cần X-User-Reference-Id). Existing SQL seed `scripts/seed-thesis-demo-tenants.sh` là synthetic tenants (khác mục đích).
3. **GAP-537** screenshots (25%) — feed từ seed script.

## Lưu ý
- Stack local 13 service healthy; kiteclass-core rebuilt với V73 + GAP-798 consumer. Owner tenant 877dff9d có teacher id=2 + course IELTS-RW01 (từ re-walk).
- Worktree husks đã prune đầu session. Branch `wave-a-fixes-rewalk` (local integration, throwaway) + `wave-a-gap798-bridge` (merged #1948) còn local — có thể xóa.
