# GAP-1070: Thiếu detector FE→BE method-level contract (chiều ngược của check-be-fe-url-contract)

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** DevOps (CI tooling)
**Found:** 2026-06-08 (KC-1 G2 — sweep lớp GAP-1069 phát hiện detector hiện tại sai hướng)
**Affects:** CI contract-drift coverage; lớp bug "FE gọi endpoint BE không expose" (GAP-1069 class) cho mọi flow

## Problem

`scripts/check-be-fe-url-contract.sh` (GAP-802 cơ chế #2) chỉ check **BE→FE**: path BE build (email link / redirect) → có FE route không. Nó **KHÔNG check chiều ngược** FE→BE: FE axios gọi `GET /api/v1/<resource>` → BE có @*Mapping **đúng method + path** không.

→ Lớp GAP-1069 (FE dashboard gọi `GET /api/v1/classes` collection nhưng BE chỉ có `GET /api/v1/classes/{id}`) **slip qua CI** — chỉ lộ khi browser-walk (G2). Sweep prefix-level thô (`comm` FE vs BE root path) cũng không bắt vì prefix `/api/v1/classes` match cả `/{id}` mapping → cần granularity **method + exact path**.

Verify: sweep prefix-level 2026-06-08 → 0 drift (8/8 FE resource có BE controller) NHƯNG GAP-1069 vẫn tồn tại trước fix → chứng minh prefix-sweep không đủ.

## Proposed Fix

Tạo `scripts/check-fe-be-api-contract.sh`: (1) extract FE axios/fetch call sites `(method, path-template)` trong `*-frontend/src`; (2) extract BE `@{Get,Post,Put,Delete,Patch}Mapping` full path (class `@RequestMapping` + method path) trong `*-core`/`kitehub-*`; (3) flag FE `(method,path)` không có BE mapping khớp. WARN-mode đầu (FP từ dynamic path template). Wire CI job mirror `be-fe-url-contract`. Bổ sung cơ chế #3 cho GAP-802 family.

## Current State (verified 2026-06-08)

Detector `scripts/check-fe-be-api-contract.sh` (~270 LOC) shipped + self-test PASS. Coverage:

| Hạng mục | Trạng thái |
|---|---|
| Extract FE call sites `(METHOD, path)` (axios `.get/.post/.put/.delete/.patch` + `fetch`) | ✅ done |
| Extract BE mappings `(METHOD, full-path)` (class `@RequestMapping` + method `@{Get,Post,...}Mapping` + `@RequestMapping(method=)` → `ANY`) | ✅ done |
| Normalize `${id}` / `{classId}` → `{*}` wildcard, strip query string | ✅ done |
| Segment-wise wildcard match (segment-count equal, `{*}` matches 1 segment) | ✅ done |
| WARN-mode (exit 0 + `--json`) | ✅ done |
| Self-test fixtures (Case A pre-GAP-1069 FLAG + Case B post-fix PASS) | ✅ `scripts/tests/test-check-fe-be-api-contract.sh` 4/4 PASS |
| Wire CI WARN job (mirror `be-fe-url-contract`) | ⬜ deferred → coordinator wire |
| HARD-STOP promotion sau giảm FP | ⬜ deferred |

Self-test verdict: Case A (FE `GET /api/v1/classes` collection vs BE chỉ `/{id}`) → drift=1 FLAG ✅; Case B (BE thêm `GET /api/v1/classes` flat list) → drift=0 PASS ✅. Repo thật: checked=23 · 6 WARN findings (toàn bộ là false-positive: `/api/auth/*` gateway-routed + `/api/platform/branding/*` cross-service + `/api/v1/instance/config` singular-vs-plural) — 0 false-negative trên lớp GAP-1069 (`classes`/`invoices` resolve đúng).

## Acceptance Criteria

- [x] Detector FE→BE method-level shipped (WARN-mode) — `scripts/check-fe-be-api-contract.sh`
- [x] Self-test: GAP-1069 pre-fix state (FE GET /classes collection, BE chỉ /{id}) → detector FLAG — Case A fixture drift=1
- [ ] Wire CI job + WARN initially — deferred (coordinator wire job `fe-be-api-contract` trong `quality-code.yml`)
- [x] Document trong GAP-802 family (cơ chế #3) — script header + gap §Problem cite GAP-802 #3

## Related

- Discovered in: KC-1 G2 sweep 2026-06-08
- GAP-802 (BE↔FE contract detectors — cơ chế #2 = BE→FE; rule này = #3 FE→BE)
- GAP-1069 (classes/invoices 404 — incident lớp này)
- `g1-browser-walk-before-flip.md` (runtime-catch lớp này; detector = static pre-CI catch)

## Log

- **2026-06-08:** Scope revised after build. Shipped `scripts/check-fe-be-api-contract.sh` (FE axios/fetch + BE @*Mapping extract, method+path wildcard match, WARN-mode) + `scripts/tests/test-check-fe-be-api-contract.sh` (Case A pre-GAP-1069 FLAG + Case B post-fix PASS, 4/4 assertions). Self-test verified detector catches method-level collection-vs-{id} drift (GAP-1069 class) while passing post-fix flat list. Repo thật 6 WARN = all false-positive (gateway-routed `/api/auth/*` + cross-service `/api/platform/branding/*` + singular `/api/v1/instance/config`); 0 false-negative trên target class. Status OPEN→PARTIAL: 3/4 AC done; CI wiring + HARD-STOP deferred to coordinator (per `audit-to-gap-pipeline.md` §2.5 PARTIAL exit ramp).
