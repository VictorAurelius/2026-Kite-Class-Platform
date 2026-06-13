---
title: G2 Human Test Recipe — LMS student consumption (lesson-player + assignment-submit + enrollment-scope)
audience: dev
created: 2026-06-14
scope: Flow Verification Campaign G2 handoff — LMS Increment B (GAP-1113) + student-self enrollment endpoint (GAP-1285); student-shell unblocked by KC-9 student-auth (GAP-1277)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1113-lms-frontend-headless-no-consumer.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1285-student-self-enrolled-courses-classes-endpoint.md
  - .claude/rules/g2-handoff-md-mandate.md
  - .claude/rules/kitehub-kiteclass-boundary.md
---

# G2 Recipe — LMS student consumption (KiteClass `:3000`)

> **KiteClass (KC) — FE port `:3000`** (`kiteclass-frontend`), student PWA shell. Login STUDENT (KC-9, GAP-1277).

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn tự test student-shell LMS Increment B:
- (a) `/student/learning` liệt kê **đúng khoá đã ghi danh** (qua `GET /api/v1/enrollments/me` — GAP-1285), KHÔNG phải catalog toàn tenant.
- (b) **Lesson player** (`/student/learning/[courseId]/lessons/[lessonId]`): xem markdown + video embed + **đánh dấu hoàn thành** (mark-complete) + **tiến độ** (progress).
- (c) `/student/assignments` quét **đúng lớp student ghi danh** (enrollment-scoped) + **nộp bài**.
- (d) Isolation: student chỉ thấy enrollment của chính mình (không thấy khoá/lớp student khác).

**Prereq:**
- Stack UP (rebuild `kiteclass-core` + `kiteclass-frontend`).
- STUDENT credential provisioned + student đã được **ghi danh** ≥1 lớp/khoá (per recipe RBAC §2.2 + enrollment seed/KC-4).
- STUDENT login: `hocsinh1@skyedu.vn` / `Student@2026`.

**Thời lượng:** ~12-15 phút.

## 2. Setup

```bash
cd /home/kitedev/projects/2026-Kite-Class-Platform
bash kitehub/scripts/up.sh && bash kitehub/scripts/status.sh
```

- Browser + DevTools → Network (filter `enrollments/me` / `lessons` / `progress` / `assignments`) + Console.
- URL: `http://localhost:3000/student/learning`
- Verify student có enrollment (nếu trống → ghi danh student qua KC-4 hoặc seed):
  ```bash
  docker exec kite-postgres psql -U kite -d kiteclass_shared -c \
  "SET app.current_tenant='<tenant-uuid>'; SELECT id, student_id, class_id FROM enrollments ORDER BY id DESC LIMIT 5;"
  ```

## 3. Các bước (browser-walk qua FE `:3000`)

### Bước 1 — Student login → `/student/learning`
- **Hành động:** `http://localhost:3000/login` → login `hocsinh1@skyedu.vn` / `Student@2026` → redirect `/student` → mở `/student/learning` ("Học tập — Khóa học của bạn").
- **✅ Kỳ vọng (PASS):** Trang liệt kê **các khoá student đã ghi danh** (derive từ enrollments). Network `GET /api/v1/enrollments/me` → 200 (KHÔNG 403). Nếu chưa ghi danh → empty-state "Bạn chưa ghi danh khóa học nào."
- **⚠️ Sad path:** `GET /enrollments/me` → **403** → GAP-1285 regression (endpoint phải cho student tự gọi). Trang hiện **toàn bộ khoá tenant** (catalog) thay vì enrolled → FE chưa rewire sang `enrollmentsApi.getMine()` (báo BLOCKING).
- **🔍 Verify:** Network `GET /api/v1/enrollments/me` 200; số khoá = số enrollment của student này (không phải tổng tenant).

### Bước 2 — Mở lesson player
- **Hành động:** Click 1 khoá đã ghi danh → mở 1 lesson → `/student/learning/<courseId>/lessons/<lessonId>`.
- **✅ Kỳ vọng:** Lesson render markdown nội dung; nếu có video → embed YouTube/Vimeo (`LessonVideo`); hiện trạng thái completion + progress.
- **⚠️ Sad path:** Lesson **tính phí mà student CHƯA ghi danh** → paywall (không full content) — đúng (BR-LMS). Lesson đã ghi danh nhưng trắng nội dung → content load lỗi (báo).
- **🔍 Verify:** Network `GET /lessons/<id>` → 200 + content; `GET .../progress` (useLessonProgress) → trạng thái.

