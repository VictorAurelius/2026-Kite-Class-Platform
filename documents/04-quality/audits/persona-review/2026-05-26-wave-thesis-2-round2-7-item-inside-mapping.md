---
title: Wave thesis-2 Round 2 — 7-item user inside checklist
status: in-progress
created: 2026-05-26
phase: phase-1-beta
wave: thesis-2
audience: dev
tag_primary: thesis
tags_secondary: [content-quality, docx-render]
---

# Wave thesis-2 Round 2 — 7-item user inside checklist

## Source

User direction 2026-05-26 (post batch PR #1868 review):

```
1. documents/image-2.png: DANH MỤC BẢNG BIỂU chưa page break
   caption của hình vẽ quá dài => tại sao không tự bắt được bug này
2. 1.3 Công nghệ và công cụ sử dụng => bỏ phần này, không cần thiết
3. documents/image-3.png => 2 sơ đồ dạng ngang nên bé, khó nhìn, cần tối ưu lại
4. documents/image-4.png => ảnh được paste nguyên khi render docx, chưa căn chỉnh
   cho hợp lý, khớp trang, dễ nhìn => áp dụng lại với tất cả các ảnh
5. documents/image-5.png => tương tự, hình Hình 2.49. Luồng xác thực JWT và truyền
   ngữ cảnh tenant quá bé trong trang a4, cân nhắc vẽ dạng khác hoặc căn chỉnh hợp lý
6. 4.3 KPI Metrics + Measurement Plan và 4.4 Beta Tenant Scope + Limitations đã chốt
   bỏ đi rồi mà nhỉ
7. KẾT LUẬN VÀ KIẾN NGHỊ => chỉ là KẾT LUẬN thôi
```

## Checklist

| # | Item | Pipeline area | Status | Notes |
|---|---|---|---|---|
| 1a | DANH MỤC BẢNG BIỂU thiếu page break | `add_list_of_figures_tables()` | ⏳ pending | Add `doc.add_page_break()` trước heading |
| 1b | Caption hình quá dài | `add_seq_caption()` + source MDs | ⏳ pending | Auto-wrap captions OR truncate; quality check tool add length check |
| 1c | Quality check tool không bắt long caption | `scripts/check-thesis-docx-quality.sh` | ⏳ pending | Add check #10: caption length ≤80 chars (visual A4 fit) |
| 2 | Bỏ §1.3 Công nghệ và công cụ sử dụng | Pipeline `CHAPTER_FILES[1]` + `CHAPTER_TITLES[1]` + chapter-1-ai-techniques.md | ⏳ pending | Remove ai-techniques từ CHAPTER_FILES[1]; update title drop "và các công nghệ, công cụ" |
| 3 | 2 sơ đồ ngang bé khó nhìn | `add_image_inline()` width sizing | ⏳ pending | Detect aspect ratio (landscape) + use wider page-fit |
| 4 | Ảnh paste nguyên, chưa căn chỉnh | `add_image_inline()` smart sizing all images | ⏳ pending | Compute optimal width via Pillow image dim read, cap to A4 body (16cm) |
| 5 | Hình 2.49 quá bé | Same as Item 4 — pipeline-wide image sizing | ⏳ pending | Same fix as #4 — applies to all figures |
| 6 | §4.3 KPI Metrics + §4.4 Beta Tenant Scope bỏ | `chapter-4-deployment-results.md` | ⏳ pending | Verify state + ensure removed |
| 7 | KẾT LUẬN VÀ KIẾN NGHỊ → chỉ KẾT LUẬN | `add_conclusion()` line ~1477 | ⏳ pending | Change heading text |

## Sub-mapping per Bucket

| Cluster | Items | Effort |
|---|---|---|
| **Pipeline render quality** | 1a, 3, 4, 5 | Image sizing + page break — substantial pipeline work |
| **Content structure** | 2, 7 | CHAPTER_TITLES + CHAPTER_FILES update + heading text |
| **Tool enhancement** | 1b, 1c | check-thesis-docx-quality.sh extension (caption length check) |
| **Content verify** | 6 | chapter-4 source MD grep + verify removed |

## Outside-in audit decision

Per `outside-in-coverage-trigger.md` v1.1.0 §4 exception "User explicit scope locked, just execute current scope" — user fix-list specific items review of docx, không phải new feature scope. Skip outside-in audit + execute inside-out.

## Notes — khung primary §1 deviation

Items 2 + 7 override khung primary §1:
- **Khung Ch.1** = "Tổng quan về bài toán và các công nghệ, công cụ" với 3 sub (Hiện trạng / Bài toán / Công nghệ, công cụ sử dụng)
- **User override Item 2:** drop §1.3 → Ch.1 chỉ còn 2 sub-sections (Hiện trạng / Bài toán)
- **Khung Ch.4 §4.4** = "Kết luận, kiến nghị + Phương hướng phát triển + Kiến nghị"
- **User override Item 7:** drop "KIẾN NGHỊ" — chỉ "KẾT LUẬN"

User direction trumps khung in iterative review. Document deviation trong rule `thesis-content-standard.md` v3.0.0 future bump (or annotate as exception).

## Related

- Batch PR: #1868 (Wave thesis-2 content batch)
- Source: `documents/action-2.md` lines 83-92 user-flagged 2026-05-26
- Rule: `.claude/rules/thesis-content-standard.md` v2.0.0
- Previous Round 1 audit: `documents/04-quality/audits/persona-review/2026-05-20-wave-102.7-14-item-inside-mapping.md`
- Khung primary: `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png`
- Wave plan: `documents/03-planning/waves/wave-2026-05-26-thesis-2-fix-khung-chuan.md` (Wave thesis-2 OPEN iterative)
