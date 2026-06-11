# GAP-1109: JPQL untyped-null-param 42P18 sweep — branding + kiteclass residual sites

**Status:** 🟢 DONE 2026-06-10
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-10 (GAP-1106 detector `check-jpql-untyped-null-param.sh` surfaced sister sites)
**Affects:** kitehub-branding (1 site) + kiteclass-core (9 sites) — JPQL `(:param IS NULL OR ...)` untyped-null pattern

## Problem

GAP-1106 (subscription cursor 42P18) fix ship CI detector `scripts/check-jpql-untyped-null-param.sh`. Detector self-test (post-fix subscription = 0 hit) đồng thời lộ **1 branding + 9 kiteclass** sister site CÙNG CLASS `(:param IS NULL OR ...)` — Hibernate bind untyped null vào vị trí `IS NULL` → Postgres 42P18 `could not determine data type of parameter` tại PREPARE (H2 che mất; chỉ lộ trên Postgres production-equivalent).

Đây là cùng bug-class với GAP-1106 + GAP-1105 (branding lifecycle-events 42P18) đã fix riêng. Per `cross-flow-bug-class-sweep.md` §4.1: statically-detectable class → đã ship detector; residual sites cần sweep theo cùng precedent (GAP-1028 split-query: first-page no-param + after-cursor typed-param + default-method branch).

## Proposed Fix

Per GAP-1028 / GAP-1106 precedent cho từng site:
1. Liệt kê 10 site qua `bash scripts/check-jpql-untyped-null-param.sh` (full output, không `| head`)
2. Mỗi `@Query` có `(:param IS NULL OR ...)` → split thành 2 query (no-param first-page + typed-param) + default-method branch, HOẶC dùng typed `CAST(:param AS ...)` nếu split không khả thi
3. Testcontainers Postgres IT cho mỗi repo touched (production-equivalent, bắt 42P18 mà H2 che)
4. Giữ public method signature ổn định (caller không đổi — per `api-contract-change-caller-sweep.md`)
5. Sau fix: detector full-repo 0 hit → cân nhắc flip detector WARN → HARD-STOP

## Acceptance Criteria

- [x] 9 kiteclass site fixed via Spring Data Specification (branding site self-corrected qua GAP-1105 → detector 0 branding hit); signature-stable
- [x] Testcontainers Postgres IT `JpqlUntypedNullParam42P18IT` (4 repos: Course/Incident/Vetting/PayrollPeriod), PASS, verify no 42P18
- [x] Caller sweep clean (signatures unchanged — `@Query` → `default` method, prod + test mock unaffected)
- [x] `check-jpql-untyped-null-param.sh` full-repo scan = 0 hit
- [x] Detector giữ WARN mode (decision: shared-detector HARD-STOP flip = wave meta riêng, ngoài scope wave này)

## Related

- Sibling fixed: GAP-1106 (subscription cursor — DONE), GAP-1105 (branding lifecycle-events 42P18 — PARTIAL)
- Precedent: GAP-1028 (split-query pattern)
- Detector: `scripts/check-jpql-untyped-null-param.sh` (shipped GAP-1106, commit 142919be → 01bfad3e)
- Discovered in: PR #2279 integration (GAP-1106 detector output)
- Sweep rule: `.claude/rules/cross-flow-bug-class-sweep.md`

## Log

- **2026-06-10 (DONE):** Fixed 9 kiteclass JPQL 42P18 sites via Spring Data `Specification` (predicate add chỉ khi param non-null → không bao giờ bind untyped null ở vị trí IS NULL; giữ tenant `@Filter`, signature-stable). Testcontainers IT `JpqlUntypedNullParam42P18IT` 4/4 PASS; `check-jpql-untyped-null-param.sh` = 0 hit repo-wide (branding site đã tự khỏi qua GAP-1105). Wave branding-fix-2026-06-10 (agent a160bdac, SHA 7aa22976).
- **2026-06-10:** Filed từ GAP-1106 detector output (1 branding + 9 kiteclass residual sites). DEFER sang wave-fix riêng — cần Testcontainers IT per repo + caller sweep; vượt scope PR #2279 (subscription-only). Per `discovery-to-gap-inline-filing.md`.
