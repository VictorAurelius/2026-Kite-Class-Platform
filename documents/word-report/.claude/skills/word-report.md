# Skill: Tao Bao cao Thuc tap Word

## Mo ta
Tao file Word (.docx) cho Bao cao Thuc tap Tot nghiep theo mau "Huong dan trinh bay bao cao TTTN.pdf" - DH GTVT.

**Tinh nang tu dong:**
- Heading styles (Heading 1, 2, 3) cho tao Muc luc tu dong
- SEQ fields cho danh so Bang va Hinh tu dong
- IEEE format cho Tai lieu tham khao

## Trigger phrases
- "tao bao cao word"
- "create word report"
- "xuat file docx"
- "chay script bao cao"

## Files

| File | Path |
|------|------|
| Script chinh | `create_bao_cao_thuc_tap.py` |
| Logo UTC | `logo_utc.png` |
| Output | `BAO_CAO_THUC_TAP.docx` |
| Context | `CONTEXT_BAO_CAO_THUC_TAP.md` |
| Mau huong dan | `Huong dan trinh bay bao cao TTTN.pdf` |
| Thong tin SV | `.claude/skills/student-info.md` |

## Tinh nang tu dong hoa Word

### 1. Heading Styles (Tao Muc luc tu dong)
Script su dung Word built-in Heading styles:
- **Heading 1**: Tieu de chuong (1. GIOI THIEU...)
- **Heading 2**: Tieu de muc (1.1. Thong tin chung...)
- **Heading 3**: Tieu de tieu muc (1.1.1. Chi tiet...)

=> Trong Word: References > Table of Contents > Automatic Table

### 2. SEQ Fields (Danh so Bang/Hinh tu dong)
Script su dung SEQ fields giong nhu Insert > Caption trong Word:
- Bang: `SEQ Table{chapter}` -> Bang 2.1, Bang 2.2...
- Hinh: `SEQ Figure{chapter}` -> Hinh 1.1, Hinh 1.2...

=> Trong Word: References > Insert Table of Figures

**Luu y:** Sau khi mo file, bam **Ctrl+A** roi **F9** de cap nhat tat ca fields.

### 3. IEEE Bibliography Format
Tai lieu tham khao theo chuan IEEE:
- So thu tu trong ngoac vuong [1], [2]...
- Tac gia, "Tieu de," Nam. [Online]. Available: URL. [Accessed: Date].
- URL duoc to mau xanh

## Huong dan su dung file docx

### Tao Muc luc tu dong
1. Mo file `BAO_CAO_THUC_TAP.docx` trong MS Word
2. Dat con tro tai trang Muc luc
3. Vao **References > Table of Contents > Automatic Table 1**
4. Muc luc se tu dong tao tu cac Heading styles

### Tao Danh muc Bang
1. Dat con tro tai vi tri muon chen danh muc
2. Vao **References > Insert Table of Figures**
3. Chon **Caption label: Table** (hoac Bang)
4. Click OK

### Tao Danh muc Hinh ve
1. Dat con tro tai vi tri muon chen danh muc
2. Vao **References > Insert Table of Figures**
3. Chon **Caption label: Figure** (hoac Hinh)
4. Click OK

### Cap nhat tat ca Fields
Sau khi chinh sua noi dung:
1. Bam **Ctrl+A** (chon tat ca)
2. Bam **F9** (cap nhat fields)
3. Chon "Update entire table" neu co hoi

## Cau truc bao cao (theo mau moi)

### 1. Bia chinh
- Truong Dai hoc Giao thong Van tai
- Khoa Cong nghe thong tin (gach chan)
- Logo truong
- BAO CAO (gach chan)
- THUC TAP TOT NGHIEP
- CU NHAN (mau vang, gach chan)
- Bang thong tin sinh vien (khong co vien)

### 2. Bia phu
- Giong bia chinh nhung them truong "Don vi thuc tap"

### 3. Ban nhan xet cua co so thuc tap

### 4. Loi cam on

### 5. Muc luc + Danh muc hinh ve + Danh muc bang bieu

### 6. Danh muc tu viet tat

