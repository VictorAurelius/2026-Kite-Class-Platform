---
title: Pre-defense thesis-v*.docx checklist — manual F9 + sanity review
audience: dev
last-updated: 2026-05-26
status: stable
---

# Pre-defense thesis-v*.docx checklist

Manual steps before submitting thesis docx cho academic defense. Run lần cuối khi file đã ready (post-Bucket-A.4 re-bake hoặc subsequent revisions).

---

## 1. F9 update fields (CRITICAL — không tự động được)

Pipeline `create_thesis_v1.py` set TOC + danh mục bảng + danh mục hình via `auto_populate_fields()` qua LibreOffice headless nếu available. NHƯNG nếu LibreOffice không sẵn (vd Windows-only review machine), fields render placeholder text "(Bấm Ctrl+A rồi F9...)" thay vì populated.

**Bước manual F9 trong Word/LibreOffice:**

1. Mở `thesis-v1.docx` trong Microsoft Word (preferred) hoặc LibreOffice Writer
2. **Ctrl+A** (select all)
3. **F9** (update fields)
4. Khi prompt "Update entire table" hiển thị → chọn **Update entire table** cho mỗi field
5. Lặp lại Ctrl+A + F9 lần 2 (đảm bảo cross-references update theo TOC numbering)
6. Save file (Ctrl+S) — giữ format `.docx`

Verify post-F9:
- Mục lục populated với page numbers thực
- Danh mục bảng list "Bảng X.Y. ..." với page numbers
- Danh mục hình list "Hình X.Y. ..." với page numbers
- Cross-refs "xem Bảng X.Y" / "xem Hình X.Y" trong narrative point đúng page

---

## 2. Manual sanity review (visual check)

Open docx → scroll through and verify:

| Item | Check |
|---|---|
| **Trang bìa** | Logo UTC visible (NOT `[LOGO UTC]` placeholder text); info table 6-field đúng |
| **Bìa phụ** | KHÔNG có logo (per khung primary); info table khác bìa chính content |
| **Sequence** | Bìa → Bìa phụ → Lời cảm ơn → Mục lục → Danh mục viết tắt+thuật ngữ → Danh mục bảng → Danh mục hình → Mở đầu → Ch.1-4 → Kết luận → TLTK (khớp khung) |
| **No banned sections** | Verify KHÔNG có LỜI CAM ĐOAN / TÓM TẮT (VN abstract page riêng) / ABSTRACT (EN page riêng) / NHẬN XÉT GVHD (per khung) |
| **Heading numbering** | Chương 1: §1.1 Hiện trạng / §1.2 Bài toán / §1.3 Công nghệ và công cụ sử dụng (3 sub-sections khớp khung) |
| **Chapter titles** | Ch.1 "Tổng quan về bài toán và các công nghệ, công cụ" / Ch.2 "Phân tích và thiết kế hệ thống" / Ch.3 "Phân tích, thiết kế và triển khai hệ thống" / Ch.4 "Đánh giá kết quả và Kết luận" |
| **Page count** | Cử nhân target 60-80 trang; soft deduct 81-90; auto-FAIL >90 per `thesis-content-standard.md` §4 |
| **Margins** | T=2.5 / B=2.5 / L=3.0 / R=2.0 cm per UTC §2.1 |
| **Font** | Times New Roman 13pt body / 14pt H3 / 16pt H2 / 18pt H1 per UTC §2.2 |
| **Page numbers** | Header center top, arabic 1, 2, 3... bắt đầu từ Mở đầu (Bìa+Bìa phụ không có; Lời cảm ơn+Mục lục+Danh mục dùng La-mã i, ii, iii) |
| **Bibliography** | "TÀI LIỆU THAM KHẢO" heading + IEEE format hanging indent + ≥30 entries cử nhân + hyperlinks blue underline |
| **No project jargon** | grep visible text — không có "BETA" / "GA" / "Phase 1" / "Wave N" / "GAP-XXX" / "Claude" / `.claude/rules/` paths |

---

## 3. Print preview (Ctrl+P)

Verify in print preview mode:
- A4 portrait orientation
- Margins consistent across pages
- No orphan/widow paragraphs (heading at bottom of page + content overflow)
- Tables không bị cắt giữa pages
- Figures với captions cùng page (không split)
- Page numbers visible mỗi page (except Bìa + Bìa phụ)

---

## 4. Convert to PDF (final submission format)

Cho academic submission qua trang đăng ký đồ án trường UTC, file cuối cùng cần PDF format:

**Microsoft Word:** File → Save As → PDF format

**LibreOffice Writer:** File → Export As → Export Directly as PDF

**Command-line (Linux/Mac/Termux LibreOffice):**

```bash
libreoffice --headless --convert-to pdf documents/08-thesis/thesis-v1.docx --outdir documents/08-thesis/
```

Verify PDF post-conversion:
- Mở PDF → spot-check 5-10 pages random → format intact (fonts/spacing/figures)
- File size reasonable (cử nhân ~5-15 MB; >30 MB = embed unnecessary high-res images)

---

## 5. Backup before submission

```bash
cp documents/08-thesis/thesis-v1.docx documents/08-thesis/thesis-v1-final-$(date +%Y%m%d).docx
cp documents/08-thesis/thesis-v1.pdf documents/08-thesis/thesis-v1-final-$(date +%Y%m%d).pdf
```

Or commit to repo:

```bash
git add documents/08-thesis/thesis-v1.docx documents/08-thesis/thesis-v1.pdf
git commit -m "thesis-v1 final submission YYYY-MM-DD"
git push
```

---

## Related

- Pipeline: `documents/08-thesis/create_thesis_v1.py`
- Khung primary: `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png`
- Rule: `.claude/rules/thesis-content-standard.md` v2.0.0 §4 page count cap + §3 banned non-khung sections
- UTC spec: `documents/07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.pdf`
