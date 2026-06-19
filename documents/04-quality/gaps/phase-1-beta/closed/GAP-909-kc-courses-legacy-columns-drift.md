# GAP-909: KC `courses` entity vs migration history drift — cover_image_url + suggested_tuition

**Status:** 🟢 DONE (wave-gap-audit-p1-1 2026-06-19 — substantive work shipped + CI-verified; residual cosmetic doc-ref/AC-checkbox only per verify pass)
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC academic structure)
**Affects:** `kiteclass-core` module course; `courses` table

## Problem

Lịch sử migration dài (V1/V27/V67/V70). Drift entity↔DB:
- Entity dùng `cover_image_url`, KHÔNG `thumbnail_url`
- `suggested_tuition`/`default_sessions` là cột V1 không còn map vào entity
- `R67__undo_pricing_model.sql` là script rollback THỦ CÔNG (KHÔNG tự áp dụng bởi Flyway)

Pattern cleanup tương tự GAP-904 (grades legacy).

## Proposed Fix

Cleanup migration V## DROP cột legacy `suggested_tuition`/`default_sessions` sau khi verify zero usage. Document R67 rollback script trong runbook.

## Acceptance Criteria

- [ ] Verify zero usage cột legacy
- [ ] Migration V## DROP legacy columns
- [ ] R67 rollback runbook
- [ ] Reference cluster doc 01-academic-structure courses section

## Discovered in

`documents/02-architecture/database/kiteclass/01-academic-structure.md` courses section
