# Thesis Reference Materials

Reference materials for the Kite Class Platform graduation thesis project.

## Purpose

This directory provides structured reference materials that map directly to thesis chapters, making it easy to extract relevant content for the written thesis document.

## Structure

```
08-thesis/
├── README.md                    — This file (index + purpose)
├── chapter-mapping.md           — Maps existing docs → thesis chapters
└── references/
    ├── technology-stack.md      — Technology choices + rationale
    ├── methodology.md           — Development methodology (Superpowers, Agile, TDD)
    ├── testing-results.md       — Test results summary across all services
    ├── quality-metrics.md       — Quality audit score timeline
    └── deployment-guide.md      — Cloud deployment instructions
```

## How to Use

1. Start with `chapter-mapping.md` to identify which documents correspond to each thesis chapter
2. Use files in `references/` for supplementary data (metrics, methodology details, deployment)
3. Cross-reference with primary docs in `01-business/`, `02-architecture/`, etc.

## Pre-defense ship checklist (Word post-process)

Sau khi re-render `thesis-v1.docx` qua Python pipeline, các trường SEQ + TOC + Danh mục Hình/Bảng/Thuật ngữ/Từ viết tắt được embed ở dạng **placeholder text** (vd `(Bấm Ctrl+A rồi F9 để cập nhật)`). Reader/committee mở file trên Microsoft Word PHẢI thực hiện post-process sau:

1. Mở `thesis-v1.docx` bằng Microsoft Word (KHÔNG LibreOffice — F9 trên LibreOffice không update SEQ field giống Word).
2. Bấm `Ctrl+A` để select toàn bộ document.
3. Bấm `F9` để cập nhật mọi field (TOC + Danh mục + Bảng X.Y + Hình X.Y SEQ numbering).
4. (Tuỳ chọn) Bấm `Ctrl+A` lần 2 + `F9` lần 2 để đảm bảo nested fields cập nhật (TOC reference SEQ fields).
5. Save (`Ctrl+S`) — file sẵn sàng in.

**Lý do:** Python pipeline sinh DOCX với XML field codes nhưng chưa "execute" để render thành text values. Word's F9 command thực thi field codes → populate text content. Same approach as professional thesis templates dùng tại UTC + các trường đại học khác.

**Verification trước khi ship:** TOC + Danh mục hiện đầy đủ section names + page numbers (không còn placeholder text); Bảng/Hình captions numbering sequential (Bảng 1.1, Bảng 1.2... Hình 1.1, Hình 1.2...).

---

## Related Documents

| Directory | Content |
|-----------|---------|
| `01-business/` | Business rules, domain logic |
| `02-architecture/` | System architecture, design decisions |
| `03-planning/` | Implementation plans, wave strategy |
| `04-quality/` | Quality audits, completion checks |
| `05-guides/` | Operations and user guides |
| `06-diagrams/` | PlantUML diagrams (use case, ERD, class, architecture) |
| `07-archived/` | Research materials (competitive analysis, technology evaluation) |
