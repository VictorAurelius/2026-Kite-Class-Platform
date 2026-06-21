# GAP-1509: cross-layer-contract-drift detector heuristic v1 FP rate quá cao

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-21 (API-contract full audit refresh — phase-1-closeout-loop)
**Affects:** `scripts/check-cross-layer-contract-drift.sh` + CI job `cross-layer-contract-drift` (`.github/workflows/quality-code.yml`)

## Problem

Detector `check-cross-layer-contract-drift.sh` (heuristic v1, shipped Wave 99C per `contract-first-for-cross-layer.md` §6.2) báo **84 drift candidate** trong audit run 2026-06-21, nhưng verify mẫu cho thấy ~đa số là FALSE POSITIVE:

- `/api/v1/classes` → documented trong 8 file api-contract.md
- `/api/v1/courses` → 5 file
- `/api/v1/students` → 12 file
- `/api/v1/grades` → 4 file

Detector báo "no matching api-contract Endpoint" vì heuristic so khớp base-path literal với format `Endpoint` declaration cụ thể, nhưng docs thực tế dùng path trong prose/bảng (vd `## POST /api/v1/classes/{id}/start`) → không match.

Hệ quả: detector không thể chuyển sang HARD-STOP (mục tiêu post-30-day stabilization per `contract-first-for-cross-layer.md` §6.2) với FP rate này → mất giá trị làm CI guard cho Cat 1.1/1.2; auditor phải verify thủ công từng candidate (84 lần) → audit Cat 1.2 luôn `❓ UNCHECKED`.

## Root Cause

Heuristic v1 grep-based, so khớp controller base-path literal với 1 format Endpoint declaration cố định; không parse được path declaration trong markdown heading/table/prose (đa số api-contract.md dùng heading `## METHOD /path`). Không có AST/path-normalization.

## Proposed Fix

Nâng detector: extract path từ markdown headings `^#{1,4}\s+(GET|POST|PUT|DELETE|PATCH)\s+/api/...` + path trong code-fence + table cells, normalize path params (`{id}` ↔ `{classId}`), so khớp set-difference thay vì exact literal. Mục tiêu FP < 5% để promote HARD-STOP.

## Acceptance Criteria

- [ ] Detector extract path từ markdown heading + table + code-fence (không chỉ `Endpoint` declaration format)
- [ ] Re-run trên main HEAD → FP rate < 5% (verify classes/courses/students/grades không còn báo drift)
- [ ] Genuine undocumented (branding cluster GAP-1251) vẫn được flag đúng
- [ ] Self-test fixture (match + real-drift) updated

## Related

- Discovered in: API-contract full audit `documents/04-quality/audits/api-contract/2026-06-21-api-contract-full-audit.md` (B4)
- Rule: `.claude/rules/contract-first-for-cross-layer.md` §6.2 (detector wiring, HARD-STOP target)
- Genuine undocumented cluster detector should catch: GAP-1251 (branding)
