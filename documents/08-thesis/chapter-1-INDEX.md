---
chapter: 1
generated: 2026-05-23
audit_source: .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-1.json
---

# Chapter 1: Index hình ảnh + bảng

Chương 1 cluster gồm 3 file (không tính `-backup-*` files):
- `chapter-1-ai-techniques.md`, §1.4 AI techniques (1 figure)
- `chapter-1-competitor-analysis.md`, §1.1–§1.3 competitor analysis (5 screenshots)
- `chapter-1-vn-law-methodology.md`, VN law + methodology (0 figure)

## Hình ảnh (figure / diagram / screenshot)

| # | Loại | File / Block | Caption | Vị trí |
|:-:|------|--------------|---------|--------|
| 1.1 | Mermaid | `chapter-1-ai-techniques.md` line 30 | ⚠️ thiếu caption, đề xuất `**Hình 1.1: Kiến trúc kỹ thuật AI Branding (text-to-image generation)**` | §1.3.2.1 Kiến trúc kỹ thuật AI Branding |
| 1.2 | PNG screenshot | `chapter-1-competitor-analysis.md` line 54 | ⚠️ thiếu caption, đề xuất `**Hình 1.2: Giao diện trang chủ BeeClass**` | §1.3.2 BeeClass |
| 1.3 | PNG screenshot | `chapter-1-competitor-analysis.md` line 65 | ⚠️ thiếu caption, đề xuất `**Hình 1.3: Giao diện MISA AMIS Trường Học**` | §1.3.3 MISA AMIS Trường Học |
| 1.4 | PNG screenshot | `chapter-1-competitor-analysis.md` line 76 | ⚠️ thiếu caption, đề xuất `**Hình 1.4: Giao diện trang chủ Mona eLMS**` | §1.3.4 Mona eLMS |
| 1.5 | PNG screenshot | `chapter-1-competitor-analysis.md` line 87 | ⚠️ thiếu caption, đề xuất `**Hình 1.5: Giao diện trang chủ Easy Edu**` | §1.3.5 Easy Edu |
| 1.6 | PNG screenshot | `chapter-1-competitor-analysis.md` line 98 | ⚠️ thiếu caption, đề xuất `**Hình 1.6: Giao diện trang chủ DotB**` | §1.3.6 DotB |

## Bảng

| # | File / Block | Caption | Vị trí |
|:-:|--------------|---------|--------|
| không có | (chưa có bảng được đánh số `**Bảng 1.M**` chính thức) | không có |, |

> **Ghi chú:** `chapter-1-competitor-analysis.md §1.3.8` có "Bảng so sánh tổng hợp" rendered as markdown table nhưng chưa có caption `**Bảng 1.1**` chính thức. Đề xuất bổ sung khi refresh chapter.

## Listing

| # | File / Block | Caption | Vị trí |
|:-:|--------------|---------|--------|
| không có | (chưa có code listing >20 dòng được đánh số) | không có |, |

## Tóm tắt audit

- **Tổng visual block:** 6 (1 Mermaid + 5 PNG)
- **Caption coverage:** 0/6 (0%), ⚠️ **toàn bộ figure thiếu caption chuẩn `**Hình N.M: ...**`**
- **Numbering integrity:** N/A (chưa có figure được đánh số chính thức)
- **Citation coverage:** N/A, chưa có caption nên không thể audit citation
- **Bảng/Listing:** chưa có caption formal

## Trạng thái figure cần bổ sung

- [ ] Thêm caption `**Hình 1.1: Kiến trúc kỹ thuật AI Branding (text-to-image generation)**` sau Mermaid block line 30 trong `chapter-1-ai-techniques.md`
- [ ] Thêm caption `**Hình 1.2: Giao diện trang chủ BeeClass**` sau screenshot line 54 trong `chapter-1-competitor-analysis.md`
- [ ] Thêm caption `**Hình 1.3: Giao diện MISA AMIS Trường Học**` sau screenshot line 65
- [ ] Thêm caption `**Hình 1.4: Giao diện trang chủ Mona eLMS**` sau screenshot line 76
- [ ] Thêm caption `**Hình 1.5: Giao diện trang chủ Easy Edu**` sau screenshot line 87
- [ ] Thêm caption `**Hình 1.6: Giao diện trang chủ DotB**` sau screenshot line 98
- [ ] Thêm caption `**Bảng 1.1: Ma trận so sánh tính năng KiteHub vs BeeClass vs MISA vs Mona vs Easy Edu vs DotB**` cho bảng tổng hợp §1.3.8 trong `chapter-1-competitor-analysis.md`
- [ ] Verify citation trong body text cho mỗi figure (heuristic ±3 đoạn) sau khi thêm caption

## Last audit

```bash
bash .claude/skills/quality/thesis-figure-curation/scripts/audit-figures.sh --json \
  documents/08-thesis/chapter-1-ai-techniques.md \
  documents/08-thesis/chapter-1-competitor-analysis.md \
  documents/08-thesis/chapter-1-vn-law-methodology.md \
  > .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-1.json
```

Audit timestamp: 2026-05-23
