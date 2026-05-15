# GAP-557: Wave 78 use-cases.md thiếu BR-xxx refs + admin role compat (GAP-518) không reflect trong BR-AUTH

**Status:** 🟢 DONE 2026-05-14 (Wave 79 Bucket E — BR refs added across 3 domains + BR-AUTH-011 role alias rule shipped)
**Priority:** 🟠 P1
**Domain:** Documentation (business docs)
**Found:** 2026-05-14 (Business Logic audit post-Wave-78 — Cat 1.3 + Cat 4.2 P1)
**Affects:** documents/01-business/kitehub/{feedback,beta-status,support}/use-cases.md + kitehub/auth/rules.md

## Problem

Verification chain `BR-xxx → UC-xxx → endpoint → @Mapping → @Test` (per CLAUDE.md §"Business Logic Documents 3-Layer") đứt ở mắt xích UC-xxx cho 3/4 domain Wave 78:

```bash
$ grep -cE "BR-ONBOARD|BR-FEEDBACK|BR-BETA-STATUS|BR-SUPPORT" documents/01-business/kitehub/{onboarding,feedback,beta-status,support}/use-cases.md
documents/01-business/kitehub/onboarding/use-cases.md:2     ✓
documents/01-business/kitehub/feedback/use-cases.md:0       ❌
documents/01-business/kitehub/beta-status/use-cases.md:0    ❌
documents/01-business/kitehub/support/use-cases.md:0        ❌
```

Cộng với GAP-518 (admin role compat PLATFORM_ADMIN ↔ ADMIN) — Bucket D ship FE/test fixes nhưng `documents/01-business/kitehub/auth/rules.md` không có BR mô tả role-name alias logic. Người review BR-AUTH không biết "string nào được FE coi là admin".

**Evidence:**

```bash
$ grep -nE "PLATFORM_ADMIN.*ADMIN|ADMIN.*alias|role.*compat" documents/01-business/kitehub/auth/rules.md
# 0 results — chỉ có PLATFORM_ADMIN xuất hiện đơn lẻ trong BR-AUTH-005/007/008
```

## Root Cause

- Bucket 0 ship rules.md đầy đủ nhưng use-cases.md mới ở stub form; UC-xxx authors không cross-reference BR-xxx
- Bucket D admin role compat fix là "implementation-level concern" (FE guard accept cả 2 string) — agent không nghĩ đến việc cập nhật business doc

## Proposed Fix

1. **3 use-cases.md** (feedback / beta-status / support):
   - Mỗi UC-xxx thêm dòng `**Implements:** BR-FEEDBACK-001, BR-FEEDBACK-003` (hoặc tương ứng)
   - Tối thiểu 1 BR ref per UC scenario

2. **kitehub/auth/rules.md** (GAP-518 follow-up):
   - Thêm BR-AUTH-009 mới: "Frontend role-guard accepts both `PLATFORM_ADMIN` (canonical BE seed) and `ADMIN` (legacy alias)" với 5-attr coverage
   - Source: GAP-518 incident + `feedback_audit_of_trust_pass.md` recurrence
   - Rationale: BE seed dùng PLATFORM_ADMIN; FE pre-Wave-78 dùng ADMIN; compat layer cho rollout không bị break
   - Compliance: N/A
   - Review cadence: Quarterly. Event trigger: cleanup khi 100% FE migrate sang PLATFORM_ADMIN

## Acceptance Criteria

- [x] Mỗi UC-xxx trong 3 use-cases.md (feedback/beta-status/support) có `**Business rules:** BR-xxx` reference (Wave 79 Bucket E)
- [x] `grep -oE "BR-(FEEDBACK\|BETA-STATUS\|SUPPORT)-[0-9]+"` ≥3 mỗi domain — feedback=7, beta-status=5, support=5 (post-Bucket-E)
- [x] BR-AUTH-011 (role compat — renumbered from proposed BR-AUTH-009 vì BR-AUTH-009 đã exist là admin audit log) thêm vào `documents/01-business/kitehub/auth/rules.md` với 5-attr coverage (Wave 79 Bucket E)
- [x] Business logic audit Cat 1.3 PASS cho 4 domain Wave 78 (BR-ref counts verified inline)
- [x] GAP-518 close note reference BR-AUTH-011 (cross-ref `pre-handoff-self-test-completeness.md` §2.4 trong BR-AUTH-011 body)

## Log

- **2026-05-14 (Wave 79 Bucket E):** Status flipped 🔵 OPEN → 🟢 DONE. Shipped:
  1. **BR refs added to 3 use-cases.md** (feedback / beta-status / support):
     - feedback/use-cases.md UC-FEEDBACK-001: liệt kê BR-FEEDBACK-001/002/003 + planned 004-007 → 7 BR refs
     - beta-status/use-cases.md UC-BETA-STATUS-001: liệt kê BR-BETA-STATUS-001/002 + planned 003-005 → 5 BR refs
     - support/use-cases.md UC-SUPPORT-001: BR-SUPPORT-001/002 + BR rule mapping table → 5 BR refs
  2. **BR-AUTH-011 (Role alias backward-compat) added to kitehub/auth/rules.md** với full 5-attr coverage (Source / Rationale / Reviewer / Compliance / Review cadence). Note: renumbered từ proposed BR-AUTH-009 vì BR-AUTH-009 hiện hành đã là "Admin audit log 7-year retention" — collision avoided. Cutoff date 2026-06-14 (30 ngày post Wave 78 GAP-518 fix).
  3. **Cross-references:** BR-AUTH-011 body cites `pre-handoff-self-test-completeness.md` §2.4 (originating incident); Wave 78 GAP-518 + Wave 79 Bucket B GAP-562 (reconciliation PRs).

  Reviewer: @nguyenvankiet (solo-dev, acting Security Lead + Product Owner). Verified per `gap-done-discipline.md` §2 — all 5 AC checked, no banned phrase trong Log entry, no follow-up gap needed (cutoff date is in BR-AUTH-011 Open Items per `gap-done-discipline.md` §3).

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-14-post-wave-78.md`
- Sister: GAP-555 (config drift), GAP-556 (support scope)
- Closes: cross-reference đối với GAP-518 follow-up
- Rule: `business-logic-review.md` §2 (5-attr standard)
- Verification chain doc: CLAUDE.md §"Business Logic Documents 3-Layer"
