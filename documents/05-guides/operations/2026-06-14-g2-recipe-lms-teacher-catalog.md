---
title: G2 Human Test Recipe — LMS teacher-authoring + guest catalog/paywall (Increment A)
audience: dev
created: 2026-06-14
scope: Flow Verification Campaign G2 handoff — LMS frontend Increment A (GAP-1113): teacher content authoring + guest catalog trial/paywall CTA
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1113-lms-frontend-headless-no-consumer.md
  - .claude/rules/g2-handoff-md-mandate.md
  - .claude/rules/kitehub-kiteclass-boundary.md
---

# G2 Recipe — LMS teacher-authoring + guest catalog (KiteClass `:3000`)

> **KiteClass (KC) — FE port `:3000`** (`kiteclass-frontend`). Catalog là trang **public** (không cần login); authoring cần login **TEACHER/OWNER**.

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn tự test LMS Increment A (GAP-1113):
- (a) **Teacher authoring:** mở `/courses/[id]` tab **"Nội dung"** → tạo / sửa / xoá / sắp xếp lại (reorder) module + lesson của khoá mình dạy.
- (b) **Guest catalog:** mở `/catalog` (không login) → xem danh sách khoá học của tenant; mở 1 khoá → xem **trial lesson** miễn phí + **paywall CTA** cho lesson tính phí.

**Prereq:**
- Stack UP (rebuild `kiteclass-core` + `kiteclass-frontend`).
- Tenant `sky-education`; có ≥1 course (seed academic) + 1 teacher dạy course đó.
- TEACHER login (provision per recipe RBAC §2.2 nếu chưa có): `giaovien1@skyedu.vn` / `Teacher@2026`.

**Thời lượng:** ~12-15 phút.

## 2. Setup

```bash
cd /home/kitedev/projects/2026-Kite-Class-Platform
bash kitehub/scripts/up.sh && bash kitehub/scripts/status.sh
```

- Browser + DevTools → Network (filter `lms` / `modules` / `lessons` / `courses`) + Console.
- URL: dashboard `http://localhost:3000` · catalog public `http://localhost:3000/catalog`
- Lấy `courseId` thật:
  ```bash
  docker exec kite-postgres psql -U kite -d kiteclass_shared -c \
  "SET app.current_tenant='<tenant-uuid>'; SELECT id, name FROM courses ORDER BY id LIMIT 5;"
  ```

## 3. Các bước (browser-walk qua FE `:3000`)

### Bước 1 — Teacher login + mở khoá học
- **Hành động:** `http://localhost:3000/login` → login `giaovien1@skyedu.vn` / `Teacher@2026` → redirect `/teacher` → mở `http://localhost:3000/courses/<courseId>`.
- **✅ Kỳ vọng (PASS):** Trang chi tiết khoá render đầy đủ shell + có tab **"Nội dung"** (CourseContentManager — GAP-1113 Increment A).
- **⚠️ Sad path:** Không thấy tab "Nội dung" → FE chưa wire CourseContentManager (báo). 403 mở course → teacher không dạy course này (dùng course teacher có dạy).
- **🔍 Verify:** Network `GET /api/v1/.../courses/<id>/modules` → 200 + cấu trúc module/lesson.

### Bước 2 — Tạo module + lesson (CRUD)
- **Hành động:** Tab "Nội dung" → **Thêm module** (nhập tên, vd "Chương 1: Nhập môn") → lưu. Trong module → **Thêm bài học** (nhập tiêu đề "Bài 1: Giới thiệu", nội dung markdown / video URL).
- **✅ Kỳ vọng:** `POST /modules` → 200/201; `POST /lessons` → 200/201; module + lesson hiện ngay trên UI.
- **⚠️ Sad path:** Tên rỗng → 400 + validation message tiếng Việt. 403 → không phải owner của course (`X-Teacher-Id` BR-LMS-006).
- **🔍 Verify:** `GET .../modules` reload → có module + lesson vừa tạo.

