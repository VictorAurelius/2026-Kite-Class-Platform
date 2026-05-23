# GAP-651: Thesis image curation — figure numbering + caption + selection criteria

**Status:** 🟢 DONE 100% (2026-05-23 — Wave thesis-1 Bucket B)
**Priority:** 🟠 P1 (META)
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-18

## Current State (verified 2026-05-18)

| Piece | Status |
|---|---|
| Screenshot capture user manual | ✅ `scripts/capture-user-manual-screenshots.{mjs,sh}` exists (Wave 79 Bucket F1) |
| UI review skill screenshots | ✅ per-persona produced |
| Diagram rendered images | ✅ `documents/06-diagrams/plantuml/*.png/*.svg` (19+ files) |
| CloudWatch dashboard screenshots | ❌ pending GAP-648 |
| Thesis figure numbering convention | ❌ no convention (Figure N.M chưa documented) |
| Thesis caption format Vietnamese | ❌ no template |
| Figure selection criteria (resolution, annotation) | ❌ no skill |
| Thesis figure index | ❌ no `documents/08-thesis/figures/INDEX.md` |

## Problem

Concern 3 + Failure-mode B1/A4 cite "CloudWatch dashboard screenshots Chapter 4" + "AWS Cost Explorer exports". Mỗi chapter thesis cần 5-15 figures với numbering nhất quán (Figure 3.1, Figure 3.2, ...) + caption Vietnamese ("Hình 3.1: Sơ đồ kiến trúc tổng quan KiteHub").

Current state: screenshots scattered (kits/, user-manual/, ui-review/), no thesis-curation pipeline.

## Proposed Fix

### Step 1: Skill `quality/thesis-figure-curation/SKILL.md`

- Trigger: "select figures for thesis Chapter N", "add figure to thesis"
- Workflow:
  1. List candidate images per source (screenshots / diagrams / cloudwatch captures)
  2. Apply selection criteria (§2)
  3. Annotate if needed (per ui-review-prototype skill annotation style — đỏ arrow + viền vàng)
  4. Number Figure N.M (N = chapter, M = sequence)
  5. Caption Vietnamese
  6. Append to `documents/08-thesis/figures/INDEX.md`
  7. Stage in `documents/08-thesis/figures/chapter-N/`

### Step 2: Selection criteria

