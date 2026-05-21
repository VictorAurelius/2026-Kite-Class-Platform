---
title: documents/08-thesis/figures/ — Author-original diagrams for thesis render
audience: mixed
last-updated: 2026-05-21
---

# documents/08-thesis/figures/

## Purpose

Folder này chứa các author-original diagrams (do tác giả tự xây dựng) sẽ được render vào `thesis-v1.docx` thông qua pipeline `documents/08-thesis/create_thesis_v1.py`. Phạm vi gồm sơ đồ Business Requirements Document (BRD), sơ đồ thực thể quan hệ (ERD), sơ đồ kiến trúc AWS, sơ đồ luồng nghiệp vụ (sequence flow), và các minh hoạ kỹ thuật khác do tác giả tự thiết kế phục vụ trình bày báo cáo khoá luận tốt nghiệp.

Folder hiện được tạo trong Wave 102.7.5 Bucket C để chuẩn bị infrastructure trước khi populate nội dung trong Wave 102.7.6+. Theo `docs-subfolder-maturity.md` §2 Volume criterion, folder dự kiến chứa ≥5 files trong 30 ngày tới (BRD overview + ERD tenant-domain + AWS architecture + FE signup flow + FE dashboard flow tối thiểu), đáp ứng ngưỡng maturity cho phép tạo subdir mới.

## Directory map

| Đường dẫn | Mục đích | Số file dự kiến |
|---|---|:---:|
| `figures/` | Sơ đồ author-original (BRD/ERD/AWS architecture/sequence flows) | 4-8 trong 30 ngày |
| `figures/{chapter-num}-{topic}-{seq}.{ext}` | Sub-naming convention per file | — |

Sub-naming convention chi tiết:

| Pattern | Ví dụ thực tế | Áp dụng |
|---|---|---|
| `1-brd-{topic}-{seq}.png` | `1-brd-overview-01.png` | Chương 1 — Tổng quan + BRD |
| `2-erd-{domain}-{seq}.png` | `2-erd-tenant-domain-01.png` | Chương 2 — Kiến trúc hệ thống ERD |
| `2-aws-{topic}-{seq}.png` | `2-aws-architecture-01.png` | Chương 2 — Sơ đồ kiến trúc AWS |
| `3-{module}-{topic}-{seq}.png` | `3-payment-flow-01.png` | Chương 3 — Triển khai luồng nghiệp vụ |
| `4-fe-{flow}-{seq}.png` | `4-fe-signup-flow-01.png` | Chương 4 — Kết quả triển khai FE |

Quy ước đặt tên:
- `chapter-num` = số chương báo cáo (1-4)
- `topic` / `domain` / `module` / `flow` = từ khoá viết-thường-gạch-ngang mô tả nội dung sơ đồ
- `seq` = số thứ tự 2 chữ số (`01`, `02`, ...) bắt đầu mỗi topic
- `ext` = `png` (mặc định, ≥150 DPI), `svg` (nếu cần scale vô hạn cho rendering pipeline)

## File placement rules

Folder `figures/` chỉ dùng cho **author-original diagrams** — các sơ đồ do tác giả tự thiết kế dùng cho thesis. Cần phân biệt rõ với folder `documents/08-thesis/screenshots/` đã tồn tại từ trước:

| Folder | Loại nội dung | Ví dụ |
|---|---|---|
| `documents/08-thesis/figures/` (mới — Wave 102.7.5) | **Author-original diagrams** — sơ đồ tác giả tự xây dựng | BRD overview, ERD tenant-domain, AWS architecture, sequence flow nghiệp vụ |
| `documents/08-thesis/screenshots/` (tồn tại từ Wave 102.6) | **UI captures** — ảnh chụp giao diện sản phẩm KiteHub | Landing marketing page, Signup wizard step, Owner dashboard, Email template render |

Quy tắc bắt buộc:
- KHÔNG đặt UI screenshot vào `figures/` — UI captures phải đặt vào `screenshots/`
- KHÔNG đặt author-original diagram vào `screenshots/` — sơ đồ tự xây dựng phải đặt vào `figures/`
- Mỗi figure render vào thesis-v1.docx phải có caption `**Hình X.Y. <Mô tả>**` theo UTC convention §2.4
- Mỗi figure phải có dòng source attribution italic NGAY SAU caption theo `thesis-content-standard.md` §10 S8:
  - Sơ đồ tự xây dựng → `*Nguồn: tác giả tự xây dựng*` HOẶC `*Nguồn: tác giả tự xây dựng dựa trên [N, tr.NNN]*` (nếu phái sinh từ tham khảo)
  - UI screenshot → `*Nguồn: ảnh chụp giao diện KiteHub Platform, truy cập DD/MM/YYYY*`
- File PNG nên có resolution ≥150 DPI để in giấy chấp nhận được (test bằng `file <name>.png` hoặc image metadata viewer)

Đối với Wave 102.7.5 Bucket C: folder và README được tạo nhưng chưa populate file content nào. Việc thêm các figure cụ thể được defer sang Wave 102.7.6+ theo scope decision tại wave plan §3 Bucket C Task C1.

## Archive policy

Folder `figures/` tuân theo cùng archive policy với folder cha `documents/08-thesis/` — defer archive đến thesis ship lifecycle. Cụ thể:

- Trong giai đoạn active development (Wave 102.7.x → Wave 105+ defense): figures giữ ở `figures/` để pipeline `create_thesis_v1.py` đọc thực tế khi rebake thesis.
- Sau khi thesis V1 final được bảo vệ trước hội đồng (sau defense window 2026-08-15 → 2026-10-15): toàn bộ folder `documents/08-thesis/` có thể được archive sang `documents/07-archived/academic/thesis-2026-utc-cnpm/` cùng với compiled DOCX/PDF final.
- KHÔNG áp dụng cadence-based archival per `docs-archival-cadence.md` §2 cho figures cá nhân — chúng là active reference suốt thesis lifecycle. Folder `figures/` được coi như sister của `documents/08-thesis/references/`, `documents/08-thesis/screenshots/` cùng cycle.
- Khi figure cũ bị thay thế (rev 2, rev 3): file cũ có thể được rename `{name}-v1-archived.png` hoặc move sang `figures/_archived-rev/` subfolder (chỉ tạo khi có ≥3 file cần archive nội bộ).

Reference: `docs-folder-structure.md` §3 template, `docs-subfolder-maturity.md` §2 Volume criterion, `thesis-content-standard.md` §10 S8 figure source attribution.