### Bước 3 — Sửa + reorder
- **Hành động:** Sửa tên 1 lesson (PUT) → lưu. Kéo-thả (hoặc nút lên/xuống) để **sắp xếp lại** thứ tự module/lesson.
- **✅ Kỳ vọng:** `PUT /lessons/.../manage` → 200; reorder → thứ tự cập nhật + persist (F5 reload giữ nguyên thứ tự mới).
- **⚠️ Sad path:** Reorder không persist sau F5 → endpoint reorder chưa wire (báo).
- **🔍 Verify:** F5 → thứ tự mới giữ nguyên.

### Bước 4 — Xoá lesson
- **Hành động:** Xoá 1 lesson test → confirm.
- **✅ Kỳ vọng:** `DELETE /lessons/...` → 200; lesson biến mất; module còn lại nguyên.
- **⚠️ Sad path:** Xoá nhầm cascade cả module → báo.

### Bước 5 — Guest catalog (không login)
- **Hành động:** Mở **tab ẩn danh** (không session) → `http://localhost:3000/catalog`.
- **✅ Kỳ vọng:** Trang public render danh sách "Các khoá học đang được trực tiếp giảng dạy"; có ô tìm kiếm "Tìm khóa học"; hiển thị số khoá ("Hiển thị N khóa học").
- **⚠️ Sad path:** "Không thể tải danh sách khóa học" (đỏ) → public courses endpoint lỗi/403 (catalog phải permitAll). Trống danh sách → tenant chưa seed course.
- **🔍 Verify:** Network `GET .../publicCourses` (queryKey `['publicCourses','catalog']`) → 200.

### Bước 6 — Trial lesson + paywall CTA
- **Hành động:** Click 1 khoá → `/catalog/<id>` → mở 1 **trial lesson** (miễn phí) → rồi mở 1 lesson **tính phí**.
- **✅ Kỳ vọng:**
  - Trial lesson: guest **xem được** nội dung (BR-LMS-001/002).
  - Lesson tính phí: nội dung bị che + hiện **paywall CTA** (vd "Ghi danh"/"Liên hệ tư vấn miễn phí") thay vì nội dung đầy đủ.
- **⚠️ Sad path:** Guest xem được full nội dung lesson tính phí → paywall leak (báo BLOCKING — guest chỉ thấy trial). Trial lesson bị che → over-restrict.
- **🔍 Verify:** Network `GET /lessons/<id>` cho lesson phí → response KHÔNG chứa full `content`/`videoUrl` (chỉ metadata + flag `isTrial=false`).

## 4. Sad path quick checks (tổng hợp)
- Teacher sửa content của course KHÔNG dạy → 403.
- Tên module/lesson rỗng → 400 + lỗi tiếng Việt.
- Reorder không persist sau F5 → endpoint chưa wire.
- Guest xem full lesson tính phí (không phải trial) → paywall leak.
- Catalog public 403 → public endpoint cấu hình sai (phải permitAll).

## 5. Báo kết quả
- ✅ **FULL PASS** → Claude flip GAP-1113 Increment A → chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic: nhãn nút, layout) → fix inline nếu nhỏ (per `small-gap-inline-fix.md`).
- 🔴 **BLOCKING** (authoring 403 sai / paywall leak / reorder không persist) → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot + Network error.

Format: `Authoring: ✅ | Catalog: ⚠️ (paywall leak lesson phí)`.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|---|---|
| Không thấy tab "Nội dung" | CourseContentManager chưa wire — báo blocker |
| Catalog "Không thể tải" | public courses endpoint 403/500 → check gateway route permitAll |
| Teacher authoring 403 mọi call | teacher không dạy course → dùng course teacher có dạy (`X-Teacher-Id`) |
| `:3000` ERR_EMPTY_RESPONSE | restart `kiteclass-frontend` container (GAP-1067 class) |

**G3 preview (AWS-gated GAP-612):** LMS authoring + catalog qua gateway `:9000` JWT→header (teacher) / permitAll (guest) trên RDS+Flyway thật; RLS GAP-1121 merged trước production (per GAP-1113 AC). Production access-mode subdomain per `g1-browser-walk-before-flip.md` §3.2.
