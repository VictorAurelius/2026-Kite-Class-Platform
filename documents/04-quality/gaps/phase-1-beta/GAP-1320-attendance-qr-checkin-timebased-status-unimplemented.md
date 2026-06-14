# GAP-1320: Attendance QR check-in + time-based auto-status documented-but-unimplemented

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend (KiteClass attendance) + Docs
**Found:** 2026-06-14 (Business Logic full audit post wave-p0-closeout-1)
**Affects:** `documents/01-business/kiteclass/attendance/rules.md` §1 + §4 + `use-cases.md` UC-ATT-03 / `kiteclass/kiteclass-core/.../module/attendance` / `application*.yml`

## Problem

`attendance/rules.md` + `use-cases.md` mô tả **QR check-in + auto-status theo thời gian** như feature đã ship, nhưng code KHÔNG implement:

1. **BR-ATT-002** (status by check-in time), **BR-ATT-003** (grace 5 phút), **BR-ATT-004** (late threshold 15 phút) — `grep -rnE "BR-ATT-00[234]" --include="*.java" kiteclass/` → **0 hits** (so với BR-ATT-005/008 đều có ref).
2. **UC-ATT-03 "QR Code Check-in"** (`use-cases.md:58-83`) mô tả flow đầy đủ: scan QR → record timestamp → auto-status (≤5min PRESENT / ≤15min LATE / >15min ABSENT) → 400 "QR code expired". KHÔNG đánh dấu "(Planned)" (khác §3 Emails đã ghi rõ "Planned").
3. **5 config key** cited trong `rules.md` §4 — `attendance.grace-period-minutes`(5) / `attendance.late-threshold-minutes`(15) / `attendance.qr-code.expiry` / `attendance.low-warning-threshold`(70%) / `attendance.grade-weight`(10%) — `grep -rnE "..." --include="*.yml" kiteclass/` → **0 hits** (KHÔNG tồn tại trong bất kỳ application*.yml).
4. State-check code: `grep -rniE "qr.?code|checkin|grace.?period|determineStatus" --include="*.java" .../module/attendance` → 0 hits (QR hits chỉ ở payment module; grace hits chỉ ở storage/retention). `AttendanceServiceImpl` chỉ hỗ trợ **manual status assignment** + stats counting.

Impact: code↔rules drift. Tenant đọc rules.md/use-cases.md kỳ vọng QR check-in + auto-status; thực tế chỉ có manual marking (UC-ATT-01/02). Rubric §2.2 Cat 2.1 (config key cited phải tồn tại) + Cat 1.1 (BR-xxx phải có grep hit) + Cat 3.1 (UC error-path "QR expired 400" không có test) đều FAIL.

## Root Cause

Attendance domain ship manual-marking path trước (Wave 18b2 + GAP-993/994); QR/time-based auto-status là scope sau, được document trong rules/use-cases như intent nhưng chưa build. rules.md §4 config block + UC-ATT-03 không gắn "(Planned)" marker → đọc như đã ship.

## Proposed Fix

Chọn MỘT trong hai (per `pre-handoff-self-test-completeness.md` doc-drift resolution):

- **Option A (implement):** thêm QR session endpoint + check-in timestamp → time-based status determination (`AttendanceStatus` từ check-in vs session-start) + 5 config key vào `application.yml` + tests cho UC-ATT-03 error path (400 QR expired). Lớn — wave riêng.
- **Option B (mark Planned — recommended cho gate):** đánh dấu `(Planned Phase 1.5)` trên BR-ATT-002/003/004 + UC-ATT-03 + §4 config block (mirror pattern §3 Emails "Planned"). Đưa rules.md về đúng reality → Cat 2 phục hồi +4.

## Acceptance Criteria

- [x] rules.md §4 config keys + BR-ATT-002/003/004 + UC-ATT-03 EITHER implemented (Option A) OR marked `(Planned Phase 1.5)` (Option B). → **Option B applied** (doc-honesty).
- [x] Business Logic audit refresh next: Cat 2.1 không còn cited-but-absent config key → config keys giờ ghi rõ "Planned — Phase 1.5", không còn cited-as-shipped.

## Resolution

**PARTIAL** 2026-06-15 (PR audit-fixD-bizdocs) — **Option B (doc-honesty) DONE; Option A (feature build) deferred.**

Doc-honesty (in scope, docs-only PR) — DONE:
- `attendance/rules.md`: BR-ATT-002/003/004 đánh dấu `(Planned — Phase 1.5)` + ghi rõ "config key not yet wired"; §2 "Check-in Flow (QR Code)" header + blockquote đánh dấu Planned/not-implemented; §4 Config table thêm cột Status = "Planned — Phase 1.5" cho cả 5 key + blockquote "NOT present in any application*.yml".
- `attendance/use-cases.md`: UC-ATT-03 header + blockquote đánh dấu `(Planned — Phase 1.5, not yet implemented)`.
- Verified accurate trước khi mark: `grep` → BR-ATT-002/003/004 = 0 code hit; 5 config key = 0 yml hit; 0 QR/checkin/determineStatus trong `module/attendance`.

Feature build (Option A — QR session endpoint + time-based status determination + 5 config key trong yml + UC-ATT-03 error-path test) — **DEFERRED to a future feature wave** (per gap §Proposed Fix Option A "Lớn — wave riêng"; out of scope cho docs-only audit-fix PR). Gap giữ PARTIAL làm tracker cho phần feature; sẽ flip DONE khi feature ship HOẶC re-scope thành feature gap riêng nếu được prioritize.

## Related

- **Parent audit:** `documents/04-quality/audits/business-logic/2026-06-14-business-logic-full-audit.md` (Finding 1)
- **Rule:** rubric `audit-skill-rubric-business-logic-audit.md` §2.1/§2.2 + CLAUDE.md §"3-Layer Structure"
- **Sibling:** GAP-1321 (MAKEUP Layer-1), GAP-666 (BR-ID traceability)
