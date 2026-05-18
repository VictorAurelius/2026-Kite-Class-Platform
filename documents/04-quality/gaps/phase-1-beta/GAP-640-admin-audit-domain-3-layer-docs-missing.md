# GAP-640: Admin audit domain 3-layer docs missing

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META P1 force-multiplier per `.claude/rules/meta-gap-priority.md` §3)
**Domain:** Meta (Business docs structure)
**Detected:** 2026-05-18 (Wave 92 post-wave audit suite per GAP-619)
**Related Audits:** [documents/04-quality/audits/business-logic/2026-05-18-wave-92-business-logic-audit.md](../audits/business-logic/2026-05-18-wave-92-business-logic-audit.md)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|---|---|---|
| V54 migration enrichment `admin_audit_log` 5 columns | `kitehub/kitehub-admin/src/main/resources/db/migration/V54__...sql` | ✅ shipped Wave 92 Bucket A |
| `admin_audit_log` entity + repository | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/` | ✅ shipped Wave 72a + Wave 92 enriched |
| `documents/01-business/kitehub/admin-audit/rules.md` | `documents/01-business/kitehub/admin-audit/` | ❌ missing — folder không tồn tại |
| `documents/01-business/kitehub/admin-audit/use-cases.md` | `documents/01-business/kitehub/admin-audit/` | ❌ missing |
| `documents/01-business/kitehub/admin-audit/api-contract.md` | `documents/01-business/kitehub/admin-audit/` | ❌ missing |

**Grep commands run:**

```bash
ls documents/01-business/kitehub/ | grep -i audit
find documents/01-business -type d -iname "*audit*"
grep -rn "admin_audit_log\|admin-audit" documents/01-business/
```

## Problem

Audit Business Logic Wave 92 (2026-05-18) phát hiện finding META P1-2: domain `admin-audit` đã có code production-grade (Wave 72a `admin_audit_log` table + Wave 92 Bucket A V54 enrichment 5 columns: `request_id`, `target_resource_type`, `target_resource_id`, `before_state` JSONB, `after_state` JSONB) NHƯNG **0 documentation** trong `documents/01-business/kitehub/admin-audit/`. Folder không tồn tại.

Vi phạm CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure" mandate:

> Mỗi domain = 1 folder, 3 files (rules.md + use-cases.md + api-contract.md)

Code shipped Wave 72a (audit log table + write path) + Wave 92 Bucket A (enrichment 5 fields) orphan từ business docs ≥3 waves. PDPL Article 11 evidence-trail compliance mandate yêu cầu "Compliance" field documented trong rules.md cho mỗi business rule điều hành audit log behavior.

## Context

`admin_audit_log` table là **immutable audit trail** cho mọi admin action (PDPL Art 11 + Phase 1 BETA compliance gate). V54 enrichment thêm 5 columns để capture full context:
- `request_id`: trace correlation
- `target_resource_type` + `target_resource_id`: resource being modified
- `before_state` + `after_state` JSONB: diff snapshot for forensic replay

Mỗi field này cần BR entry mô tả purpose + compliance angle + retention policy. Hiện tại reader không biết admin-audit domain tồn tại trong business layer.

META P1 force-multiplier: fix 3-layer docs scaffold 1 lần → mọi future enhancement của domain auto-comply mandate, không cần retroactive Living Docs sync.

## Proposed Fix

### Step 1: Create folder + 3 files

```bash
mkdir -p documents/01-business/kitehub/admin-audit/
```

### Step 2: Draft rules.md

Per `documents/01-business/_TEMPLATE/rules.md` template + apply `.claude/rules/business-logic-review.md` §2 attributes:

```markdown
# Admin Audit — Business Rules

## BR-ADMIN-AUDIT-001 — Immutable audit log
Trigger: every admin action (login, role grant, instance create, payment refund, etc.)
Field: `admin_audit_log` table
Compliance: PDPL Art 11 (evidence trail mandate), ISO27001 A.12.4
Retention: 7 năm minimum
Immutability: V54 migration enforce trigger BEFORE UPDATE → RAISE EXCEPTION
Config key: `kitehub.admin-audit.retention-years` (default 7)

## BR-ADMIN-AUDIT-002 — Required fields per action
...

## BR-ADMIN-AUDIT-003 — JSONB before/after state snapshot
...
```

### Step 3: Draft use-cases.md

Cover top 5 actor flows:
- UC-ADMIN-AUDIT-001: Admin approves beta request → audit log emit
- UC-ADMIN-AUDIT-002: Admin issues refund → audit log với before_state/after_state diff
- UC-ADMIN-AUDIT-003: Auditor queries log by date range + actor filter
- UC-ADMIN-AUDIT-004: Compliance officer exports log cho regulator
- UC-ADMIN-AUDIT-005: System scheduler emit log cho automated transition (e.g., BetaRequestAbortCleanupScheduler)

### Step 4: Draft api-contract.md

Document admin audit query endpoints (if exposed) + write path conventions (which services emit, expected payload shape).

## Acceptance Criteria

- [ ] Folder `documents/01-business/kitehub/admin-audit/` exists với 3 files
- [ ] rules.md có ≥3 BR entries cover Wave 72a + Wave 92 enrichment scope
- [ ] use-cases.md có ≥5 use cases cover admin actor + system actor + compliance reader
- [ ] api-contract.md document read endpoints (if any) + write path conventions
- [ ] Cross-references: rules.md ↔ entity Java class ↔ V54 migration ↔ test cases verifiable
- [ ] Pre-flight check passed per `.claude/skills/quality/pre-flight-check.md` (3-layer structure complete)

## Related

- **Audit origin:** [documents/04-quality/audits/business-logic/2026-05-18-wave-92-business-logic-audit.md](../audits/business-logic/2026-05-18-wave-92-business-logic-audit.md)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** [GAP-619](GAP-619-wave-92-post-wave-audit-suite.md)
- **Sister gap:** [GAP-642](GAP-642-v54-jsonb-testcontainers-it-missing.md) (V54 JSONB columns testing concern)
- **Code references:**
  - `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/` (entity + repository)
  - `kitehub/kitehub-admin/src/main/resources/db/migration/V54__*.sql` (enrichment migration)
- **Rules:**
  - CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
  - `.claude/rules/business-logic-review.md` §2
  - `.claude/rules/meta-gap-priority.md` §3 (META P1 force-multiplier)
  - `.claude/rules/contract-first-for-cross-layer.md`
- **Compliance:** PDPL 2023 Art 11 (evidence trail), ISO27001 A.12.4

## Log

- **2026-05-18** — Initial write-up. Filed từ Wave 92 post-wave audit suite (GAP-619) Business Logic audit finding META P1-2. State-check confirmed `find documents/01-business -type d -iname "*audit*"` returns 0 hits — domain folder không tồn tại. Code shipped Wave 72a + Wave 92 Bucket A orphan ≥3 waves. META P1 force-multiplier — fix 1 lần codify domain → mọi future enhancement auto-comply Living Docs mandate.
