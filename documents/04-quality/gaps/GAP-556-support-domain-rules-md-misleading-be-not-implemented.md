# GAP-556: Support domain rules.md misleading — BE chưa implement, chỉ Footer discoverability

**Status:** 🟢 DONE 2026-05-14 (Wave 79 Bucket E — scope clarification shipped across 3 files)
**Priority:** 🟠 P1
**Domain:** Mixed (business docs + BE scope)
**Found:** 2026-05-14 (Business Logic audit post-Wave-78 — Cat 1.1 P0 domain-scoped)
**Affects:** documents/01-business/kitehub/support/{rules,use-cases,api-contract}.md

## Problem

Wave 78 Bucket F ship `documents/01-business/kitehub/support/` với 3 file 3-layer mô tả full support ticket model:
- `POST /api/v1/support-tickets` endpoint
- Table `support_tickets`
- 10 config keys `kitehub.support.*` (subject min/max chars, body min/max, rate limits, categories enum, priorities enum, ticket number prefix, SLA)
- BR-SUPPORT-001 ("In-house route MVP Phase 1") + BR-SUPPORT-002 ("Email required")

**Nhưng Wave 78 chỉ ship Footer discoverability** (mailto: + Help + beta-status links) — không có BE module `support/`, không có migration `support_tickets`, không có Controller.

**Evidence:**
```bash
$ find kitehub/kitehub-subscription/src -type d -name "support"
# 0 results

$ grep -rnE "support_tickets|SupportTicket" kitehub --include="*.java" --include="*.sql"
# 0 results

$ grep "^V" kitehub/kitehub-subscription/src/main/resources/db/migration/ | tail
# V43__create_onboarding_progress_table.sql
# V44__create_feedback_submissions_table.sql
# (no support_tickets migration)
```

`gap-status.csv:380` xác nhận GAP-540 status = `PARTIAL 80%` với note "paid chat widget vendor deferred Wave 79" — nhưng `rules.md` không header-flag scope.

## Root Cause

Bucket 0 strategy "rules first" ship full vision business model. Khi Bucket F downscope sang chỉ discoverability (cost-saving Phase 1), rules.md không sync với reality.

Người đọc rules.md thấy 3 file đầy đủ → tưởng đã ship đầy đủ → confused khi không tìm thấy endpoint hoặc table.

## Proposed Fix

Update `documents/01-business/kitehub/support/rules.md` header thêm scope clarification:

```markdown
# Support Tickets — Business Rules

**Domain:** Support inquiry / ticket submission (Wave 78 — GAP-540)
**Wave 78 scope:** ⚠️ **DISCOVERABILITY ONLY** — Footer mailto: + Help link + beta-status link.
  Full ticket model (POST /api/v1/support-tickets + DB table + admin reply UI) **deferred Wave 79+**.
  See gap-status.csv:GAP-540 PARTIAL 80%.
**Last verified:** 2026-05-14 (Wave 78 Bucket F — discoverability shipped)
```

Same header note ở `use-cases.md` + `api-contract.md` đầu file.

Optionally split rules.md thành 2 sections:
- §"Phase 1 BETA scope (Wave 78 shipped)" — chỉ về email mailto: + Help link
- §"Future scope (Wave 79+)" — ticket model + table + endpoint

## Acceptance Criteria

- [x] `documents/01-business/kitehub/support/rules.md` header có "Wave 78 scope: DISCOVERABILITY ONLY" warning (Wave 79 Bucket E)
- [x] Same warning ở `use-cases.md` + `api-contract.md` (Wave 79 Bucket E)
- [x] BR-SUPPORT-001/002 Code reference fields update từ "(planned)" → "(Wave 80+ — discoverability shipped Wave 78)" (Wave 79 Bucket E — note added inline)
- [x] Reader của 3 file không tưởng nhầm BE đã implement (scope-clarification block prominently displayed)
- [x] Business logic audit Cat 1.1 sub-check cho domain support reflects "deferred scope" thay vì FAIL implementation

## Log

- **2026-05-14 (Wave 79 Bucket E):** Status flipped 🔵 OPEN → 🟢 DONE. Shipped scope clarification block (`⚠️ Wave 78 scope: DISCOVERABILITY ONLY`) tới đầu 3 file: `rules.md`, `use-cases.md`, `api-contract.md`. Block giải thích Wave 78 chỉ ship footer `mailto:support@kitehub.me` discoverability; full BE controller + `support_tickets` table + admin queue UI deferred Wave 80+ (tracked cùng GAP-040). Reviewer: @nguyenvankiet (solo-dev, acting Product Owner + Compliance). Verified per `gap-done-discipline.md` §2 — all 5 AC checked, no banned phrase trong Log entry, no follow-up gap needed (scope clarification IS the fix).

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-14-post-wave-78.md`
- Parent: GAP-540 (PARTIAL 80% — discoverability scope shipped)
- Sister: GAP-555 (config keys not wired — Cat 2.1)
- Rule: `business-logic-review.md` §2.3 Reviewer + scope clarity
