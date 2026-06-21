# GAP-1516: kitehub/marketing + kitehub/consent thiếu use-cases.md (3-layer incomplete)

**Status:** 🟢 DONE (2026-06-21 — both use-cases.md written; marketing + consent no longer in 3-layer violation list)
**Priority:** 🟡 P2
**Domain:** Meta (Living Docs 3-layer compliance)
**Found:** 2026-06-21 (Business Logic full audit refresh — `scripts/check-3-layer-completeness.sh`)
**Affects:** `documents/01-business/kitehub/marketing/` + `documents/01-business/kitehub/consent/`

## Problem

CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure" mandate: mỗi domain PHẢI có 3 files (`rules.md` + `use-cases.md` + `api-contract.md`). 2 domain có sẵn nhưng thiếu Layer-2 (`use-cases.md`):

| Domain | Files có | Thiếu | Created |
|---|---|---|---|
| `kitehub/marketing` | README.md + api-contract.md + rules.md | `use-cases.md` | 2026-05-06 (PR #816, BR-PDPL-CONSENT-001..004) |
| `kitehub/consent` | README.md + api-contract.md + rules.md | `use-cases.md` | 2026-05-24 (PR #1782, PDPL consent immutable + hash chain) |

`scripts/check-3-layer-completeness.sh` (2026-06-21) báo 4 violation tổng: 2 cái này CỘNG `kitehub/preferences` + `kitehub/email` (đã thuộc GAP-664). marketing + consent **ngoài scope GAP-664** (GAP-664 file ghi rõ "preferences + email domains") → finding riêng.

Thiếu `use-cases.md` = user-facing scenario (actor, steps, errors, FE behavior) mất khỏi docs; verification chain UC-xxx → endpoint không trace được; future reader không tái dựng được intent từ doc alone. Đặc biệt consent + marketing đều chạm PDPL (L1) — use-cases ghi nhận consent capture/withdraw/version-stamp flow là load-bearing cho compliance evidence.

## Root Cause

2 domain ship rules.md (Layer-1 BR) + api-contract.md (Layer-3 endpoint) nhưng drop use-cases.md (Layer-2). Detector `check-3-layer-completeness.sh` chỉ WARN (non-blocking pre-grace), nên drift tồn tại từ lúc tạo domain mà không bị chặn. Cùng anti-pattern class với GAP-664 (preferences/email) + GAP-1322 (multi-tenancy, đã DONE) + GAP-640 (admin-audit).

## Proposed Fix

Backfill `use-cases.md` cho 2 domain, cùng wave với GAP-664 preferences/email backfill (gộp class):

- `documents/01-business/kitehub/marketing/use-cases.md`:
  - UC-MKT-01: Visitor xem marketing landing/kit theme (consent banner trigger)
  - UC-MKT-02: PDPL cookie/marketing consent capture + version-stamp (link BR-PDPL-CONSENT-001..004)
- `documents/01-business/kitehub/consent/use-cases.md`:
  - UC-CONSENT-01: User cấp consent → ghi time + version + legal basis
  - UC-CONSENT-02: User rút consent → immutable hash-chain append (không xóa)
  - UC-CONSENT-03: Re-consent khi version bump

Grounded trong code consent/marketing hiện có (BR-PDPL-CONSENT-*, hash-chain logic PR #1782).

## Acceptance Criteria

- [x] `kitehub/marketing/use-cases.md` tồn tại với ≥2 UC mapping tới BR-PDPL-CONSENT-* — **5 UC** (banner display/read/revoke + DSAR request/status), BR-PDPL-CONSENT-001..004 + BR-PDPL-DSAR-001..006
- [x] `kitehub/consent/use-cases.md` tồn tại với ≥3 UC (capture/withdraw/re-consent) — **3 UC** (record/view-history/withdraw) immutable v2
- [x] `scripts/check-3-layer-completeness.sh` → marketing + consent không còn trong violation list (verified — chỉ còn 2 domain GAP-664 preferences/email)
- [x] UC mỗi domain trace được tới api-contract.md endpoint hiện có (consent v2 `/api/v1/consent/v2/*`; marketing `/api/v1/consent/*` + `/api/v1/dsar/*`)

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-06-21-business-logic-full-audit.md` (Finding 1)
- GAP-664 (PARTIAL P1) — same class, scope preferences + email; recommend gộp backfill cùng wave
- GAP-1322 (DONE) — multi-tenancy 3-layer backfill = pattern reference
- GAP-666 (OPEN) — business README index sync (sub-folder README cũng thiếu, fold vào đây)
- CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
- `scripts/check-3-layer-completeness.sh` (detector)

## Log

- 2026-06-21 (loop round 3) — 🔵 OPEN → 🟢 DONE. Viết 2 use-cases.md còn thiếu: `kitehub/marketing/use-cases.md` (5 UC — cookie banner display/read/revoke + DSAR request/status, grounded BR-PDPL-CONSENT-001..004 + BR-PDPL-DSAR-001..006 + api-contract endpoints `/api/v1/consent/*` + `/api/v1/dsar/*`) + `kitehub/consent/use-cases.md` (3 UC — immutable v2 record/view-history/withdraw, grounded `/api/v1/consent/v2/*`). `check-3-layer-completeness.sh` xác nhận marketing + consent không còn trong violation list (2 domain còn lại = GAP-664 preferences/email, separate scope). Advances business-logic audit 3-layer Cat (path-to-80 cùng GAP-664 + GAP-156 counsel). Coordinator inline per `agent-concurrency-budget-inline-hybrid` (song song GAP-1251 agent).
