# GAP-651: Thesis image curation — figure numbering + caption + selection criteria

**Status:** 🔵 OPEN
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

- [ ] Skill `quality/thesis-figure-curation/SKILL.md` shipped
- [ ] Selection criteria documented (5 items)
- [ ] Caption format Vietnamese documented
- [ ] `documents/08-thesis/figures/INDEX.md` skeleton created (7 chapters)
- [ ] Sample 3 figures curated cho Chapter 4 (audit score dashboard) — verify pipeline works
- [ ] GAP-646 thesis-docx-pipeline injection marker `{{figure:N.M}}` documented

## Related

- GAP-646 thesis-docx-pipeline (figure injection)
- GAP-648 thesis-nfr-data-capture (CloudWatch screenshots feed this curation)
- `user-manual-content-standard.md` §2 row 6 annotation style (reuse pattern)
- `quality/ui-review/SKILL.md` (screenshot capture pipeline reuse)
- `scripts/capture-user-manual-screenshots.{mjs,sh}` (extend cho thesis scope)

## Log

- **2026-05-18 (created):** Filed per Concern 3 + Failure-mode requirement Chapter 4 data visualization.
