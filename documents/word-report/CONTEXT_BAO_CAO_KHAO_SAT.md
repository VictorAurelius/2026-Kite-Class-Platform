# Context: Tạo Báo cáo Khảo sát KiteClass bằng Python

**Ngày tạo:** 2026-01-15
**Cập nhật:** 2026-01-15
**Mục đích:** Lưu context để các session sau có thể tiếp cận nhanh

---

## 1. Tổng quan

Dự án tạo file Word (.docx) cho **Báo cáo Kết quả Khảo sát** của hệ thống **KiteClass Platform** theo quy định trình bày của **Trường Đại học Giao thông Vận tải (UTC)**.

### 1.1 Các file quan trọng

| File | Mô tả |
|------|-------|
| `create_bao_cao_khao_sat.py` | Script Python chính tạo file Word (~1350 dòng) |
| `logo_utc.png` | Logo trường UTC (dùng chung) |
| `BAO_CAO_KHAO_SAT_KITECLASS.docx` | File output (~224KB) |
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

### 3.3 Cấu trúc báo cáo (đã cập nhật)

1. **Bìa** (không đánh số trang)
2. **Mục lục**
3. **Danh mục bảng biểu** (15 bảng)
4. **Danh mục hình vẽ** (7 hình)
5. **Danh mục từ viết tắt**
6. **MỞ ĐẦU**
7. **NỘI DUNG 1**: Kế hoạch khảo sát
   - 1.1 Đối tượng khảo sát (5 actors)
   - 1.2 Kế hoạch khảo sát chi tiết (timeline 8 tuần)
   - 1.3 Bảng hỏi khảo sát chi tiết (4 bảng hỏi: CENTER_OWNER, TEACHER, STUDENT, PARENT)
   - 1.4 Kịch bản phỏng vấn chi tiết (câu hỏi phỏng vấn + hướng dẫn)
8. **NỘI DUNG 2**: Khảo sát sản phẩm cạnh tranh và kết quả
   - 2.1 Khảo sát 3 sản phẩm tương tự (BeeClass, Edupage, ClassIn)
   - 2.2 Bảng so sánh tính năng
   - 2.3 Kết quả khảo sát từ 215 responses + 18 phỏng vấn
9. **NỘI DUNG 3**: Phân tích và đề xuất
   - 3.1 Key Insights và điểm khác biệt
   - 3.2 Feature Prioritization
   - 3.3 User Personas
   - 3.4 Chiến lược Go-to-Market và Roadmap
10. **KẾT LUẬN**
11. **TÀI LIỆU THAM KHẢO**
12. **PHỤ LỤC** (Link forms, Checklist phỏng vấn, Danh sách phỏng vấn)

---

## 4. Khảo sát sản phẩm cạnh tranh

### 4.1 Danh sách sản phẩm được khảo sát

| Sản phẩm | Quốc gia | Website | Điểm mạnh | Điểm yếu |
|----------|----------|---------|-----------|----------|
| BeeClass | Việt Nam | beeclass.net | Tiếng Việt, Zalo | Giao diện cũ |
| Edupage | Slovakia | edupage.org | Đa tính năng, Mobile | Phức tạp |
| ClassIn | Trung Quốc | classin.com | Live streaming | Không quản lý học phí |

### 4.2 Điểm khác biệt của KiteClass

- Gamification (điểm thưởng, huy hiệu) - chưa đối thủ nào có
- Multi-tenant SaaS architecture
- Tích hợp đa kênh: Zalo + App + SMS + Email
- UI/UX hiện đại, mobile-first

---

## 5. Bảng hỏi khảo sát

### 5.1 Cấu trúc bảng hỏi

| Actor | Số câu | Thời gian | Các phần |
|-------|--------|-----------|----------|
| CENTER_OWNER | 15 câu | 7-10 phút | A. Thông tin, B. Thực trạng, C. Nhu cầu, D. Chi trả |
| TEACHER | 12 câu | 5-7 phút | A. Cá nhân, B. Giảng dạy, C. Nhu cầu |
| STUDENT | 11 câu | 5 phút | A. Cá nhân, B. Học tập, C. Gamification |
| PARENT | 10 câu | 5 phút | A. Thông tin, B. Theo dõi, C. Thanh toán |