### 7. Noi dung chinh - 4 Chuong (dung Heading styles)
1. GIOI THIEU CHUNG VE DON VI THUC TAP (Heading 1)
   - 1.1. Thong tin chung... (Heading 2)
   - 1.2. Chuc nang, nhiem vu... (Heading 2)
   - 1.3. Moi truong lam viec... (Heading 2)

2. NOI DUNG THUC TAP (Heading 1)
   - 2.1. Muc tieu va yeu cau... (Heading 2)
   - 2.2. Ke hoach thuc tap (Heading 2)
   - 2.3. Cac cong viec da thuc hien (Heading 2)
     - 2.3.1. Thiet ke CSDL (Heading 3)
     - 2.3.2. Thiet ke man hinh (Heading 3)
   - 2.4. Cong nghe, cong cu... (Heading 2)

3. KET QUA VA DANH GIA (Heading 1)
   - 3.1. Ket qua dat duoc... (Heading 2)
   - 3.2. Kien thuc va ky nang... (Heading 2)
   - 3.3. Thuan loi va kho khan (Heading 2)

4. NHAN XET VA DINH HUONG (Heading 1)
   - 4.1. Nhan xet chung... (Heading 2)
   - 4.2. Bai hoc kinh nghiem... (Heading 2)
   - 4.3. Dinh huong nghe nghiep... (Heading 2)

### 8. TAI LIEU THAM KHAO (Heading 1, IEEE format)

### 9. PHU LUC (Heading 1)

## Actions

### 1. Chay script tao bao cao
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
python3 create_bao_cao_thuc_tap.py
```

### 2. Sua thong tin sinh vien
Sua trong file `create_bao_cao_thuc_tap.py`:
```python
STUDENT_INFO = {
    "name": "Nguyen Van Kiet",
    "student_id": "221230890",
    "class": "CNTT1-K63",
    "course": "63",
    "major": "Cong nghe thong tin",
}

INTERNSHIP_INFO = {
    "company": "SY PARTNERS., JSC",
    "advisor": "ThS. Nguyen Duc Du",
}
```

### 3. Them bang moi voi SEQ field
```python
add_table_with_caption(doc,
    chapter_num=2,           # So chuong
    caption_text="Ten bang",  # Noi dung caption
    headers=["Col1", "Col2"],
    rows=[("A", "B")],
    col_widths=[5.0, 11.0]
)
```

### 4. Them hinh moi voi SEQ field
```python
add_figure_placeholder(doc,
    chapter_num=2,           # So chuong
    caption_text="Ten hinh"  # Noi dung caption
)
```

### 5. Them tai lieu tham khao IEEE
```python
add_ieee_reference(doc,
    ref_num=6,
    author="Author Name",
    title="Document Title",
    source_type="Online",
    year="2024",
    url="https://example.com",
    accessed="Jan. 2026"
)
```

## Quy dinh format

### Can le
- Tren: 2cm, Duoi: 2cm
- Trai: 3cm, Phai: 2cm
- So trang: Giua, phia tren (header)

### Font chu (Times New Roman)
- Heading 1 (Chuong): 14pt Bold, left
- Heading 2 (Muc): 13pt Bold, left
- Heading 3 (Tieu muc): 13pt Bold + Italic, left
- Normal (Doan van): 13pt, justify, indent 1.27cm, spacing 1.5

### Bang/Hinh
- Caption bang: phia TREN bang, dung SEQ field
- Caption hinh: phia DUOI hinh, dung SEQ field

## Dependencies
```bash
pip install python-docx --user
```

## Troubleshooting

### Loi: externally-managed-environment
```bash
pip install python-docx --user
```

### Thieu logo
```bash
curl -L -o logo_utc.png "https://cdn.haitrieu.com/wp-content/uploads/2022/03/Logo-Dai-Hoc-Giao-Thong-Van-Tai-UTC.png"
```

### Muc luc/Danh muc khong cap nhat
1. Bam Ctrl+A (chon tat ca)
2. Bam F9 (cap nhat fields)
3. Chon "Update entire table"

## Tham chieu
- Thong tin sinh vien: `.claude/skills/student-info.md`
- Context day du: `CONTEXT_BAO_CAO_THUC_TAP.md`
- Mau template: `Huong dan trinh bay bao cao TTTN.pdf`
