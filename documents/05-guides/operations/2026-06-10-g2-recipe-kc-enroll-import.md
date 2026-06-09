---
title: G2 Human Test Recipe — KC enroll/import (GAP-1102/1103/1104)
audience: dev
created: 2026-06-10
scope: Flow Verification Campaign G2 handoff — student bulk-import template + single-enroll dialog + bulk-enroll xlsx
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1102-student-bulk-import-template-download.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1103-fe-add-student-to-class-enroll-ui.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1104-bulk-enroll-into-class-xlsx-template.md
---

# G2 Recipe — KC enroll/import (GAP-1102/1103/1104)

> **Sản phẩm:** KiteClass (KC) — FE `kiteclass-frontend` **`:3000`** (per `kitehub-kiteclass-boundary.md` §2). BE `kiteclass-core` + gateway `:9000`.

## 1. Mục tiêu

Walk 3 tính năng nghiệp vụ trường:
- **GAP-1102**: tải template `.xlsx` mẫu để import học sinh (blank + header chuẩn + ví dụ).
- **GAP-1103**: dialog "Thêm học sinh vào lớp" (single-enroll) trên trang lớp.
- **GAP-1104**: ghi danh hàng loạt qua `.xlsx` (template + preview + commit).

## 2. Prereq

- ⚠️ **Stack đã rebuild với code mới** (kiteclass-core + kiteclass-frontend) — coordinator đã `dev-rebuild.sh core` + `dev-rebuild.sh frontend` (kiểm tra `docker ps`: 2 service Up < 15 phút).
- Login tenant KC (vai trò admin/GV của trường) qua app `:3000`. Dùng dữ liệu seed (per GAP-950: 1 GV + 1 lớp + 3 HS). Nếu chưa seed: `bash kiteclass/scripts/init-admin.sh` hoặc tài khoản test local.
- ≥1 lớp tồn tại (để có `/classes/[id]`).
- Thời lượng: ~10-15 phút.

## 3. Setup

- Browser + DevTools:
  - **Network tab** filter `bulk-import` + `enrollments` + `template`.
  - **Console tab** (canh uncaught error).
- Verify stack mới:
  ```bash
  docker ps --format '{{.Names}} {{.Status}}' | grep -E 'kiteclass-core|kiteclass-frontend'
  ```
- DB verify (tùy chọn):
  ```bash
  docker exec kite-postgres psql -U kite -d kiteclass -c \
    "SELECT id, class_id, student_id, status FROM enrollments ORDER BY created_at DESC LIMIT 5;"
  ```

## 4. Các bước (browser-walk — `:3000`)

### Bước 1 — Đăng nhập + lấy classId
- **Hành động**: `http://localhost:3000/login` → đăng nhập tenant → mở `http://localhost:3000/classes` → chọn 1 lớp → ghi lại `classId` từ URL `/classes/{id}`.
- **✅ Kỳ vọng**: danh sách lớp render; vào được `/classes/{id}` (roster lớp hiển thị).
- **⚠️ Sad path**: redirect `/login` → token hết hạn → đăng nhập lại; danh sách lớp rỗng → seed data (`init-admin.sh`).
- **🔍 Verify**: Network `GET /api/v1/classes` + `/api/v1/classes/{id}` 2xx.

### Bước 2 — GAP-1102: tải template import học sinh
- **Hành động**: mở `http://localhost:3000/admin/bulk-import` → bấm nút **"Tải template mẫu"**.
- **✅ Kỳ vọng**: file `.xlsx` tải về; mở được bằng Excel/LibreOffice; có **header chuẩn** (canonical columns) + dòng ví dụ. Network `GET /api/v1/students/bulk-import/template` → **200** + `Content-Type` xlsx.
- **⚠️ Sad path**: 404/500 → kiteclass-core chưa rebuild (XlsxTemplateGenerator) → check `docker ps`; file tải về 0 byte / không mở được → báo lại.
- **🔍 Verify**: mở file xác nhận header khớp doc BR-BI-007.

### Bước 3 — GAP-1103: dialog "Thêm học sinh vào lớp" (single-enroll)
- **Hành động**: trên `/classes/{id}`, bấm nút **"Thêm học sinh vào lớp"** → chọn 1 học sinh + nhập tuition + discount + notes → bấm Lưu/Ghi danh.
- **✅ Kỳ vọng**: dialog mở; submit → Network `POST /api/v1/enrollments` **201**; toast thành công; roster refresh (học sinh mới xuất hiện, không cần F5).
- **⚠️ Sad path**:
  - học sinh đã ghi danh → **409** → toast báo rõ "đã ghi danh" (KHÔNG generic error).
  - thiếu field bắt buộc → **400** → toast/inline báo rõ.
- **🔍 Verify**:
  ```bash
  docker exec kite-postgres psql -U kite -d kiteclass -c \
    "SELECT * FROM enrollments WHERE class_id='{id}' ORDER BY created_at DESC LIMIT 1;"
  ```

### Bước 4 — GAP-1104: ghi danh hàng loạt qua xlsx
- **Hành động**: mở `http://localhost:3000/classes/{id}/bulk-enroll` → bấm **tải template bulk-enroll** → điền vài dòng (cols `student_email|student_phone|class_code|tuition_amount|discount_percent|note`) → upload → **preview** → **commit**.
- **✅ Kỳ vọng**:
  - `GET .../enrollments/bulk-import/template` 200 + xlsx đúng cols.
  - upload → **preview** hiển thị danh sách resolve (email→phone, class_code tenant-scoped) + dòng lỗi (skip-and-report).
  - **commit** → **201**, ghi danh thành công các dòng hợp lệ, báo cáo dòng skip.
- **⚠️ Sad path**: email không tồn tại → dòng đó skip + báo trong report (KHÔNG fail cả batch); class_code sai tenant → skip.
- **🔍 Verify**: roster lớp tăng đúng số dòng hợp lệ; DB `enrollments` có rows mới.

## 5. Sad path checks tổng hợp
- Template tải về: xlsx hợp lệ, header chuẩn, mở được.
- Single-enroll 409/400 → message rõ (không generic).
- Bulk-enroll: skip-and-report (1 dòng lỗi KHÔNG fail cả batch).
- Watch `@EntityScan` boot-crash class (như GAP-1101): nếu kiteclass-core không boot sau rebuild → check log `docker logs kiteclass-core | grep -i "not a managed type"`.

## 6. Báo kết quả (báo lại 1 trong 4)
- ✅ **FULL PASS** (3 tính năng) → coordinator flip GAP-1102/1103/1104 DONE, chờ G3.
- ⚠️ **MOSTLY PASS** (vd 2/3, hoặc cosmetic) → catalog gap polish.
- 🔴 **BLOCKING** (template 404 / enroll 500 / bulk fail batch / boot crash) → catalog + fix loop + re-walk.
- ❓ **UNCLEAR** → screenshot Network + Console + ping.

## 7. Troubleshooting + G3 preview
| Triệu chứng | Quick fix |
|---|---|
| template 404/500 | kiteclass-core chưa rebuild — `docker ps` check Up time |
| nút "Tải template"/"Thêm học sinh" không thấy | kiteclass-frontend chưa rebuild |
| kiteclass-core không boot | `docker logs kiteclass-core` — check @EntityScan / migration error |
| enroll 201 nhưng roster không refresh | invalidate query — báo coordinator (useCreateEnrollment) |

**G3 preview**: sau G2 PASS, production-parity walk qua gateway `:9000` với tenant JWT mint + RLS check (cross-tenant IDOR) — coordinator chạy.
