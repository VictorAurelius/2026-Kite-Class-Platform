# GAP-1070: Thiếu detector FE→BE method-level contract (chiều ngược của check-be-fe-url-contract)

**Status:** 🔵 OPEN
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

## Acceptance Criteria

- [ ] Detector FE→BE method-level shipped (WARN-mode)
- [ ] Self-test: GAP-1069 pre-fix state (FE GET /classes collection, BE chỉ /{id}) → detector FLAG
- [ ] Wire CI job + WARN initially
- [ ] Document trong GAP-802 family (cơ chế #3)

## Related

- Discovered in: KC-1 G2 sweep 2026-06-08
- GAP-802 (BE↔FE contract detectors — cơ chế #2 = BE→FE; rule này = #3 FE→BE)
- GAP-1069 (classes/invoices 404 — incident lớp này)
- `g1-browser-walk-before-flip.md` (runtime-catch lớp này; detector = static pre-CI catch)
