# GAP-683: Thesis chapter citation polish — Ch.2 numbering collision + PDPL inconsistency + orphan retro-cite

**Status:** 🟢 DONE 2026-05-19 — Phase 4 V1 closure (Foundation + Buckets A/B/C/D). Orphan 24 → 5 (-79%); Ch.2 LOCAL collision resolved; PDPL [5] fix shipped; [40] dedup
**Priority:** 🔴 P0 (defense risk — surfaced bởi cross-ref audit 2026-05-19)
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-19 (Wave 100.7 Phase 3a cross-ref audit)
**Closed:** 2026-05-19 (Wave 100.7 Phase 4 V1 ship)
**Related Audits:** [`cross-ref-audit-2026-05-19.md`](../../../08-thesis/references/cross-ref-audit-2026-05-19.md) Round 3 final state

## Current State (verified 2026-05-19 — Wave 100.7 Phase 3a Agent 3a findings)

| Piece | Status |
|---|---|
| Bibliography format IEEE-compliant | ✅ 43/43 refs PASS audit Phase 3a |
| Chapter 1 in-text citations resolve | ✅ 17 unique refs cited, 0 missing |
| Chapter 2 numbering collision | ❌ LOCAL `[1]-[8]` section line 683-699 conflicts global `[1]-[43]` |
| `[40]` vs `[21]` duplicate (PDPL 49/2023/QH15) | ❌ Same VN law cited 2 lần, slight format diff |
| Ch.2 `[5]` PDPL number | ❌ Cites `91/2025/QH15` — incorrect; correct = `49/2023/QH15` per global `[21]/[40]` |
| Orphan refs (Wave 100 + Wave 100.7 Phase 2 adds) | ❌ 24/43 refs (56%) chưa được cite retroactively trong chapters: `[3] [4] [6]–[16] [18]–[20] [22]–[30] [31]–[38] [40]–[43]` |

## Problem

Wave 100.7 Phase 3a (Agent 3a) audit bibliography 43 IEEE refs + grep mọi chapter file. 4 phát hiện ảnh hưởng thesis defense quality:

### 1. Chapter 2 LOCAL numbering collision (P0 — examiner sẽ catch ngay)

`chapter-2-system-architecture.md` line 683-699 có section "Tài liệu tham khảo" RIÊNG với 8 entries `[1]-[8]` (AWS SaaS Lens / Azure / Pothon / PostgreSQL / VN PDPL / VN Cybersecurity / Microsoft / AWS SaaS Factory) tách rời global bibliography `[1]-[43]`. Reader sẽ confuse: `[1]` Chương 2 (= AWS SaaS Lens) vs `[1]` global (= EasyEdu). Format chuẩn UIT/HUST/UET yêu cầu **single global bibliography** ở cuối, in-text citation `[N]` resolve về duy nhất 1 row.

### 2. `[40]` duplicate `[21]` (same VN law, 2 entries)

Bibliography global:
- `[21]` "Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023)," Số 49/2023/QH15, 2023.
- `[40]` "Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023) — Số 49/2023/QH15, có hiệu lực 2026-07-01," 2023.

Hai entries reference cùng văn bản pháp luật, format khác nhẹ. Phải merge thành 1 ref.

### 3. Chapter 2 `[5]` PDPL số hiệu sai

`chapter-2-system-architecture.md` cite `[5] PDPL số 91/2025/QH15` — sai. Số hiệu canonical = `49/2023/QH15` (per global `[21]/[40]` cùng thuvienphapluat.vn link). `91/2025/QH15` không tồn tại trong bibliography. Examiner check law citation accuracy sẽ catch ngay.

### 4. Orphan refs (24/43) không được cite trong chapter narrative

Refs `[31]-[38]` Wave 100 Bucket D added + `[40]-[43]` Wave 100.7 Phase 2 added — chapters chưa được retro-cite. Examiner check "tại sao bibliography liệt kê ref X nhưng narrative không tham chiếu?" → red flag.

Examples:
- `[35]` Anthropic Claude API docs — Ch.1 ai-techniques có nội dung relevant nhưng KHÔNG cite `[35]`
- `[42]` Forsgren Accelerate (DevOps metrics) — Ch.2 NFR section có nội dung relevant nhưng KHÔNG cite
- `[43]` Sato et al. Continuous Delivery ICSE 2020 — Ch.2 methodology relevant nhưng KHÔNG cite
- `[31]-[34]` VN edu SaaS competitors (Mona / DotB / TT29/2024/TT-BGDDT / VECITA EdTech report) — Ch.1 competitor-analysis có nội dung relevant nhưng KHÔNG cite retroactively

## Proposed Fix

### Step 1: Merge `[40]` → `[21]` deduplication

- Rename `[40]` content vào `[21]` body: keep canonical "Số 49/2023/QH15, 2023" với note "có hiệu lực 2026-07-01" appended
- Renumber `[41]-[43]` → `[40]-[42]` (3 refs shift -1)
- Update mọi in-text `[40]` → `[21]`, `[41]` → `[40]`, `[42]` → `[41]`, `[43]` → `[42]`

### Step 2: Fix Chapter 2 LOCAL numbering collision

- Delete section "Tài liệu tham khảo" line 683-699 trong `chapter-2-system-architecture.md`
- Mỗi entry trong section ấy được migrate vào global `bibliography.md` (check dedup trước — vd AWS SaaS Lens đã có `[9]`)
- Update mọi in-text `[1]-[8]` trong Ch.2 narrative refer global ref numbers

### Step 3: Fix Chapter 2 `[5]` PDPL number

- Trong `chapter-2-system-architecture.md` body: `[5] Luật Bảo vệ Dữ liệu Cá nhân (PDPL) số 91/2025/QH15` → `[21] PDPL số 49/2023/QH15` (after Step 1 dedup → still `[21]`)
- Verify all PDPL references trong narrative thống nhất `[21]`

