---
title: Thesis V1 — canonical metadata (sinh viên + đề tài + GVHD)
status: active
created: 2026-05-19
updated: 2026-05-19
audience: dev
related-gap: GAP-688
---

# Thesis V1 — Canonical Metadata

Nguồn truth duy nhất cho metadata khóa luận tốt nghiệp. Mọi script sinh DOCX (`create_thesis_v1.py` per GAP-688) PHẢI đọc các giá trị từ file này hoặc giữ inline `THESIS_INFO` constant đồng bộ với file này.

**Cross-references:**
- Đề cương đồ án nguồn: `documents/07-archived/academic/word-reports/de-cuong-datn/DE_CUONG_DATN.docx`
- Pipeline tham khảo: `documents/07-archived/academic/word-reports/bao-cao-thuc-tap/create_bao_cao_thuc_tap.py`
- Quy định trình bày: `documents/07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.pdf`
- GAP tracking: `documents/04-quality/gaps/phase-1-beta/GAP-688-thesis-v1-python-pipeline-pivot.md`

---

## 1. Thông tin sinh viên

| Thông tin | Giá trị |
|-----------|---------|
| Họ và tên | Nguyễn Văn Kiệt |
| Mã sinh viên | 221230890 |
| Lớp | CNTT1-K63 |
| Khóa | 63 |
| Ngành | Công nghệ thông tin / Khoa học máy tính |
| **Bộ môn** | **Công nghệ phần mềm** |
| Khoa | Công nghệ thông tin |
| Hệ đào tạo | Cử nhân chính quy |
| Trường | Đại học Giao thông Vận tải (UTC GTVT) |

## 2. Thông tin đề tài khóa luận

| Thông tin | Giá trị |
|-----------|---------|
| **Tiêu đề** (in trên bìa chính) | **XÂY DỰNG HỆ THỐNG SAAS CUNG CẤP DỊCH VỤ ĐÀO TẠO** |
| Tiêu đề tiếng Anh (tùy chọn cho bìa phụ) | KiteHub — A Multi-Tenant SaaS Platform for Education Service Providers |
| Loại đồ án | Đồ án tốt nghiệp cử nhân |
| Năm bảo vệ | 2026 |
| Defense window | 15/08/2026 → 15/10/2026 (`release-1.5-thesis-scope.md` §4) |

## 3. Thông tin hướng dẫn

| Vai trò | Giá trị |
|---------|---------|
| **GVHD tại trường** | TS. Nguyễn Đức Dư |
| Đơn vị công tác GVHD | Khoa Công nghệ thông tin — Đại học Giao thông Vận tải |
| **GV phản biện** | TBD — khoa chưa phân công (cập nhật khi có quyết định) |
| CBHD đơn vị (nếu có) | N/A (đồ án nội bộ KiteHub, không có đơn vị ngoài) |

## 4. Inline THESIS_INFO Python constant

Dùng trực tiếp khi adapt `create_bao_cao_thuc_tap.py` → `create_thesis_v1.py`:

```python
# ============== THÔNG TIN SINH VIÊN ==============
STUDENT_INFO = {
    "name": "Nguyễn Văn Kiệt",
    "student_id": "221230890",
    "class": "CNTT1-K63",
    "course": "63",
    "major": "Công nghệ thông tin / Khoa học máy tính",
    "specialization": "Công nghệ phần mềm",      # Bộ môn cấp 2
    "department": "Công nghệ thông tin",
    "degree": "Cử nhân",
    "training_mode": "Chính quy",
    "university": "Đại học Giao thông Vận tải",
    "university_short": "UTC GTVT",
}

# ============== THÔNG TIN ĐỀ TÀI ==============
THESIS_INFO = {
    "title": "XÂY DỰNG HỆ THỐNG SAAS CUNG CẤP DỊCH VỤ ĐÀO TẠO",
    "title_en": "KiteHub — A Multi-Tenant SaaS Platform for Education Service Providers",
    "type": "Đồ án tốt nghiệp cử nhân",
    "year": "2026",
    "defense_window_open": "2026-08-15",
    "defense_window_close": "2026-10-15",
    "advisor": "TS. Nguyễn Đức Dư",
    "advisor_dept": "Khoa Công nghệ thông tin",
    "advisor_university": "Đại học Giao thông Vận tải",
    "reviewer": None,    # TBD — khoa chưa phân công; cập nhật khi có
    "external_mentor": None,    # N/A đồ án nội bộ KiteHub
}
```

## 5. Log

- **2026-05-19**: File tạo per GAP-688. Tiêu đề + bộ môn + GV phản biện confirm bởi user post Wave 101 closure. GV phản biện = TBD (khoa chưa phân công); update lại file này khi có quyết định.
