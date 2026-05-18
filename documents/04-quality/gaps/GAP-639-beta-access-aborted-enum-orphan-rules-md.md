# GAP-639: ABORTED enum orphan trong beta-access/rules.md

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (Living Docs sync)
**Detected:** 2026-05-18 (Wave 92 post-wave audit suite per GAP-619)
**Related Audits:** [documents/04-quality/audits/business-logic/2026-05-18-wave-92-business-logic-audit.md](../audits/business-logic/2026-05-18-wave-92-business-logic-audit.md)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|---|---|---|
| `BetaAccessRequestStatus.ABORTED` enum | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/BetaAccessRequestStatus.java` | ✅ shipped Wave 92 Bucket C |
| `BetaRequestAbortCleanupScheduler` | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/BetaRequestAbortCleanupScheduler.java` | ✅ shipped Wave 92 Bucket C |
| BR-BETA-004 entry trong rules.md | `documents/01-business/kitehub/beta-access/rules.md` | ❌ missing — không có entry mô tả ABORTED |
| BR-BETA-002 Rationale mention ABORTED | `documents/01-business/kitehub/beta-access/rules.md:25` | 🟡 partial — chỉ liệt kê `REJECTED/EXPIRED`, thiếu `ABORTED` |
| use-cases.md ABORTED state transitions | `documents/01-business/kitehub/beta-access/use-cases.md` | ❌ missing — chưa document transition path |

**Grep commands run:**

```bash
grep -n "ABORTED" documents/01-business/kitehub/beta-access/
grep -n "BR-BETA-004" documents/01-business/kitehub/beta-access/rules.md
grep -rn "BetaAccessRequestStatus" kitehub/kitehub-subscription/src/main/java/
```

## Problem

Audit Business Logic Wave 92 (2026-05-18) phát hiện finding P1-1: code shipped Wave 92 Bucket C (Status `ABORTED` terminal + scheduler dispatcher cleanup stale requests) nhưng `documents/01-business/kitehub/beta-access/rules.md` không update cùng PR. Cụ thể:

1. **No BR-BETA-004 entry:** rules.md liệt kê BR-BETA-001/002/003 nhưng thiếu BR-BETA-004 mô tả ABORTED terminal status (trigger conditions, retention, audit log requirements)

2. **BR-BETA-002 Rationale orphan:** line 25 chỉ ghi "Status transitions: PENDING → APPROVED / REJECTED / EXPIRED" — thiếu `ABORTED` trong terminal status list. Reader đọc rules.md sẽ không biết ABORTED tồn tại.

3. **use-cases.md không sync:** Chưa document trigger conditions cho ABORTED (e.g., scheduler dispatch sau N ngày stale) + audit log evidence trail (PDPL Art 11 angle).

Vi phạm CLAUDE.md §"CRITICAL: Living Documents" mandate "đổi logic = đổi doc trong cùng PR".

## Context

Wave 92 Bucket C shipped:
- Enum addition `BetaAccessRequestStatus.ABORTED`
- Scheduler `BetaRequestAbortCleanupScheduler` với `@Scheduled` cron transitioning stale `PENDING` requests sang `ABORTED`
- Admin audit log emit cho mỗi transition (per PDPL Art 11 evidence trail)

Nhưng business docs không sync — code-vs-docs drift xảy ra ngay tại merge time.

## Proposed Fix

### Step 1: Add BR-BETA-004 entry

Thêm vào `documents/01-business/kitehub/beta-access/rules.md`:

```markdown
### BR-BETA-004 — Stale beta request auto-abort

Trigger: scheduler `BetaRequestAbortCleanupScheduler` (cron `0 0 2 * * ?`)
Source state: PENDING
Target state: ABORTED (terminal)
Condition: `created_at` < NOW() - `kitehub.beta-access.abort-threshold-days` (default 30 ngày)
Audit: emit admin_audit_log entry per transition (action `BETA_ABORT`)
Config key: `kitehub.beta-access.abort-threshold-days`
```

### Step 2: Update BR-BETA-002 Rationale

Sửa line 25 của rules.md:

```diff
- Status transitions: PENDING → APPROVED / REJECTED / EXPIRED
+ Status transitions: PENDING → APPROVED / REJECTED / EXPIRED / ABORTED (terminal)
```

### Step 3: Update use-cases.md

Thêm use-case `UC-BETA-006: Stale request auto-abort` trong `documents/01-business/kitehub/beta-access/use-cases.md` với actor (Scheduler), preconditions, steps, error paths.

## Acceptance Criteria

- [ ] BR-BETA-004 entry tồn tại trong rules.md với complete fields (trigger, source, target, condition, audit, config key)
- [ ] BR-BETA-002 Rationale line 25 update mention ABORTED trong terminal status list
- [ ] use-cases.md có UC-BETA-006 cover scheduler-driven transitions
- [ ] api-contract.md sync if admin endpoint expose ABORTED status filter
- [ ] Pre-handoff self-test verify reader đọc rules.md hiểu ABORTED purpose + lifecycle

## Related

- **Audit origin:** [documents/04-quality/audits/business-logic/2026-05-18-wave-92-business-logic-audit.md](../audits/business-logic/2026-05-18-wave-92-business-logic-audit.md)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** [GAP-619](GAP-619-wave-92-post-wave-audit-suite.md)
- **Sister gap:** [GAP-644](GAP-644-beta-request-abort-scheduler-drift-metric.md) (same scheduler, drift metric concern)
- **Rules:**
  - CLAUDE.md §"CRITICAL: Living Documents"
  - CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
  - `.claude/rules/contract-first-for-cross-layer.md`

## Log

- **2026-05-18** — Initial write-up. Filed từ Wave 92 post-wave audit suite (GAP-619) Business Logic audit finding P1-1. State-check confirmed `grep -n "ABORTED" documents/01-business/kitehub/beta-access/` returns 0 hits — code shipped Wave 92 Bucket C nhưng rules.md không update. Living Docs mandate vi phạm — gap closure yêu cầu sync 3 files (rules.md, use-cases.md, api-contract.md if applicable).
