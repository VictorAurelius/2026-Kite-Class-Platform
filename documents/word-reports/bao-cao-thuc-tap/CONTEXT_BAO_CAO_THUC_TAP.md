# Context: Tạo Báo cáo Thực tập Tốt nghiệp bằng Python

**Ngày tạo:** 2026-01-14
**Mục đích:** Lưu context để các session sau có thể tiếp cận nhanh

---

## 1. Tổng quan

Dự án tạo file Word (.docx) cho **Báo cáo Thực tập Tốt nghiệp** theo quy định trình bày của **Trường Đại học Giao thông Vận tải (UTC)**.

### 1.1 Các file quan trọng

| File | Mô tả |
|------|-------|
| `create_bao_cao_thuc_tap.py` | Script Python chính tạo file Word |
| `logo_utc.png` | Logo trường UTC (1200x1200, 170KB) |
| `BAO_CAO_THUC_TAP_SORA.docx` | File output (~215KB) |
| `Quy dinh trinh bay do an tot nghiep.pdf` | Tài liệu quy định format chuẩn |
| `Quy dinh trinh bay do an tot nghiep.docx` | File Word của quy định (có thể copy mẫu) |

### 1.2 Thư viện Python sử dụng

```bash
pip install python-docx --user
```

---

## 2. Quy định trình bày (từ PDF ĐH GTVT)

### 2.1 Căn lề và số trang

| Thành phần | Giá trị |
|------------|---------|
| Lề trên | 2.5 cm |
| Lề dưới | 2.5 cm |
| Lề trái | 3.0 cm |
| Lề phải | 2.0 cm |
| Số trang | Giữa, **phía trên** đầu trang (header) |

### 2.2 Font chữ và định dạng

| Thành phần | Font | Size | Style | Căn lề |
|------------|------|------|-------|--------|
| **Chương** | Times New Roman | 18pt | Bold | Center |
| **Mục (1.1)** | Times New Roman | 16pt | Bold | Left |
| **Tiểu mục (1.1.1)** | Times New Roman | 14pt | Bold | Left |
| **Đoạn văn** | Times New Roman | 13pt | Normal | Justify |
| Thụt đầu dòng | - | 1 cm | - | - |
| Giãn dòng | - | 1.2 lines | - | - |

### 2.3 Bảng biểu và hình vẽ

- **Tên bảng**: Phía **TRÊN** bảng, đậm
- **Tên hình**: Phía **DƯỚI** hình, đậm
- Đánh số theo chương: Bảng 1.1, Hình 2.3, ...

---

## 3. Bố cục trang bìa (theo mẫu trang 6 PDF)

```
        TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI     ← 14pt, không đậm
              KHOA CÔNG NGHỆ THÔNG TIN         ← 14pt, đậm, gạch chân
              ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

                    [LOGO UTC]                 ← 3.5cm width


          BÁO CÁO THỰC TẬP TỐT NGHIỆP         ← 26pt, đậm

                     ĐỀ TÀI                    ← 14pt

           THIẾT KẾ HỆ THỐNG PHẦN MỀM         ← 16pt, đậm
        TẠI DOANH NGHIỆP PHÁT TRIỂN PHẦN MỀM


    Giảng viên hướng dẫn   : TS. Nguyễn Đức Dư
    Sinh viên thực hiện    : [Họ và tên]       ← 14pt, căn trái
    Lớp                    : [Tên lớp]
    Mã sinh viên           : [MSSV]




                  Hà Nội – 2026                ← 14pt, đậm
```

**Lưu ý:** Trang bìa **KHÔNG CÓ KHUNG VIỀN**

---

## 4. Cấu trúc báo cáo thực tập

Theo file `maubaocaothuctap.png`:

1. **Bìa** (không đánh số trang)
2. **Mục lục**
3. **Danh mục bảng biểu**
4. **Danh mục hình vẽ**
5. **Danh mục từ viết tắt**
6. **MỞ ĐẦU**
7. **NỘI DUNG 1**: Giới thiệu đơn vị thực tập
8. **NỘI DUNG 2**: Công việc đã thực hiện
9. **NỘI DUNG 3**: Kết quả và đánh giá
10. **KẾT LUẬN**
11. **TÀI LIỆU THAM KHẢO**
12. **PHỤ LỤC**

---

## 5. Nội dung báo cáo (SORA Project)

### 5.1 Thông tin thực tập

| Thông tin | Giá trị |
|-----------|---------|
| Đơn vị thực tập | SY PARTNERS., JSC |
| Vị trí | Software Engineer |
| Dự án | SORA STEP4 |
| Công việc chính | Thiết kế hệ thống (DB, Screen, API, Batch) |

### 5.2 Các chủ đề chính trong báo cáo

