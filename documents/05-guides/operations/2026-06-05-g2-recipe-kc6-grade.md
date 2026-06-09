---
title: G2 Human Test Recipe — KC-6 Grade (entry → calculate → finalize → transcript)
audience: dev
product: KiteClass (KC) — FE kiteclass-frontend :3000, backend kiteclass-core qua gateway :9000 (per kitehub-kiteclass-boundary.md §2)
created: 2026-06-05
scope: Flow Verification Campaign G2 handoff cho KC-6 — nhập điểm thành phần → tính tổng kết → chốt → bảng điểm/transcript + thống kê lớp
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-05-flow-kc6-grade.md
  - documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc6-grade.md
---

# G2 Human Test Recipe — KC-6 Grade

## Mục tiêu

Giáo viên nhập điểm thành phần (giữa kỳ/cuối kỳ với trọng số) → tính điểm tổng kết (letter + GPA) → chốt điểm → xem bảng điểm/transcript + thống kê lớp. Verify fix G1: **GAP-998 P0** (grading_scales seed + drift V88 — calculate chạy được, trước đó 404), **GAP-999** (authz grade write OWASP A01).

**Prereq:**
- Stack local UP: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/actuator/health` → 200.
- Data sẵn (tenant `sky-education` `0edaee10-...`): student 4 enrolled class 14, teacher UUID `00aa4ce9-...`, teacher_classes MAIN_TEACHER (teacher id 3), grading_scales seeded per-tenant (8 bands A+..F). Grade id=25 đã finalize ở G1 (có thể unfinalize để test lại, hoặc dùng student/class khác).

**Thời lượng:** ~15 phút.

## ⚠️ Lưu ý quan trọng (contract surprises)

- **calculate/finalize cần grading_scales đã seed** (V88). Nếu 404 `GRADING_SCALE_NOT_FOUND` → V88 chưa apply (rebuild). **Tenant mới** chưa có scale → GAP-1002 (provisioning follow-up).
- **"Report card" = `/api/v1/grades/transcripts/*`**, KHÔNG ở `/api/v1/reports` (chỉ revenue/attendance).
- **Authz:** grade write/calc cần `X-User-Id` = teacher của lớp (`00aa4ce9-...`). finalize body cần `teacherId` (numeric MAIN_TEACHER = 3). ⚠️ teacherId hiện self-asserted (GAP-1000) + ADMIN bị chặn finalize (GAP-1000).
- **Thứ tự bắt buộc:** initialize → components (Σ weight=100%) → calculate → finalize → transcript.

## Setup

- Browser + DevTools Network (filter `grades`).
- DB verify: `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tA -c "..."`.
- Biến: `SKY=0edaee10-2d13-44be-9151-12b78b7c5fd4`, `TID=00aa4ce9-0f7c-48a9-bf8d-6e974ba30023`. Header chung: `-H X-Tenant-Id:$SKY -H X-User-Id:$TID -H Content-Type:application/json`.

## Các bước

### Bước 1 — Khởi tạo bảng điểm

- **Hành động:** Mở lớp 14 → học sinh (student 4) → khởi tạo bảng điểm.
- **✅ Kỳ vọng:** HTTP 201/200, grade status `IN_PROGRESS`, `components: []`.
- **🔍 Verify (curl):** `POST "/api/v1/grades/initialize?studentId=4&classId=14"` → `data.id` (gradeId).

### Bước 2 — Nhập điểm thành phần (trọng số cộng 100%)

- **Hành động:** Thêm Giữa kỳ (MIDTERM, 85/100, weight 40%) + Cuối kỳ (FINAL, 90/100, weight 60%).
- **✅ Kỳ vọng:** Mỗi component → 201.
- **⚠️ Sad path:** Thêm MIDTERM 2 lần (sửa điểm) → hiện tạo 2 dòng thay vì update (GAP-1001 đồng dạng / pre-walk #7) — quan sát.
- **🔍 Verify:** `POST "/api/v1/grades/components" -d '{"gradeId":<id>,"componentType":"MIDTERM","componentName":"Giữa kỳ","score":85,"maxScore":100,"weightPercent":40}'` → 201; lặp với FINAL weight 60.

### Bước 3 — Tính điểm tổng kết

- **Hành động:** Nhấn "Tính tổng kết".
- **✅ Kỳ vọng:** HTTP 200, `finalScore=88.0`, `letterGrade=B+`, `gpa=3.3` (= 85×0.4 + 90×0.6). **Trước fix V88: 404.**
- **🔍 Verify:** `POST "/api/v1/grades/<id>/calculate"` → finalScore/letterGrade/gpa.

### Bước 4 — Chốt điểm (finalize)

- **Hành động:** Chốt điểm (cần là MAIN_TEACHER).
- **✅ Kỳ vọng:** HTTP 200, `isFinalized=true`. Chốt yêu cầu Σ weight = 100% (BR-GRD-002).
- **⚠️ Sad path:** ADMIN chốt → 403 `TEACHER_NOT_IN_CLASS` (GAP-1000 đã biết — chưa cho ADMIN bypass).
- **🔍 Verify:** `POST "/api/v1/grades/<id>/finalize" -d '{"teacherId":3,"comments":"Đạt"}'` → isFinalized=true.

### Bước 5 — Bảng điểm/transcript + thống kê

- **Hành động:** Sinh transcript học sinh + xem thống kê lớp.
- **✅ Kỳ vọng:** transcript 201; statistics 200.
- **⚠️ Sad path:** transcript hiện gộp grade mọi học kỳ (không lọc semester) + credit hardcode 3.0 + studentName trống (GAP-1001 đã biết).
- **🔍 Verify:** `POST "/api/v1/grades/transcripts/generate?studentId=4&semester=Spring%202026"` → 201; `GET "/api/v1/grades/class/14/statistics"` → 200.

## Sad path quick checks (tổng hợp)

- **Authz (GAP-999):** calculate/unfinalize không `X-User-Id` hoặc user không phải teacher lớp → **403**.
- **Cách ly tenant (GAP-983):** GET grade by-id với tenant `khanh` (`126eaa8c-...`) → **404**.
- **grading_scale chưa seed:** calculate → 404 `GRADING_SCALE_NOT_FOUND` (kiểm tra V88).

## Báo kết quả

**Khi G2 xong, báo lại 1 trong 4:**
- ✅ **FULL PASS** → Claude xác nhận KC-6 G1+G2, chờ G3.
- ⚠️ **MOSTLY PASS** với cosmetic (transcript studentName trống, semester filter) → đã có GAP-1001.
- 🔴 **BLOCKING** (calculate 404, finalize fail) → catalog + fix loop.
- ❓ **UNCLEAR** → ping screenshot + Network tab.

## Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| calculate 404 GRADING_SCALE_NOT_FOUND | V88 chưa apply / tenant chưa seed scale — `SELECT count(*) FROM grading_scales WHERE instance_id='<tenant>'::uuid` |
| 403 mọi grade write | Thiếu `X-User-Id` = teacher lớp |
| finalize 403 (teacher đúng) | grade thuộc lớp khác / teacher không MAIN_TEACHER trong teacher_classes |
| finalize 400 weights | Σ weightPercent ≠ 100% |

**G3 (production parity, post AWS restore):** multi-tenant grade isolation thật + new-tenant grading_scale provisioning (GAP-1002) + K12 multi-subject gradebook (`/api/v1/grades/subjects`) — secondary chưa walk.