### 5.2 Câu hỏi phỏng vấn

| Actor | Số câu | Thời lượng | Focus |
|-------|--------|------------|-------|
| CENTER_OWNER | 10 câu | 30-45 phút | Pain points, Workflow, Tool satisfaction |
| TEACHER | 6 câu | 20-30 phút | Teaching workflow, Tech adoption |

---

## 6. Kết quả khảo sát chính

### 6.1 Tính năng ưu tiên theo actor

| Tính năng | CENTER_OWNER | TEACHER | STUDENT | PARENT |
|-----------|--------------|---------|---------|--------|
| Quản lý đăng ký | 4.8 | 3.5 | 4.2 | 4.5 |
| Lịch học | 4.2 | 4.8 | 4.5 | 4.6 |
| Theo dõi tiến độ | 4.5 | 4.5 | 4.0 | 4.8 |
| Thanh toán | 4.9 | 3.2 | 3.8 | 4.3 |
| Giao tiếp | 4.0 | 4.6 | 4.3 | 4.7 |

### 6.2 User Personas

| Actor | Persona | Mô tả |
|-------|---------|-------|
| CENTER_OWNER | Nguyễn Văn A | Chủ trung tâm 10 năm, 5 chi nhánh |
| TEACHER | Trần Thị B | Giáo viên 5 năm, dạy IELTS |
| STUDENT | Lê Văn C | Sinh viên 20 tuổi, học lập trình |
| PARENT | Phạm Thị D | Phụ huynh có 2 con học tiếng Anh |

---

## 7. Cách sử dụng script

### 7.1 Chạy script

```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
python3 create_bao_cao_khao_sat.py
```

### 7.2 Output

```
✓ Đã tạo file: BAO_CAO_KHAO_SAT_KITECLASS.docx
✓ Căn lề: Trái 3cm, Phải 2cm, Trên 2.5cm, Dưới 2.5cm
✓ Số trang: Giữa, phía trên đầu trang
✓ Chương: 18pt Bold, Mục: 16pt Bold, Tiểu mục: 14pt Bold
✓ Nội dung: 13pt, giãn dòng 1.2, thụt đầu dòng 1cm
```

### 7.3 Các hàm chính trong script

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

## 8. So sánh với báo cáo thực tập

| Tiêu chí | Báo cáo thực tập | Báo cáo khảo sát |
|----------|------------------|------------------|
| Script | create_bao_cao_thuc_tap.py | create_bao_cao_khao_sat.py |
| Output | BAO_CAO_THUC_TAP_SORA.docx | BAO_CAO_KHAO_SAT_KITECLASS.docx |
| Nội dung | Công việc thực tập | Kết quả khảo sát |
| Số chương | 3 chương | 3 chương |
| Đối tượng | 1 doanh nghiệp | 5 nhóm actors |

---

## 9. Những việc cần làm tiếp (TODO)

- [ ] Cập nhật dữ liệu khảo sát thực tế (thay thế dữ liệu giả định)
- [ ] Thêm biểu đồ trực quan cho kết quả
- [ ] Cập nhật thông tin sinh viên
- [ ] Kiểm tra lại format sau khi mở trong MS Word
- [ ] Tạo phiên bản tiếng Anh nếu cần

---

## 10. Tham chiếu tài liệu

### 10.1 Trong dự án

- `documents/plans/survey-interview-plan.md` - Kế hoạch khảo sát chi tiết
- `documents/plans/user-personas.md` - User personas đầy đủ
- `documents/specs/actor-requirements.md` - Yêu cầu theo từng actor

### 10.2 Báo cáo liên quan

- `create_bao_cao_thuc_tap.py` - Script báo cáo thực tập (tham khảo format)
- `CONTEXT_BAO_CAO_THUC_TAP.md` - Context báo cáo thực tập

---

**Cập nhật lần cuối:** 2026-01-15
