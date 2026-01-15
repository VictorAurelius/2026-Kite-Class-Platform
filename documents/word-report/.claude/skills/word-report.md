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
| Script chính | `create_bao_cao_thuc_tap.py` |
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
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
python3 create_bao_cao_thuc_tap.py
```

### 2. Sửa nội dung
Mở `create_bao_cao_thuc_tap.py` và sửa các hàm:
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

## Quy tắc độ rộng cột bảng

**QUAN TRỌNG**: Khi tạo bảng trong báo cáo, LUÔN chỉ định `col_widths` để đảm bảo bảng đẹp mắt và dễ đọc.

### Nguyên tắc thiết kế độ rộng cột
1. **Tổng độ rộng**: Tổng độ rộng các cột nên khoảng 16cm (phù hợp với lề 3cm trái + 2cm phải)
2. **Cột số thứ tự (STT/#)**: 1.5 cm
3. **Cột nội dung ngắn** (từ viết tắt, code): 3-5 cm
4. **Cột nội dung dài** (mô tả, giải thích): 8-13 cm
5. **Cột trạng thái** (trước/sau): 5-6.5 cm

### Độ rộng cột đã chuẩn hóa
| Bảng | Mô tả | col_widths (cm) |
|------|-------|-----------------|
| Bảng 1.1 | Lĩnh vực hoạt động | [1.5, 5.5, 9.0] |
| Bảng 1.2 | Các loại thiết kế | [1.5, 5.5, 9.0] |
| Bảng 2.1-2.2 | Tài liệu tham chiếu | [1.5, 5.5, 9.0] |
| Bảng 2.3 | HTTP Methods | [4.0, 4.0, 8.0] |
| Bảng 2.4 | Cấu trúc Batch | [1.5, 5.5, 9.0] |
| Bảng 2.5 | AI Checker levels | [3.0, 5.0, 8.0] |
| Bảng 2.6 | Tiêu chí Level 1 | [1.5, 5.5, 9.0] |
| Bảng 3.1 | Kỹ năng trước/sau | [4.0, 5.5, 6.5] |
| Từ viết tắt | Abbreviations | [3.0, 13.0] |

### Ví dụ sử dụng
```python
# Bảng có cột STT + tiêu đề + mô tả
add_table_with_caption(doc, "Bảng X.X. Tiêu đề",
    ["STT", "Tiêu đề", "Mô tả"],
    rows,
    col_widths=[1.5, 5.5, 9.0]
)

# Bảng so sánh 3 cột đều
add_table_with_caption(doc, "Bảng X.X. So sánh",
    ["Kỹ năng", "Trước", "Sau"],
    rows,
    col_widths=[4.0, 5.5, 6.5]
)
```

## Tham chiếu
- Context đầy đủ: `CONTEXT_BAO_CAO_THUC_TAP.md`
- Nội dung gốc: `/mnt/e/batch-workspace/output/BAO_CAO_THUC_TAP_SORA.md`
