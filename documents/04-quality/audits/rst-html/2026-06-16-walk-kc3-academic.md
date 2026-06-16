---
title: "Browser-walk KC-3 Academic (course → class → schedule) — headless nip.io subdomain"
date: 2026-06-16
flow: KC-3
type: g1-browser-walk
audience: dev
rule: g1-browser-walk-before-flip.md §3.1/§3.3 (nip.io subdomain, production-accurate Host)
stack: kiteclass-frontend :3000 + kite-gateway :9000 + kiteclass-core + kite-postgres (all healthy)
walk_url: "http://sky-education-074901.127.0.0.1.nip.io:3000"
tenant: sky-education-074901 (instance_id 5b3ef1ae-39e7-4088-888f-941fca67f410, status TRIAL)
persona: Owner (owner+074901@skyedu.vn / SkyEdu@2026)
tool: Playwright 1.59.1 headless chromium (local node)
evidence_screenshots: /tmp/kc3-step-{1,2,4,4b,5,6,7,8}*.png + /tmp/kc3-{teacher-dropdown,dashboard-invdate}.png
---

# Browser-walk KC-3 Academic — 2026-06-16

## Tóm tắt

Walk Owner end-to-end qua FE thật (`:3000`) trên nip.io subdomain (production-accurate Host resolution, KHÔNG curl gắn header tay, KHÔNG `?tenant=`). Stack full healthy; FE render production-quality tiếng Việt. **Chuỗi course → class → schedule WALKABLE qua browser** với caveat FM-1/FM-2 (niên khóa decoupled / schedule = recurrence-panel-trong-form-tạo-lớp) đúng như pre-walk dự đoán.

**Verdict:** ✅ **PASS-with-notes** — luồng implemented (course/class/recurrence) thông qua browser thật; 1 cosmetic bug mới (dashboard "Invalid Date") + 1 log-noise (login fallback 401) cần catalog. Niên khóa (FM-1) decoupled không executable — đúng GAP-982 đã track. Cross-tenant isolation NOT-TESTED qua walk này (cần phiên auth tenant thứ 2; curl-:9000 probe sai vì thiếu JWT).

## Bảng bước / status / evidence / bug

| Bước | Hành động | Status | Evidence | Bug |
|---|---|---|---|---|
| 1 — base nip.io resolve | goto `sky-education-074901.127.0.0.1.nip.io:3000/` | ⚠️ OBSERVE | nip.io DNS→127.0.0.1 ✅, HTTP 200, base `/` không redirect login (render public/landing — hợp lệ cho anonymous) | none (assertion-side; owner walk vào `/login` trực tiếp) |
| 2 — Owner login | fill email+pass → submit | ✅ PASS | redirect `/dashboard`; courses/students/teachers/invoices API đều 200 | **BUG-KC3-1** (log noise) — FE POST `/api/v1/tenant-auth/login` → **401** TRƯỚC khi fallback `/api/auth/login` → 200. Owner = platform-auth; tenant-auth dành cho member. 401 + console error mỗi owner login |
| 3 — Sidebar niên-khóa absence (FM-1) | đọc nav | ✅ CONFIRMED | sidebar: Tổng quan/Học viên/Lớp học/Điểm danh/Học phí/Giáo viên/Khóa học/Báo cáo/Bảng lương/Thương hiệu/Cài đặt — **KHÔNG có "Niên khóa"** | FM-1 confirmed (decoupled; GAP-982 đã track) |
| 4 — Courses list | goto `/courses` | ✅ PASS | render 2 khóa SKY-TOEIC-074901 + SKY-IELTS-RW-074901, trạng thái "Bản nháp", "Miễn phí"; data tenant-scoped đúng | none |
| 4b — Create course teacher dropdown (FM-3) | goto `/courses/new`, mở combobox giáo viên | ✅ REFUTED | `/api/v1/teachers?status=ACTIVE&size=100`→200; combobox = **2 options** (Lê Thị Bình, Nguyễn Văn An) | FM-3 refuted (dropdown populated) |
| 5 — Classes list guard (FM-4) | goto `/classes` | ✅ PASS | có guard "chọn khóa học" trước; không crash | FM-4 refuted (guard present, no crash) |
| 6 — Course detail → classes | goto `/courses/26` | ✅ PASS | classes của khóa hiển thị (Lớp IELTS/Tối Thứ/Beginner/Verify) | none |
| 7 — Class detail → sessions | goto `/classes/20` | ✅ PASS | surface buổi học/lịch render | none |
| 8 — Create class recurrence (FM-2) | goto `/courses/26/classes/new` | ✅ PASS | recurrence toggle "Lặp lại theo lịch (tuần)" + maxStudents field present | FM-2 confirmed (schedule = recurrence-panel-trong-form, KHÔNG trang riêng) |
| 9 — Dashboard render | post-login `/dashboard` | ⚠️ BUG | wizard "Bước 1/5" + stats (4 HV / 2 GV / 2 khóa / 7 lớp) đúng | **BUG-KC3-2** (cosmetic P3) — widget "Học viên mới nhất": mỗi student email kèm **"Invalid Date"** (4 students: quang.vu/lan.do/nam.hoang/mai.pham +074901) — date field null/unparseable |
| 10 — Cross-tenant isolation | curl `:9000` X-Tenant-Id A vs B | ⚠️ NOT-TESTED | own-tenant + cross-tenant ĐỀU 400 → probe sai (gateway :9000 cần JWT, X-Tenant-Id tay bị strip — GAP-1068 class) | KHÔNG phải bug; isolation = GAP-983 đã fix, cần phiên auth tenant-B browser để verify |

