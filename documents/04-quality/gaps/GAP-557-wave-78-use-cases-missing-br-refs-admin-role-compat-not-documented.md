# GAP-557: Wave 78 use-cases.md thiếu BR-xxx refs + admin role compat (GAP-518) không reflect trong BR-AUTH

**Status:** 🔵 OPEN
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

- [ ] Mỗi UC-xxx trong 3 use-cases.md (feedback/beta-status/support) có `**Implements:** BR-xxx` reference
- [ ] `grep -cE "BR-(FEEDBACK\|BETA-STATUS\|SUPPORT)" documents/01-business/kitehub/{feedback,beta-status,support}/use-cases.md` ≥3 mỗi domain
- [ ] BR-AUTH-009 (role compat) thêm vào `documents/01-business/kitehub/auth/rules.md` với 5-attr coverage
- [ ] Business logic audit Cat 1.3 PASS cho 4 domain Wave 78
- [ ] GAP-518 close note reference BR-AUTH-009

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-14-post-wave-78.md`
- Sister: GAP-555 (config drift), GAP-556 (support scope)
- Closes: cross-reference đối với GAP-518 follow-up
- Rule: `business-logic-review.md` §2 (5-attr standard)
- Verification chain doc: CLAUDE.md §"Business Logic Documents 3-Layer"
