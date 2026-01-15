# Skill: Tạo Báo cáo Thực tập Word

## Mô tả
Tạo file Word (.docx) cho Báo cáo Thực tập Tốt nghiệp theo quy định ĐH GTVT.

## Trigger phrases
- "tạo báo cáo word"
- "create word report"
- "xuất file docx"
- "chạy script báo cáo"

## Files

| File | Path |
|------|------|
| Script chính | `create_word_report.py` |
| Logo UTC | `logo_utc.png` |
| Output | `BAO_CAO_THUC_TAP_SORA.docx` |
| Context | `CONTEXT_BAO_CAO_THUC_TAP.md` |
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
cd /mnt/e/batch-workspace/test-word
python3 create_word_report.py
```

### 2. Sửa nội dung
Mở `create_word_report.py` và sửa các hàm:
- `add_title_page()` - Trang bìa
- `add_content1()` - Nội dung 1: Giới thiệu
- `add_content2()` - Nội dung 2: Công việc
- `add_content3()` - Nội dung 3: Kết quả

### 3. Sửa thông tin sinh viên
Trong hàm `add_title_page()`, sửa biến `info`:
```python
info = [
    ("Giảng viên hướng dẫn", "ThS. [Tên GVHD]"),
    ("Sinh viên thực hiện", "[Họ và tên SV]"),
    ("Lớp", "[Tên lớp]"),
    ("Mã sinh viên", "[MSSV]"),
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
- Context đầy đủ: `CONTEXT_BAO_CAO_THUC_TAP.md`
- Nội dung gốc: `/mnt/e/batch-workspace/output/BAO_CAO_THUC_TAP_SORA.md`