## Pre-walk failure-mode verify (8 FM)

| FM | Pre-walk claim | Walk verdict |
|---|---|---|
| FM-1 🔴 "tạo niên khóa" không executable | no @RestController + no FE nav | ✅ **THẬT** — sidebar 0 niên-khóa nav; decoupled, course/class chạy độc lập (GAP-982) |
| FM-2 🔴 không có trang xếp lịch riêng | recurrence panel trong form tạo lớp | ✅ **THẬT** — toggle "Lặp lại theo lịch (tuần)" trong `/courses/[id]/classes/new` |
| FM-3 🔴 teacher dropdown rỗng | dropdown phụ thuộc useTeachers ACTIVE | ❌ **REFUTED** — 2 teacher ACTIVE populated |
| FM-4 🟠 /classes crash khi chưa chọn course | non-null assertion | ❌ **REFUTED** — guard "chọn khóa học" present, no crash |
| FM-5 🟠 mã course pattern/unique | — | ⏭️ NOT-WALKED (không submit create trong walk read-only) |
| FM-6 🟠 cross-tenant gateway header | X-Tenant-Id resolution | ⚠️ inconclusive — login chain confirm gateway resolve tenant qua JWT (owner login 200 + data đúng tenant); curl-probe isolation sai |
| FM-7 🟡 hasAccessToClass owner 403 | — | ⏭️ NOT-WALKED (không tạo schedule trong walk) |
| FM-8 🟡 recurrence until validation | — | ⏭️ NOT-WALKED (không submit recurrence) |

## Bugs cần catalog (browser-level)

1. **BUG-KC3-1 (P3 log-noise)** — FE login chain emit `401 POST /api/v1/tenant-auth/login` + console error TRƯỚC fallback `/api/auth/login` 200 cho mọi Owner login. By-design fallback (owner=platform-auth) nhưng spam 401/console mỗi login → noise debug + confuse log triage. Đề xuất: FE route owner→`/api/auth/login` trực tiếp HOẶC silent-catch tenant-auth 401 trong fallback chain.
2. **BUG-KC3-2 (P3 cosmetic)** — Dashboard widget "Học viên mới nhất" render "Invalid Date" cho 4 seed students. Date field (createdAt/enrolledAt) null/unparseable. FE date-format không guard null → "Invalid Date" visible. Đề xuất: guard null date → "—" hoặc seed backfill date.

## 3 bug nghiêm trọng nhất

KC-3 KHÔNG có bug blocking — luồng implemented thông. 3 cần chú ý nhất (cả P3):
1. **BUG-KC3-2** dashboard "Invalid Date" — visible cosmetic, mọi owner thấy ngay sau login.
2. **BUG-KC3-1** login 401-then-fallback — log/console noise mỗi owner login.
3. **FM-1 niên khóa** — không executable (GAP-982 đã track) — không phải regression, nhưng AC "tạo niên khóa" của KC-3 không walk được.

## Ghi chú phương pháp

- nip.io subdomain resolve 127.0.0.1 OK (WSL `getent` + browser đều resolve); FE :3000 healthy (rebuilt 3h, không stale GAP-1067).
- Login flow xác nhận KH/KC boundary: Owner=platform-auth (`/api/auth/login`), tenant member=`/api/v1/tenant-auth/login`. FE thử member-auth trước → fallback platform-auth.
- Một số API call dùng `localhost:9000` absolute (branding/students/courses) lẫn subdomain `:9000` (login) — auth qua JWT nên cả hai hoạt động; minor base-URL inconsistency (không block).
- Walk read-only (không submit create course/class/schedule) → FM-5/7/8 NOT-WALKED.
