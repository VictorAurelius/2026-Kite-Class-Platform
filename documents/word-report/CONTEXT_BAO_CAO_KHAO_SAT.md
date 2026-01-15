# Context: Tạo Báo cáo Khảo sát KiteClass bằng Python

**Ngày tạo:** 2026-01-15
**Mục đích:** Lưu context để các session sau có thể tiếp cận nhanh

---

## 1. Tổng quan

Dự án tạo file Word (.docx) cho **Báo cáo Kết quả Khảo sát** của hệ thống **KiteClass Platform** theo quy định trình bày của **Trường Đại học Giao thông Vận tải (UTC)**.

### 1.1 Các file quan trọng

| File | Mô tả |
|------|-------|
| `create_survey_report.py` | Script Python chính tạo file Word |
| `logo_utc.png` | Logo trường UTC (dùng chung) |
| `BAO_CAO_KHAO_SAT_KITECLASS.docx` | File output (~215KB) |
| `Quy dinh trinh bay do an tot nghiep.pdf` | Tài liệu quy định format chuẩn |

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

## 3. Nội dung báo cáo khảo sát

### 3.1 Thông tin khảo sát

| Thông tin | Giá trị |
|-----------|---------|
| Đối tượng | 5 nhóm actors của KiteClass |
| Tổng số mẫu | 215 người (giả định) |
| Phương pháp | Online survey + Interview |
| Thời gian | Tháng 12/2025 - 01/2026 |

### 3.2 Phân bố mẫu khảo sát

| Actor | Số lượng | Tỷ lệ |
|-------|----------|-------|
| CENTER_OWNER | 12 | 5.6% |
| CENTER_ADMIN | 18 | 8.4% |
| TEACHER | 45 | 20.9% |
| STUDENT | 85 | 39.5% |
| PARENT | 55 | 25.6% |
| **Tổng** | **215** | **100%** |

### 3.3 Cấu trúc báo cáo

1. **Bìa** (không đánh số trang)
2. **Mục lục**
3. **Danh mục bảng biểu**
4. **Danh mục hình vẽ**
5. **Danh mục từ viết tắt**
6. **MỞ ĐẦU**
7. **CHƯƠNG 1**: Phương pháp khảo sát
8. **CHƯƠNG 2**: Kết quả khảo sát theo actor
9. **CHƯƠNG 3**: Phân tích và đề xuất
10. **KẾT LUẬN**
11. **TÀI LIỆU THAM KHẢO**
12. **PHỤ LỤC**

---

## 4. Kết quả khảo sát chính

### 4.1 Tính năng ưu tiên theo actor

| Tính năng | CENTER_OWNER | TEACHER | STUDENT | PARENT |
|-----------|--------------|---------|---------|--------|
| Quản lý đăng ký | 4.8 | 3.5 | 4.2 | 4.5 |
| Lịch học | 4.2 | 4.8 | 4.5 | 4.6 |
| Theo dõi tiến độ | 4.5 | 4.5 | 4.0 | 4.8 |
| Thanh toán | 4.9 | 3.2 | 3.8 | 4.3 |
| Giao tiếp | 4.0 | 4.6 | 4.3 | 4.7 |

### 4.2 User Personas

| Actor | Persona | Mô tả |
|-------|---------|-------|
| CENTER_OWNER | Nguyễn Văn A | Chủ trung tâm 10 năm, 5 chi nhánh |
| TEACHER | Trần Thị B | Giáo viên 5 năm, dạy IELTS |
| STUDENT | Lê Văn C | Sinh viên 20 tuổi, học lập trình |
| PARENT | Phạm Thị D | Phụ huynh có 2 con học tiếng Anh |

---

## 5. Cách sử dụng script

### 5.1 Chạy script

```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
python3 create_survey_report.py
```

### 5.2 Output

```
✓ Đã tạo file: BAO_CAO_KHAO_SAT_KITECLASS.docx
✓ Căn lề: Trái 3cm, Phải 2cm, Trên 2.5cm, Dưới 2.5cm
✓ Số trang: Giữa, phía trên đầu trang
✓ Chương: 18pt Bold, Mục: 16pt Bold, Tiểu mục: 14pt Bold
✓ Nội dung: 13pt, giãn dòng 1.2, thụt đầu dòng 1cm
```

### 5.3 Các hàm chính trong script

| Hàm | Mô tả |
|-----|-------|
| `add_title_page(doc)` | Tạo trang bìa theo mẫu UTC |
| `add_chapter_title(doc, text)` | Tiêu đề chương 18pt Bold |
| `add_section_title(doc, text)` | Tiêu đề mục 16pt Bold |
| `add_subsection_title(doc, text)` | Tiêu đề tiểu mục 14pt Bold |
| `add_paragraph_text(doc, text)` | Đoạn văn 13pt, justify |
| `add_table_with_caption(doc, ...)` | Bảng với caption phía trên |
| `add_bullet_list(doc, items)` | Danh sách bullet |
| `add_survey_result_table(doc, ...)` | Bảng kết quả khảo sát |

---

## 6. So sánh với báo cáo thực tập

| Tiêu chí | Báo cáo thực tập | Báo cáo khảo sát |
|----------|------------------|------------------|
| Script | create_word_report.py | create_survey_report.py |
| Output | BAO_CAO_THUC_TAP_SORA.docx | BAO_CAO_KHAO_SAT_KITECLASS.docx |
| Nội dung | Công việc thực tập | Kết quả khảo sát |
| Số chương | 3 chương | 3 chương |
| Đối tượng | 1 doanh nghiệp | 5 nhóm actors |

---

## 7. Những việc cần làm tiếp (TODO)

- [ ] Cập nhật dữ liệu khảo sát thực tế (thay thế dữ liệu giả định)
- [ ] Thêm biểu đồ trực quan cho kết quả
- [ ] Cập nhật thông tin sinh viên
- [ ] Kiểm tra lại format sau khi mở trong MS Word
- [ ] Tạo phiên bản tiếng Anh nếu cần

---

## 8. Tham chiếu tài liệu

### 8.1 Trong dự án

- `documents/plans/survey-interview-plan.md` - Kế hoạch khảo sát chi tiết
- `documents/plans/user-personas.md` - User personas đầy đủ
- `documents/specs/actor-requirements.md` - Yêu cầu theo từng actor

### 8.2 Báo cáo liên quan

- `create_word_report.py` - Script báo cáo thực tập (tham khảo format)
- `CONTEXT_BAO_CAO_THUC_TAP.md` - Context báo cáo thực tập

---

**Cập nhật lần cuối:** 2026-01-15
