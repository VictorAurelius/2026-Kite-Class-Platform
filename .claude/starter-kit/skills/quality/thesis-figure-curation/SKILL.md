---
name: thesis-figure-curation
description: "Dùng khi user nói 'figure curation', 'thesis figures', 'caption', 'đánh số hình', 'INDEX figure', 'curate figure cho Chapter N', 'add figure to thesis', 'kiểm tra caption hình', 'audit figure thesis'. Codifies selection criteria (figure vs table vs prose), per-chapter numbering N.M, Vietnamese caption format, per-chapter INDEX generation. Pair với thesis-docx pipeline."
---

# Thesis Figure Curation

Skill chuẩn hoá việc curate figure (hình ảnh) + table + diagram cho thesis V1+. Closes C7 Diagram + figure rendering category audit gap per `thesis-content-standard.md`.

## Khi nào dùng

- Pre-merge PR thêm/sửa nội dung chapter thesis (`documents/<thesis-dir>/chapter-*.md`)
- Khi cần generate / refresh `chapter-N-INDEX.md` figure index
- Khi user hỏi "chương này có bao nhiêu hình", "caption có đầy đủ chưa", "đánh số hình thiếu chỗ nào"
- Trước khi assemble `thesis-vN.docx` — verify figure coverage + caption parity

## Process

1. **Audit hiện trạng** — chạy `bash scripts/audit-figures.sh <chapter-glob>` để liệt kê figure + caption coverage + numbering integrity
2. **Apply selection criteria** — dùng `reference/figure-selection-criteria.md` để quyết định visual nào nên là figure / table / prose
3. **Format caption** — dùng `reference/caption-format-vietnamese.md` template `**Hình N.M: <mô tả>**`
4. **Đánh số** — dùng `reference/numbering-scheme.md` (`<chapter>.<sequential>`, restart per chapter)
5. **Generate INDEX** — tạo / update `documents/<thesis-dir>/chapter-N-INDEX.md` từ audit output (table format § dưới)
6. **Cross-check citation** — verify mỗi figure được cite trong body text trong ±3 đoạn văn (heuristic audit script)

## Skill Contents

- `reference/figure-selection-criteria.md` — Quyết định figure vs table vs prose; chuẩn hoá Mermaid/PlantUML/ASCII/PNG; quality bar
- `reference/caption-format-vietnamese.md` — Template caption tiếng Việt + ví dụ + anti-pattern
- `reference/numbering-scheme.md` — Scheme `N.M`, restart per chapter, hybrid figure+table+diagram
- `scripts/audit-figures.sh` — Walk chapter MD → liệt kê figure count + caption coverage + numbering integrity + citation ±3 đoạn

## INDEX file format (`documents/<thesis-dir>/chapter-N-INDEX.md`)

```markdown
---
chapter: N
generated: YYYY-MM-DD
---

# Chapter N — Index hình ảnh + bảng

| # | Loại | File / Block | Caption | Vị trí |
|:-:|------|--------------|---------|--------|
| N.1 | Figure | `chapter-N-X.md` line LL | Hình N.1: ... | §N.X paragraph Y |
| N.2 | Table | inline `chapter-N-Y.md` line LL | Bảng N.2: ... | §N.Y |
| N.3 | Mermaid | block in `chapter-N-Y.md` line LL | Hình N.3: ... | §N.Y |

## Trạng thái figure cần bổ sung

- [ ] (none / list missing per audit)

## Last audit

`bash .claude/skills/quality/thesis-figure-curation/scripts/audit-figures.sh chapter-N-*.md` — output trong `data/last-run-chapter-N.json`
```

## Gotchas

- **Mermaid block đếm là 1 figure** — nhưng caption phải đứng ngay dưới fence ` ``` ` đóng (không phải trong fence)
- **Bảng vs Hình** — VN academic convention dùng `Bảng N.M` cho table + `Hình N.M` cho figure. Numbering tách rời (Bảng 2.1 độc lập Hình 2.1)
- **Restart per chapter** — Hình 1.1, 1.2, ..., 1.N → sang chương 2 lại bắt đầu Hình 2.1. KHÔNG đánh số liên tục cross-chapter
- **PNG paths** — figure PNG thường nằm rải rác (`documents/<thesis-dir>/figures/**`, `documents/06-diagrams/plantuml/*.png`, screenshots). Skill này KHÔNG migrate PNG files (out of scope) — chỉ index những gì có sẵn
- **Caption position** — caption đặt BÊN DƯỚI figure (italic, font 11pt khi render Word). KHÔNG ABOVE. Audit script flag misplaced caption
- **Citation window ±3 đoạn** — heuristic, không hoàn hảo; manual review khi figure được cite cross-section (vd Chapter 4 cite Hình 2.3) — coi là valid OK
- **Backup files exclude** — audit script skip pattern `*-backup-*.md` để không double-count
- **Mode tester** — chạy `bash scripts/audit-figures.sh --help` để xem usage; `--json` cho machine-readable output
- **Adapt to your project's path layout** — default `documents/<thesis-dir>/chapter-N-*.md`. Adjust scripts args nếu layout khác.

## Related

- `.claude/rules/diagram-format-selection.md` — Mermaid mặc định, ASCII ≤5 nodes, PlantUML cho C4
- `.claude/rules/thesis-content-standard.md` C7 Diagram+figure rendering category — this skill closes C7 audit gap
- `.claude/rules/vn-localization-audit-checklist.md` §2 — VN sample data + format conventions
- Sister skill: `quality/thesis-citation-extract/SKILL.md` (closes C3 Bibliography category)
- Sibling skill: `quality/ui-review/SKILL.md` (annotation style: mũi tên đỏ #dc2626 + viền vàng #facc15)
