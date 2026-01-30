# Skill: Tạo Báo cáo Khảo sát KiteClass Word

## Mô tả
Tạo file Word (.docx) cho Báo cáo Kết quả Khảo sát KiteClass theo quy định ĐH GTVT.

## Folder Structure
All survey report files are organized in: `documents/word-reports/bao-cao-khao-sat/`

## Trigger phrases
- "tạo báo cáo khảo sát"
- "create survey report"
- "xuất file khảo sát docx"
- "chạy script khảo sát"

## Files

| File | Path |
|------|------|
| Script chính | `bao-cao-khao-sat/create_bao_cao_khao_sat.py` |
| Output | `bao-cao-khao-sat/BAO_CAO_KHAO_SAT_KITECLASS.docx` |
| Context | `bao-cao-khao-sat/CONTEXT_BAO_CAO_KHAO_SAT.md` |
| Logo UTC | `templates/logo_utc.png` |
| Quy định | `templates/Quy dinh trinh bay do an tot nghiep.pdf` |

## Quy định format (ĐH GTVT)

### Căn lề
- Trên: 2.5cm, Dưới: 2.5cm
- Trái: 3cm, Phải: 2cm
- Số trang: Giữa, phía trên (header)

### Font chữ (Times New Roman)
- Chương: 18pt Bold, center
- Mục (1.1): 16pt Bold, left
- Tiểu mục (1.1.1): 14pt Bold, left
- Đoạn văn: 13pt, justify, indent 1cm, spacing 1.2

### Bảng/Hình
- Tên bảng: phía TRÊN bảng
- Tên hình: phía DƯỚI hình

## Actions

### 1. Chạy script tạo báo cáo
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-reports/bao-cao-khao-sat
python3 create_bao_cao_khao_sat.py
```

### 2. Sửa nội dung khảo sát
Mở `create_bao_cao_khao_sat.py` và sửa các hàm:
- `add_chapter1()` - Phương pháp khảo sát
- `add_chapter2()` - Kết quả khảo sát
- `add_chapter3()` - Phân tích và đề xuất

### 3. Cập nhật dữ liệu khảo sát
Trong các hàm chương, sửa dữ liệu bảng:
```python
# Ví dụ: Bảng phân bố mẫu
headers = ["Đối tượng", "Số lượng", "Tỷ lệ (%)"]
rows = [
    ["CENTER_OWNER", "12", "5.6"],
    ["CENTER_ADMIN", "18", "8.4"],
    # ... cập nhật số liệu thực tế
]
```

## Dependencies
```bash
pip install python-docx --user
```

## Troubleshooting

### Lỗi: externally-managed-environment
```bash
pip install python-docx --user
```

### Thiếu logo
```bash
curl -L -o logo_utc.png "https://cdn.haitrieu.com/wp-content/uploads/2022/03/Logo-Dai-Hoc-Giao-Thong-Van-Tai-UTC.png"
```

## Quy tắc độ rộng cột bảng

**QUAN TRỌNG**: Khi tạo bảng trong báo cáo, LUÔN chỉ định `col_widths` để đảm bảo bảng đẹp mắt và dễ đọc.

### Nguyên tắc thiết kế độ rộng cột
1. **Tổng độ rộng**: Tổng độ rộng các cột nên khoảng 16-17cm (phù hợp với lề 3cm trái + 2cm phải)
2. **Cột số thứ tự (STT)**: 0.94 - 1.18 cm
3. **Cột nội dung dài** (câu hỏi, mô tả): 7-10 cm
4. **Cột thông tin ngắn** (loại câu, mục đích): 2-4 cm
5. **Cột số liệu** (điểm, tỷ lệ): 2-4 cm

### Độ rộng cột đã chuẩn hóa
| Bảng | Mô tả | col_widths (cm) |
|------|-------|-----------------|
| Bảng 1.1 | Actors | [4.15, 6.65, 3.02, 2.78] |
| Bảng 1.2 | Timeline | [3.33, 2.22, 6.51, 4.52] |
| Bảng 1.3-1.6 | Bảng hỏi | [1.18, 9-10, 2.5-3, 2.5-3.5] |
| Bảng 1.7-1.8 | Phỏng vấn | [1.18, 7.5-8.5, 2.7-2.8, 4.5-5.5] |
| Bảng 2.1 | So sánh SP | [4.15, 4.15, 4.15, 4.15] |
| Bảng 2.2 | So sánh tính năng | [3.32, 3.32, 3.32, 3.32, 3.32] |
| Bảng 3.1 | Ma trận ưu tiên | [3.49, 6.19, 4.45, 2.46] |
| Bảng 3.2 | Đề xuất gói | [2.06, 3.65, 2.54, 8.33] |
| Từ viết tắt | Abbreviations | [2.70, 13.89] |
| Phụ lục | Danh sách PV | [0.94, 2.56, 3.79, 3.85, 5.83] |

### Ví dụ sử dụng
```python
# Bảng có cột STT + nội dung dài + 2 cột phụ
add_table_with_caption(doc, "Bảng X.X. Tiêu đề",
    ["STT", "Nội dung", "Loại", "Ghi chú"],
    rows,
    col_widths=[1.18, 9.80, 2.54, 3.60]
)

# Bảng so sánh cột đều nhau
add_table_with_caption(doc, "Bảng X.X. So sánh",
    ["Tiêu chí", "A", "B", "C"],
    rows,
    col_widths=[4.15, 4.15, 4.15, 4.15]
)
```

## Tham chiếu
- Context đầy đủ: `CONTEXT_BAO_CAO_KHAO_SAT.md`
- Kế hoạch khảo sát: `documents/plans/survey-interview-plan.md`
- Báo cáo thực tập: `create_bao_cao_thuc_tap.py` (mẫu format)
