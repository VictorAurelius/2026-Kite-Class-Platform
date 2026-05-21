---
id: GAP-696
phase: phase-1-beta
status: OPEN
priority: P1
domain: Meta
audience: dev
---

# GAP-696: Wave 102.7.5 deferred items — Bucket A A2/A3 + Bucket C pipeline cleanup

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META — academic polish deferred từ Wave 102.7.4 rate-limit)
**Domain:** Meta — thesis V1 polish
**Found:** 2026-05-21 (Wave 102.7.4 closure)
**Affects:** thesis-v1.docx polish quality

## Problem

Wave 102.7.4 original plan = 3 buckets parallel ship 10 items. 3 agents bị Anthropic server rate-limit mid-work. Salvage approach ship project-jargon scrub priority 1 only (32/32 hits → 0); defer 8 items remaining sang Wave 102.7.5.

## Root Cause

Anthropic server-side rate-limit (not user quota) — 3 background agents failed cùng lúc. Recovery: coordinator finished scrub directly. Pipeline cleanup + listing rewrite + Nhóm separator reframe defer do context budget + time constraint single session.

## Proposed Fix

Wave 102.7.5 3-bucket parallel (retry sau rate-limit window cleared):

### Bucket A (Ch.2)
- **Task A2** Item 12 — `Nhóm N — Title:` separator (6 hits Ch.2) → reframe văn nói → văn viết
- **Task A3** Item 14 — Service catalog listing rewrite (`6 microservice backend (a, b, c, d, e, f)` pattern) → narrative grouping

### Bucket C (Pipeline `create_thesis_v1.py` + new folder)
- **Item 8** Phụ lục A → fold Ch.4 hoặc remove; Phụ lục B → github link inline
- **Item 9** Phụ lục C (AI audit) → remove entirely
- **Item 10** Danh mục thuật ngữ + từ viết tắt → sort alphabet ABC (Vietnamese-aware)
- **action-2.md §4 lines 39-40** Create `documents/08-thesis/figures/` + README naming convention
- **action-2.md §4 line 42** Lookup `documents/07-archived/academic/word-reports/de-cuong-datn*` → extract MSSV/lớp/khóa → verify bìa pipeline uses correct data

### Items NOT in scope Wave 102.7.5
- Item 11 F9 (manual Word pre-defense)
- Item 13 from action-2.md §4 line 43 Focus khung đề cương (Wave 102.5 đã partial)
- action-2.md §4 line 41 Tuyệt đối không claude (0 hits verified)

## Acceptance Criteria

- [ ] Bucket A task A2: `Nhóm [0-9] — ` separator pattern reframe (6 hits)
- [ ] Bucket A task A3: service catalog listing rewrite (3-4 hits)
- [ ] Bucket C: Phụ lục A decision (fold OR remove) + Phụ lục B github link + Phụ lục C removed
- [ ] Bucket C: ABC sort logic verified
- [ ] Bucket C: `documents/08-thesis/figures/` folder + README created
- [ ] Bucket C: Personal data lookup đề cương + bìa pipeline verified
- [ ] thesis-v1.docx re-bake clean

## Related

- Wave 102.7.4 closure audit: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.7.4-project-jargon-scrub.md`
- Wave 102.7.4 plan: PR #1676 `documents/03-planning/waves/wave-2026-05-20-102.7.4-thesis-v1-inside-deferred-cleanup.md`
- 14-item mapping audit: `documents/04-quality/audits/persona-review/2026-05-20-wave-102.7-14-item-inside-mapping.md`
- Action-2.md inside source: PR #1585 (committed 2026-05-19)

## Log

- **2026-05-21 (filed):** Filed Wave 102.7.4 closure. 8 items defer do 3-bucket parallel rate-limit failure. Coordinator finished project-jargon scrub (Wave 102.7.4 priority 1) directly; remaining 8 items defer Wave 102.7.5 retry sau rate-limit window cleared.