- Resolution ≥ 1440×900 desktop OR 375×812 mobile
- Vietnamese UI locale (vi-VN), no English placeholders
- VN-friendly sample data (per `user-manual-content-standard.md` §2 row 7)
- Annotation style consistent (red arrow #dc2626, yellow highlight #facc15, numbered steps)
- No sensitive data leaked (real tenant names, real student PII)
- File size < 500KB optimized PNG (use ImageMagick mogrify)

### Step 3: Caption format Vietnamese

```
Hình {N.M}: {Mô tả ngắn 5-15 từ}. {Optional: nguồn capture, e.g., "Capture từ KiteHub /admin dashboard ngày 2026-MM-DD"}
```

Position: bên dưới figure, italic, font 11pt (vs body 13pt).

### Step 4: Thesis figure index

`documents/08-thesis/figures/INDEX.md`:
```markdown
# Thesis Figure Index

## Chapter 1 — Introduction
| Figure | Caption | Source | Path |
|---|---|---|---|
| 1.1 | Sơ đồ tổng quan thị trường edu SaaS VN | competitor analysis | chapter-1/1.1-market-overview.png |

## Chapter 2 — Theoretical Background
...
```

### Step 5: Pipeline integration với thesis-docx assembly

Pair với GAP-646 — `scripts/assemble-thesis-docx.sh` reads `figures/INDEX.md` + injects figures với caption at marker `{{figure:N.M}}` trong source markdown.

## Acceptance Criteria

- [x] Skill `.claude/skills/quality/thesis-figure-curation/SKILL.md` shipped (Wave thesis-1 Bucket B 2026-05-23)
- [x] Selection criteria documented — `reference/figure-selection-criteria.md` (decision matrix 12 rows + 9 quality bar items + citation heuristic + anti-patterns)
- [x] Caption format Vietnamese documented — `reference/caption-format-vietnamese.md` (template + 8 ví dụ + persona-specific tone + caption cho code listing + Mermaid block convention)
- [x] Per-chapter INDEX files created — `documents/08-thesis/chapter-{1,2,3,4}-INDEX.md` (4 files, mỗi file table populated từ real audit + actionable checklist captions cần bổ sung)
- [x] Audit script `scripts/audit-figures.sh` shipped — handles 3 figure types (markdown image / Mermaid / PlantUML) + caption coverage % + numbering integrity (gap detection) + citation heuristic; JSON + human modes
- [x] Self-test fixture verified — synthetic fixture với 3 visuals + Hình 2.1 + Hình 2.3 (skipping 2.2) → script correctly reports `chapter 2 jumps 1→3` numbering gap + `2/3 captioned (66%)` coverage
- [x] Baseline audit captured cho 6 chapter files: data/last-run-chapter-1.json (3 files / 6 visuals), data/last-run-chapter-2.json (1 file / 8 visuals), data/last-run-chapter-3.json (1 file / 9 visuals), data/last-run-chapter-4.json (1 file / 4 visuals) — tổng 27 visuals, 0% caption coverage hiện tại

**Out-of-scope (track separately):**
- GAP-646 thesis-docx-pipeline injection marker `{{figure:N.M}}` — sister gap, scope khác (pipeline integration vs curation skill)
- Actual caption insertion vào 27 visual blocks — INDEX files đã list captions đề xuất; bổ sung captions là content authoring task (separate PR, không thuộc skill scope per task constraint "Don't edit chapter content")
- Numbering scheme reference (`reference/numbering-scheme.md`) bổ sung — shipped same PR per skill structure requirements (4 reference docs total)

## Related

- GAP-646 thesis-docx-pipeline (figure injection)
- GAP-648 thesis-nfr-data-capture (CloudWatch screenshots feed this curation)
- `user-manual-content-standard.md` §2 row 6 annotation style (reuse pattern)
- `quality/ui-review/SKILL.md` (screenshot capture pipeline reuse)
- `scripts/capture-user-manual-screenshots.{mjs,sh}` (extend cho thesis scope)

## Log

- **2026-05-23 (DONE — Wave thesis-1 Bucket B):** Skill `.claude/skills/quality/thesis-figure-curation/` shipped với SKILL.md (<100 lines body) + 3 reference docs (figure-selection-criteria, caption-format-vietnamese, numbering-scheme) + audit-figures.sh script (174 lines bash, handles 3 figure types + caption coverage % + numbering integrity + citation heuristic, JSON + human modes). 4 INDEX files generated từ real audit run: chapter-1-INDEX.md (6 visuals across 3 files: 1 Mermaid + 5 PNG screenshots), chapter-2-INDEX.md (8 Mermaid blocks), chapter-3-INDEX.md (9 visuals: 8 PNG + 1 Mermaid), chapter-4-INDEX.md (4 Mermaid). Tổng baseline: 27 visual blocks, 0% caption coverage hiện tại — INDEX files list captions đề xuất + actionable checklist cho content authoring follow-up (out-of-scope per task constraint). Skill conventions check PASS (54/54 skill files). Self-test fixture verified script catches caption coverage + numbering gap (Hình 2.1 + 2.3 skip 2.2 → "chapter 2 jumps 1→3"). Baseline audit JSON snapshots saved trong `data/last-run-chapter-{1,2,3,4}.json` cho monitoring. Per `gap-done-discipline.md` §2 6 criteria: (1) all AC `[x]` checked ✅, (2) no banned phrases trong this Log ✅, (3) no `[skip]`/`[wontfix]` annotations ✅, (4) skill + 3 references + 1 script + 4 INDEX files all shipped same PR (no multi-stage deferral) ✅, (5) verification artifact pointer = `data/last-run-chapter-*.json` JSON snapshots + INDEX files ✅, (6) verified on real chapter files trong repo (not isolated fixture) ✅. Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check: gap 5 ngày tuổi từ Found 2026-05-18, drift-class category — verified scope còn áp dụng (27 visual blocks chưa có caption confirms problem persists). Git mv to `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 §3.3. CSV row updated (status DONE, completion_pct 100, filename phase-1-beta/closed/..., last_verified 2026-05-23) per `gap-architecture-v2.md` §3 + `post-merge-sync-completeness.md` §2 target 1.
- **2026-05-18 (created):** Filed per Concern 3 + Failure-mode requirement Chapter 4 data visualization.