1. **Giới thiệu SY Partners**: Công ty offshore development, thành lập 2022
2. **Quy trình thiết kế**: 10 bước từ tiếp nhận đến delivery
3. **Thiết kế DB**: Oracle Database, constraints, indexes
4. **Thiết kế màn hình**: Validation đơn lẻ và tương quan
5. **Thiết kế API**: RESTful, HTTP methods
6. **Thiết kế Batch**: Spring Batch, Chunk processing
7. **AI Checker**: Hệ thống QA tự động multi-level (Level 1-5)

---

## 6. Cách sử dụng script

### 6.1 Chạy script

```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
python3 create_bao_cao_thuc_tap.py
```

### 6.2 Output

```
✓ Đã tạo file: BAO_CAO_THUC_TAP_SORA.docx
✓ Căn lề: Trái 3cm, Phải 2cm, Trên 2.5cm, Dưới 2.5cm
✓ Số trang: Giữa, phía trên đầu trang
✓ Chương: 18pt Bold, Mục: 16pt Bold, Tiểu mục: 14pt Bold
✓ Nội dung: 13pt, giãn dòng 1.2, thụt đầu dòng 1cm
```

### 6.3 Các hàm chính trong script

| Hàm | Mô tả |
|-----|-------|
| `add_title_page(doc)` | Tạo trang bìa theo mẫu UTC |
| `add_chapter_title(doc, text)` | Tiêu đề chương 18pt Bold |
| `add_section_title(doc, text)` | Tiêu đề mục 16pt Bold |
| `add_subsection_title(doc, text)` | Tiêu đề tiểu mục 14pt Bold |
| `add_paragraph_text(doc, text)` | Đoạn văn 13pt, justify |
| `add_table_with_caption(doc, ...)` | Bảng với caption phía trên |
| `add_figure_placeholder(doc, caption)` | Placeholder hình với caption dưới |
| `add_bullet_list(doc, items)` | Danh sách bullet |
| `add_numbered_list(doc, items)` | Danh sách đánh số |

---

## 7. Các vấn đề đã giải quyết

### 7.1 Cài đặt python-docx

**Lỗi:** `externally-managed-environment`
**Giải pháp:** `pip install python-docx --user`

### 7.2 Tải logo UTC

**Nguồn:** https://cdn.haitrieu.com/wp-content/uploads/2022/03/Logo-Dai-Hoc-Giao-Thong-Van-Tai-UTC.png

```bash
curl -L -o logo_utc.png "https://cdn.haitrieu.com/wp-content/uploads/2022/03/Logo-Dai-Hoc-Giao-Thong-Van-Tai-UTC.png"
```

### 7.3 Bố cục trang bìa

- Ban đầu: Có khung viền, logo trước tên trường
- Sau khi sửa: Không khung viền, logo sau tên khoa (theo đúng PDF trang 6)

---

## 8. Những việc cần làm tiếp (TODO)

- [ ] Điền thông tin sinh viên thực tế (họ tên, lớp, MSSV)
- [ ] Cập nhật tên giảng viên hướng dẫn
- [ ] Thêm hình vẽ thực tế thay placeholder
- [ ] Tạo mục lục tự động trong Word
- [ ] Review và chỉnh sửa nội dung chi tiết
- [ ] Kiểm tra lại format sau khi mở trong MS Word

---

## 9. Tham chiếu tài liệu

### 9.1 Trong dự án

- `/mnt/e/batch-workspace/output/BAO_CAO_THUC_TAP_SORA.md` - Nội dung gốc dạng Markdown
- `/mnt/e/batch-workspace/output/CLAUDE_AGENTS_ANALYSIS_REPORT.md` - Phân tích hệ thống AI Checker
- `/mnt/e/batch-workspace/script/maubaocaothuctap.png` - Mẫu cấu trúc báo cáo

### 9.2 Tài liệu bên ngoài

- [python-docx documentation](https://python-docx.readthedocs.io/)
- [Logo UTC - Hải Triều](https://haitrieu.com/blogs/vector-logo-truong-dai-hoc-giao-thong-van-tai-utc/)

---

## 10. Claude Code Skills (nếu cần)

Để tạo skill cho việc này, có thể tạo file `.claude/skills/create-word-report/`:

```markdown
# Skill: Tạo báo cáo Word theo quy định ĐH GTVT

## Trigger
- "tạo báo cáo word"
- "create word report"
- "xuất báo cáo docx"

## Context
- Script: /mnt/e/person/2026-Kite-Class-Platform/documents/word-report/create_bao_cao_thuc_tap.py
- Output: BAO_CAO_THUC_TAP_SORA.docx
- Format: Theo quy định ĐH GTVT

## Actions
1. Đọc file create_bao_cao_thuc_tap.py để hiểu cấu trúc
2. Chỉnh sửa nội dung nếu cần
3. Chạy python3 create_bao_cao_thuc_tap.py
4. Kiểm tra file output
```

---

**Cập nhật lần cuối:** 2026-01-14 06:36 UTC
