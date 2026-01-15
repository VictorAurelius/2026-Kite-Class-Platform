# Skill: Tạo Báo cáo Khảo sát KiteClass Word

## Mô tả
Tạo file Word (.docx) cho Báo cáo Kết quả Khảo sát KiteClass theo quy định ĐH GTVT.

## Trigger phrases
- "tạo báo cáo khảo sát"
- "create survey report"
- "xuất file khảo sát docx"
- "chạy script khảo sát"

## Files

| File | Path |
|------|------|
| Script chính | `create_survey_report.py` |
| Logo UTC | `logo_utc.png` |
| Output | `BAO_CAO_KHAO_SAT_KITECLASS.docx` |
| Context | `CONTEXT_BAO_CAO_KHAO_SAT.md` |
| Quy định | `Quy dinh trinh bay do an tot nghiep.pdf` |

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
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
python3 create_survey_report.py
```

### 2. Sửa nội dung khảo sát
Mở `create_survey_report.py` và sửa các hàm:
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

## Tham chiếu
- Context đầy đủ: `CONTEXT_BAO_CAO_KHAO_SAT.md`
- Kế hoạch khảo sát: `documents/plans/survey-interview-plan.md`
- Báo cáo thực tập: `create_word_report.py` (mẫu format)
