# Skill: Tao Bao cao Thuc tap Word

## Mo ta
Tao file Word (.docx) cho Bao cao Thuc tap Tot nghiep theo mau "Huong dan trinh bay bao cao TTTN.pdf" - DH GTVT.

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
- Cong hoa xa hoi chu nghia Viet Nam
- Thong tin co so thuc tap
- Xac nhan sinh vien
- Cac muc nhan xet
- Diem thuc tap

### 4. Loi cam on

### 5. Muc luc + Danh muc hinh ve + Danh muc bang bieu

### 6. Danh muc tu viet tat

### 7. Noi dung chinh - 4 Chuong
1. GIOI THIEU CHUNG VE DON VI THUC TAP
   - 1.1. Thong tin chung ve don vi thuc tap
   - 1.2. Chuc nang, nhiem vu cua bo phan thuc tap
   - 1.3. Moi truong lam viec va quy trinh cong tac

2. NOI DUNG THUC TAP
   - 2.1. Muc tieu va yeu cau cua dot thuc tap
   - 2.2. Ke hoach thuc tap
   - 2.3. Cac cong viec da thuc hien
   - 2.4. Cong nghe, cong cu va ky thuat su dung

3. KET QUA VA DANH GIA
   - 3.1. Ket qua dat duoc trong qua trinh thuc tap
   - 3.2. Kien thuc va ky nang tich luy duoc
   - 3.3. Thuan loi va kho khan

4. NHAN XET VA DINH HUONG
   - 4.1. Nhan xet chung ve dot thuc tap
   - 4.2. Bai hoc kinh nghiem rut ra
   - 4.3. Dinh huong nghe nghiep va hoc tap sau thuc tap

### 8. Tai lieu tham khao (IEEE format)

### 9. Phu luc
- Nhat ky thuc tap
- Hinh anh, tai lieu minh chung
- San pham thuc tap

## Quy dinh format

### Can le
- Tren: 2cm, Duoi: 2cm
- Trai: 3cm, Phai: 2cm
- So trang: Giua, phia tren (header)

### Font chu (Times New Roman)
- Chuong: 14pt Bold, left
- Muc (1.1): 13pt Bold, left
- Tieu muc (1.1.1): 13pt Bold + Italic, left
- Doan van: 13pt, justify, indent 1.27cm, spacing 1.5

### Bang/Hinh
- Ten bang: phia TREN bang
- Ten hinh: phia DUOI hinh

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
    # ...
}

INTERNSHIP_INFO = {
    "company": "SY PARTNERS., JSC",
    "advisor": "ThS. Nguyen Duc Du",
    # ...
}
```

### 3. Sua noi dung cac chuong
Mo `create_bao_cao_thuc_tap.py` va sua cac ham:
- `add_chapter1()` - Gioi thieu don vi thuc tap
- `add_chapter2()` - Noi dung thuc tap
- `add_chapter3()` - Ket qua va danh gia
- `add_chapter4()` - Nhan xet va dinh huong

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

## Tham chieu
- Thong tin sinh vien: `.claude/skills/student-info.md`
- Context day du: `CONTEXT_BAO_CAO_THUC_TAP.md`
- Mau template: `Huong dan trinh bay bao cao TTTN.pdf`