### Bước 3 — Đánh dấu hoàn thành (mark-complete) + progress
- **Hành động:** Trong lesson player → click **đánh dấu hoàn thành**.
- **✅ Kỳ vọng:** `POST/PUT .../progress` (LessonProgressController) → 200; UI đổi sang "đã hoàn thành" + gamification toast; progress khoá tăng.
- **⚠️ Sad path:** Click không có phản hồi (inert) → chưa wire mark-complete (báo — wired ≠ working per `design-source-implementation-parity.md` §3.2). 401 → token hết hạn.
- **🔍 Verify:** F5 reload lesson → vẫn "đã hoàn thành" (persist); progress khoá phản ánh % mới.

### Bước 4 — `/student/assignments` enrollment-scoped + nộp bài
- **Hành động:** Mở `/student/assignments` ("Bài tập").
- **✅ Kỳ vọng:** Liệt kê assignment PUBLISHED **trong các lớp student ghi danh** (enrollment-scoped per GAP-1285, qua `enrollmentsApi.getMine()`); filter pending/submitted/graded.
- **Hành động (nộp):** Mở 1 assignment `pending` → `/student/assignments/<assignmentId>` → **nộp bài** (submit nội dung/file).
- **✅ Kỳ vọng:** Submit → 200; assignment chuyển sang `submitted` trong filter.
- **⚠️ Sad path:** Danh sách hiện assignment của **lớp student KHÔNG ghi danh** → scope leak (báo BLOCKING). Submit fail silent → chưa wire (báo).
- **🔍 Verify:** Network `enrollments/me` → resolve đúng class; assignment list scoped; `useMySubmissions` phản ánh submission.

### Bước 5 — Isolation (student chỉ thấy của mình)
- **Hành động:** (Nếu có student thứ 2 ghi danh khoá khác) login student #2 → `/student/learning`.
- **✅ Kỳ vọng:** Student #2 chỉ thấy enrollment của #2, KHÔNG thấy khoá/lớp của student #1.
- **⚠️ Sad path:** Student #2 thấy khoá student #1 → enrollment-scope leak (báo BLOCKING).
- **🔍 Verify:** `GET /enrollments/me` cho mỗi student trả tập khác nhau (đúng enrollment riêng).

## 4. Sad path quick checks (tổng hợp)
- `GET /enrollments/me` → 403 cho student → GAP-1285 regression.
- `/student/learning` hiện catalog toàn tenant thay vì enrolled → FE chưa rewire.
- Mark-complete inert (click không effect) → wired-not-working.
- `/student/assignments` quét toàn tenant thay vì enrolled classes → scope leak.
- Lesson tính phí (chưa enroll) hiện full content → paywall leak.
- Token hết hạn → 401 → FE prompt re-login.

## 5. Báo kết quả
- ✅ **FULL PASS** → Claude flip GAP-1113 Increment B + GAP-1285 → chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic: progress bar style, toast) → fix inline nếu nhỏ.
- 🔴 **BLOCKING** (enrollments/me 403 / catalog-not-enrolled / mark-complete inert / assignment scope leak) → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot + Network error.

Format: `learning: ✅ | player: ✅ | assignments: ⚠️ (scope leak)`.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|---|---|
| `/student/learning` empty | Student chưa ghi danh → seed enrollment (KC-4) hoặc owner ghi danh |
| `GET /enrollments/me` 403 | GAP-1285 regression → báo blocker (endpoint phải cho STUDENT self) |
| Hiện catalog toàn tenant | FE chưa dùng `enrollmentsApi.getMine()` → báo |
| Mark-complete inert | progress endpoint chưa wire / FE không re-render → báo |
| `:3000` ERR_EMPTY_RESPONSE | restart `kiteclass-frontend` (GAP-1067 class) |
| Student login 401 | credential chưa provision (recipe RBAC §2.2) |

**G3 preview (AWS-gated GAP-612):** student consumption qua gateway `:9000` JWT(`role=STUDENT`)→header chain trên RDS+Flyway thật; enrollment-scope RLS (GAP-1121) trước production; cross-student isolation. Production access-mode subdomain per `g1-browser-walk-before-flip.md` §3.2.
