---
id: GAP-696
phase: phase-1-beta
status: DONE
priority: P1
domain: Meta
audience: dev
---

# GAP-696: Wave 102.7.5 deferred items — Bucket A A2/A3 + Bucket C pipeline cleanup

**Status:** 🟢 DONE 2026-05-21 — Wave 102.7.5 SHIPPED 3-bucket parallel A/B/C (PRs #1682/1683/1684) + coordinator re-bake thesis-v1.docx successful (647 paragraphs, 4 sections, -136 bytes post Phụ lục removal)
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

- [x] Bucket A task A2: `Nhóm [0-9] — ` separator pattern reframe (6 hits) — PR #1684; `grep -cE "^\*\*Nhóm [0-9]+ — " documents/08-thesis/chapter-2-system-architecture.md` → 0
- [x] Bucket A task A3: service catalog listing rewrite (3-4 hits) — PR #1684; §2.3.5 inline listing reframed thành narrative grouping 3 lớp (KiteHub platform / KiteClass tenant / FE)
- [x] Bucket B: Phụ lục A REMOVE + Phụ lục B inline GitHub link (KẾT LUẬN §1) + Phụ lục C REMOVE — PR #1683; `add_appendix()` function deleted entirely + call site dropped + step counter synced "[8/8] Tài liệu tham khảo IEEE"
- [x] Bucket B: ABC sort logic verified (VN-aware manual reorder) — PR #1683; terms 10 entries + abbrevs 26 entries sorted alphabetically (`Continuous Deployment` → `Test-Driven Development`; `ALB` → `VPC`)
- [x] Bucket C: `documents/08-thesis/figures/` folder + README created (67 lines, 4 sections per `docs-folder-structure.md` §3) — PR #1682
- [x] Bucket C: Personal data lookup đề cương + bìa pipeline verified — PR #1682 audit `documents/08-thesis/audits/2026-05-21-bia-pipeline-personal-data-verify.md` verdict ALIGNED ✓ (8/8 fields match canonical `student-info.md`, no drift, no fix needed)
- [x] thesis-v1.docx re-bake clean — coordinator post-merge run `python3 create_thesis_v1.py` PASS (647 paragraphs, 4 sections, -136 bytes vs pre-Wave-102.7.5)

## Related

- Wave 102.7.4 closure audit: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.7.4-project-jargon-scrub.md`
- Wave 102.7.4 plan: PR #1676 `documents/03-planning/waves/wave-2026-05-20-102.7.4-thesis-v1-inside-deferred-cleanup.md`
- 14-item mapping audit: `documents/04-quality/audits/persona-review/2026-05-20-wave-102.7-14-item-inside-mapping.md`
- Action-2.md inside source: PR #1585 (committed 2026-05-19)

## Log

- **2026-05-21 (DONE):** Wave 102.7.5 SHIPPED 3-bucket parallel — PR #1682 (Bucket C figures + audit) + PR #1683 (Bucket B pipeline cleanup) + PR #1684 (Bucket A Ch.2 narrative reframe). Coordinator post-merge re-bake thesis-v1.docx PASS (647 paragraphs, 4 sections, -136 bytes vs pre-wave reflecting Phụ lục A+C removal). 7/7 AC verified above với evidence pointers. Per `gap-done-discipline.md` §2: all `- [x]` checked + verification artifact references (PR numbers + grep output + audit verdict + bake output) + no banned phrases in this Log entry. Pre-handoff verify per `pre-handoff-self-test-completeness.md`: AC scope = docs/thesis polish (no user-facing flow); verification = grep + AST + audit confirmed all on main post-merge. Status flip DONE per §3 PARTIAL exit ramp final close.
- **2026-05-21 (filed):** Filed Wave 102.7.4 closure. 8 items defer do 3-bucket parallel rate-limit failure. Coordinator finished project-jargon scrub (Wave 102.7.4 priority 1) directly; remaining 8 items defer Wave 102.7.5 retry sau rate-limit window cleared.
