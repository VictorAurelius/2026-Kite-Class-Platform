# Academic Year + Curriculum Structure Policy — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + School-domain SME
**Reviewer:** Education domain expert + Tech Lead + Legal counsel
**Legal basis:** **Luật Giáo dục 2019 (43/2019/QH14) (Education Law — L5)**; **Khung kế hoạch thời gian năm học** (Bộ GD&ĐT ban hành hằng năm); **Chương trình GDPT 2018** (chương trình giáo dục phổ thông); **Thông tư 27/2020/TT-BGDĐT** (đánh giá học sinh)
**Related:** ADR-002 (academic-year-structure) · ADR-001 (k12-data-model) · [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.5 VN-EDU · [`compliance-scope.md`](compliance-scope.md) §3 (Education Law/L5) + §3.4 (academic year) · [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md) (M4/M5) · GAP-053 (academic year + semester) · GAP-061 (year-end rollover)

---

## 1. Phạm vi & mục đích

Tài liệu này định nghĩa **mô hình cấu trúc năm học (academic year) + chương trình (curriculum)** làm chuẩn nghiệp vụ cho KiteClass, áp dụng cho cả 2 chế độ vận hành: **center mode** (trung tâm — linh hoạt) và **K-12 school mode** (trường — theo khung MoET). Đây là tầng policy/BRD; quyết định kỹ thuật entity đã chốt tại **ADR-002** (AcademicYear + Semester + Holiday) — tài liệu này hợp nhất ngữ cảnh nghiệp vụ + ràng buộc pháp lý.

Skeleton Phase 1: mô hình niên khóa/học kỳ + cấp lớp + phân biệt 2 mode + năm học rollover structure. Chi tiết chương trình GDPT 2018 + thang đánh giá K-12 cần School SME Phase 3.

---

## 2. Mô hình Niên khóa / Học kỳ (Academic Year / Semester model)

Theo **ADR-002** (ACCEPTED): `AcademicYear` là cấu trúc tổ chức cấp cao nhất.

```
AcademicYear  (vd "2026-2027")
├── startDate / endDate        (khung năm học, thường Sep → Jun cho K-12)
├── status: UPCOMING | CURRENT | COMPLETED
├── semesters: [HK1, HK2, SUMMER]   (Học kỳ 1, Học kỳ 2, học kỳ hè)
└── holidays: [VN national + school-specific]
Class / HomeroomClass  → references academicYear
```

### 2.1 Niên khóa (Academic Year)

- K-12 school: theo **khung kế hoạch thời gian năm học** MoET ban hành hằng năm (thường khai giảng đầu tháng 9, kết thúc tháng 5-6).
- Center: linh hoạt — có thể dùng năm dương lịch hoặc khóa học rolling.

### 2.2 Học kỳ (Semester)

- **HK1 / HK2** chuẩn cho K-12; **SUMMER** cho học kỳ hè / khóa hè.
- Center có thể bỏ qua semester (class theo startDate/endDate flat) hoặc map sang khóa.

> TBD (Phase 3 — needs School SME input): ranh giới ngày HK1/HK2 chính xác theo từng năm (lấy từ khung MoET); xử lý năm học vắt qua 2 năm dương lịch trong reporting.

### 2.3 Ngày nghỉ (Holidays)

VN national holidays seed sẵn qua migration (1/1, Tết Nguyên Đán, 30/4, 1/5, 2/9, …) theo ADR-002 + Bộ luật Lao động. School-specific holidays thêm per tenant.

> TBD (Phase 2/3): cập nhật Tết âm lịch hằng năm (ngày thay đổi); school-specific holiday UI.

---

## 3. Cấp lớp (Grade Levels)

| Cấp | Lớp | Tuổi điển hình | Mode |
|---|---|---|---|
| Mầm non (preschool) | — | <6 | out-of-scope Phase 1-3 |
| Tiểu học (primary) | 1-5 | 6-11 | K-12 (P3) |
| THCS (lower secondary) | 6-9 | 11-15 | K-12 (P3) |
| THPT (upper secondary) | 10-12 | 15-18 | K-12 (P3) |
| Trung tâm (center) | n/a (theo khóa/trình độ) | mọi lứa | Phase 1-2 |

Center mode KHÔNG dùng grade-level cứng — dùng **trình độ/khóa** (level/course) linh hoạt (vd A1-C2 ngoại ngữ, sơ-trung-cao cấp kỹ năng).

> TBD (Phase 3 — needs School SME): mã môn học + phân môn theo **Chương trình GDPT 2018** per cấp (M5 trong [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md)).

---

## 4. K-12 mode vs Center mode

| Khía cạnh | K-12 School mode (P5, Phase 3) | Center mode (P1/P2/P3, Phase 1-2) |
|---|---|---|
| Niên khóa | Bắt buộc, theo khung MoET | Tùy chọn / rolling |
| Học kỳ | HK1/HK2 bắt buộc | Tùy chọn |
| Cấp lớp | Tiểu học/THCS/THPT cứng | Trình độ/khóa linh hoạt |
| Đánh giá | Thang 10 + nhận xét + hạnh kiểm (Thông tư 27/2020) | Tự do (điểm/pass-fail/nhận xét) |
| Học bạ | Học bạ điện tử đúng format MoET | Bảng điểm/chứng chỉ tự do |
| Year-end rollover | Lên lớp / ở lại (promotion/retention — GAP-061) | n/a hoặc enroll khóa mới |

> TBD (Phase 3 — needs School SME): logic promotion/retention (điều kiện lên lớp theo Thông tư 27/2020); xử lý học sinh chuyển trường giữa năm (M7).

---

## 5. Year-end Rollover (chuyển năm học)

Theo ADR-002 + GAP-061:
- Cuối năm học → `AcademicYear.status` chuyển COMPLETED, năm mới UPCOMING → CURRENT.
- K-12: tính promotion/retention (lên lớp / ở lại) theo kết quả đánh giá.
- Carry-over: học sinh, homeroom class, transcript history phải bảo toàn qua năm.

> TBD (Phase 3 — needs School SME): điều kiện lên lớp/ở lại định lượng; xử lý học sinh tốt nghiệp (lớp 12 → archive); audit trail rollover.

---

## 6. Tuân thủ pháp lý (Compliance)

- **Education Law L5 (`compliance-scope.md` §3.4):** cấu trúc năm học align khung kế hoạch MoET.
- **Chương trình GDPT 2018:** mã môn + phân môn (K-12, Phase 3).
- **Thông tư 27/2020:** thang đánh giá + học bạ (cross-ref [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md) M1/M2).
- **VN-EDU-3 (`compliance-checklist.md` §1.5):** lưu trữ hồ sơ học sinh (transcript) theo MoET — interplay với [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) edu-records 5y.

> TBD (Phase 3 — needs counsel): xác nhận cấu trúc năm học + đánh giá đủ tuân thủ Luật GD trước launch trường.

---

## 7. Dependencies / References

- ADR-002 (academic-year-structure), ADR-001 (k12-data-model)
- BRD: [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md), [`compliance-scope.md`](compliance-scope.md) §3, [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md), [`product-scope-mrd.md`](product-scope-mrd.md) §3 (phase scope)
- Gaps: GAP-053 (academic year + semester), GAP-061 (year-end rollover)
- Consumer: `01-business/class`, `01-business/grade` (rules.md implement)
- Checklist: [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.5

---

## 8. Out of Scope (this skeleton)

- Mã môn + chương trình GDPT 2018 chi tiết (Phase 3 — School SME)
- Logic promotion/retention định lượng (Phase 3 — SME + engineering)
- Thang đánh giá K-12 đầy đủ theo Thông tư 27/2020 (Phase 3)

---

## 9. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). Mô hình niên khóa/học kỳ (theo ADR-002) + cấp lớp + K-12 vs center mode + rollover structure complete; chương trình GDPT 2018 + thang đánh giá K-12 + promotion logic marked TBD (Phase 3, needs School SME + counsel). Cites Education Law L5 + Chương trình GDPT 2018 + Thông tư 27/2020.
