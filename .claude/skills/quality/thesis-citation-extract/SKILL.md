---
name: thesis-citation-extract
description: "Dùng khi user nói 'extract citations', 'trích dẫn luận văn', 'audit thesis cite', 'verify bibliography', 'orphan citation check', 'kiểm tra cite luận văn', hoặc khi review thesis chapter trước defense. Parse `[N]` cite keys từ thesis chapters + verify chéo với `documents/08-thesis/references/bibliography.md` → báo 3 bucket: matched / orphan-body (cite không có entry) / orphan-bib (entry không cite)."
user-invocable: true
---

# Thesis Citation Extract Skill

Tự động extract `[N]` citation keys từ thesis chapter markdown + verify chéo với canonical bibliography (`documents/08-thesis/references/bibliography.md`) theo IEEE format. Phát hiện 2 loại orphan: **broken refs** (cite trong body nhưng không có entry) + **dead weight** (entry trong bib nhưng không được cite). Output JSON cache vào `data/last-run.json` để track delta giữa các lần audit.

## Khi nào dùng

- Trước khi flip `documents/08-thesis/**` chapter PR sang DONE
- Audit thesis V1/V2/V3 trước defense GVHD/GVPB
- Wave thesis closure sub-task — verify cite hygiene
- Phát hiện citation drift sau khi user thêm/sửa bibliography

## Quy trình (≈3 phút)

1. **Extract** — chạy `bash scripts/extract-citations.sh <chapter-files...>` → output `filename:line:cite_key` cho mọi `[N]` / `[N, M]` / `[N]–[M]` trong body. Range expand thành cá nhân (`[5]–[7]` → 5, 6, 7); danh sách `[1, 4, 9]` tách 1+4+9.
2. **Verify** — chạy `bash scripts/verify-citations.sh <chapter-files...> <bibliography-path>` → so sánh tập cite (body) vs tập entry (bib). Output 3 nhóm + báo cáo summary stdout + ghi JSON cache vào `data/last-run.json`.
3. **Interpret** — đọc summary:
   - ✅ `matched: N` — cite có entry, OK
   - ⚠️ `orphan-body: N` — cite trong body nhưng KHÔNG có entry bib → **broken reference** (fix bib hoặc xóa cite)
   - ⚠️ `orphan-bib: N` — entry trong bib nhưng KHÔNG được cite → **dead weight** (xóa entry hoặc thêm cite trong body)
4. **Self-test** — `bash scripts/self-test.sh` validate logic trên 3 fixture (good / bad-orphan-body / bad-orphan-bib).

## Skill contents

- `reference/ieee-citation-rules.md` — quy ước IEEE in-text + bibliography flat list format (project canonical)
- `reference/orphan-detection.md` — định nghĩa 2 loại orphan + decision tree fix
- `scripts/extract-citations.sh` — parser regex `\[[0-9]+\]` với range expansion
- `scripts/verify-citations.sh` — set-diff body cites vs bibliography entries
- `scripts/self-test.sh` — synthetic fixtures verify extract + verify logic
- `data/last-run.json` — cache lần verify gần nhất (delta tracking)

## Gotchas

- **Bibliography flat list** — project bibliography đã renumber Wave 102.7.6 sang single sequential `[1]...[38]` flat (NOT per-chapter sections). Section headings `## Chapter N` chỉ là grouping cho reader; cite key độc lập section.
- **Range expansion** — `[5]–[7]` dùng em-dash `–` (U+2013), KHÔNG phải hyphen `-`. Parser support cả 2 (em-dash + hyphen) để phòng typo.
- **List separator** — `[1, 2]` dùng comma + space; parser tolerate `[1,2]` (no space) và `[1 ,2]` (trailing space).
- **Code block exclusion** — KHÔNG parse `[N]` trong code fence ```` ``` ```` (vd `array[1]` Java code). Parser skip lines giữa code fence markers.
- **Markdown link false-positive** — `[text](url)` link syntax có `[]` nhưng KHÔNG phải cite. Parser yêu cầu content trong `[]` PHẢI là số nguyên thuần (`^[0-9]+$`).
- **Bibliography entry regex** — entry line PHẢI bắt đầu chính xác `[N]` đầu line (không indent). Indent line bị skip vì có thể là continuation hoặc quote.
- **Backup file scope** — KHÔNG include `chapter-*-backup-YYYY-MM-DD.md` trong verify mặc định. Backup là snapshot, không phản ánh current cite state.
- **Out of scope: scope của skill này** — chỉ extract + verify, KHÔNG sửa source. Người dùng tự fix (sửa bib hoặc body) sau khi nhìn report.

## Related

- `documents/08-thesis/references/bibliography.md` — canonical bibliography (flat IEEE list 38 entries)
- `documents/08-thesis/references/CITATION-STYLE.md` — IEEE format spec (in-text + entry templates)
- `documents/08-thesis/references/cross-ref-audit-2026-05-19.md` — manual audit precedent Wave 102.4
- `.claude/rules/skill-conventions.md` — skill structure mandate
- GAP-647 — thesis bibliography IEEE (parent gap, this skill closes Step 3)
- GAP-655 — citation-extract skill (this skill IS the deliverable)