### Step 4: Orphan refs retro-cite (best-effort)

Walk 24 orphan refs. Per orphan ref:
- Identify chapter section narrative content relevant
- Insert `[N]` citation inline (1 sentence with ref support)
- If no narrative content matches → leave orphan + document trong cross-ref-audit Q3 note "ref được giữ trong bibliography để defense Q&A reference, không cite inline"

Target: reduce orphan count 24 → ≤8 (sweet spot = ≥80% refs cited trong narrative).

### Step 5: Re-run cross-ref audit

Re-run grep `\[[0-9]+\]` mọi chapter file. Verify:
- 0 missing-ref (every `[N]` resolves to global bibliography row)
- 0 LOCAL numbering collision (every chapter uses global numbering)
- Orphan count ≤8
- PDPL số hiệu thống nhất `[21]` across all chapter mentions

Update `documents/08-thesis/references/cross-ref-audit-2026-05-19.md` với "Round 2 audit" section + final stats.

## Acceptance Criteria

- [x] `[40]` merged into `[21]`; `[41]-[43]` renumbered `[40]-[42]`; all in-text shifted — DONE Phase 4 Foundation `b3a7361a` (PR #1595)
- [x] Chapter 2 LOCAL `[1]-[8]` section deleted; entries migrated to global bibliography (dedup); in-text Ch.2 numbering uses global refs — DONE Phase 4 Bucket B (PR #1598). Mapping: `[1]`→`[9]` AWS SaaS Lens / `[2]`→`[10]` Azure / `[3]`→new `[43]` Pothon / `[4]`→`[12]` PostgreSQL / `[5]`→`[21]` PDPL / `[6]`→`[23]` Cybersecurity / `[7]`→new `[44]` Brown C4 / `[8]`→`[28]` OWASP. Bibliography 42 → 44 net refs.
- [x] Chapter 2 `[5]` PDPL số fixed: `91/2025/QH15` → `49/2023/QH15` matching global `[21]` — DONE Phase 4 Bucket B (3 narrative fixes lines 29/144/659)
- [x] Orphan refs retro-cite: 24 → ≤8 (≥80% utilization) — **EXCEEDED:** Phase 4 final state 24 → **5 orphans** (89% inline utilization). Buckets A/C cited 15+ orphans; Bucket D added Ch.2 inline cites cho new refs `[43]` + `[44]`.
- [x] cross-ref-audit-2026-05-19.md updated với Round 2 + Round 3 results — DONE (Round 2 banner Foundation; Round 3 §10 final state Bucket D)
- [x] No new orphan refs introduced; no new missing-refs in chapters — VERIFIED (Round 3 audit confirms 5 remaining orphans là intentional defer for VN academic context + V2 expansion candidates)
- [x] Phase 4 V1 PR bundles these fixes — DONE this PR (Phase 4 V1 closure)

## Remaining intentional orphans (≤ target ≤8) — defer rationale per Round 3 audit

| Ref | Reason left orphan |
|---|---|
| `[5]` UIT microservices thesis | VN academic context reserve; optional cite trong Ch.1 V2 expansion |
| `[6]` UIT thesis catalog | Same as `[5]` |
| `[8]` M. Fowler *Patterns of Enterprise Application Architecture* | Ch.2 V2 expansion candidate |
| `[30]` VMware Tanzu Spring Security 6.4 | Ch.3 implementation reference; optional inline cite (Ch.2 OWASP table cites `[28]` directly) |
| `[36]` OpenAI GPT-4 technical report | Defer Wave 101+ if Ch.5-7 expansion mở rộng AI scope (Ch.1 AI section cites Anthropic `[35]` only) |

## Related

- GAP-647 thesis-bibliography-ieee (PARTIAL 50% — bibliography exists; this gap addresses retro-cite + numbering polish)
- GAP-646 thesis-docx-pipeline (PARTIAL 20% — DOCX assembly will need clean citation refs)
- GAP-650 thesis-chapter-1-literature (Wave 100.7 Phase 4 closure)
- Wave 100.7 Phase 4 (parent plan) — natural venue for these fixes (V1 PR sweep over chapter files)
- [`cross-ref-audit-2026-05-19.md`](../../../08-thesis/references/cross-ref-audit-2026-05-19.md) — Phase 3a Agent 3a findings (source of truth)

## Log

- **2026-05-19 (DONE flip):** Status `🔵 OPEN` → `🟢 DONE`. All 7 AC checked. Phase 4 V1 closure shipped: Foundation `b3a7361a` (`[40]`→`[21]` dedup + renumber) + Bucket A `34c863ee` (Ch.1 retro-cite 6 refs) + Bucket B `b113e646` (Ch.2 LOCAL→global migration + PDPL fix) + Bucket C `5279cbbd` (Ch.3+4 retro-cite 9 refs) + Bucket D (this V1 closure PR — Ch.2 inline `[43]`+`[44]` + Round 3 audit). Orphan 24 → 5 (-79%, target ≤8 EXCEEDED). 5 remaining orphans documented as intentional defer (VN academic / V2 expansion / Wave 101+ scope).
- **2026-05-19 (created):** Filed per Wave 100.7 Phase 3a cross-ref audit findings (Agent 3a PR #1592). 4 P0 issues surfaced: Ch.2 LOCAL numbering collision + `[40]` duplicate `[21]` + Ch.2 `[5]` PDPL số sai + 24/43 orphan refs. Filed per `audit-to-gap-pipeline.md` Step 3. Defense risk — examiner check citation discipline sẽ catch ngay. Natural fix venue = Wave 100.7 Phase 4 V1 PR (next session) sweeping chapter files for final polish.
