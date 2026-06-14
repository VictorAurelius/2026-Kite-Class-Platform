---
title: G1 FE Browser Walk — RBAC + LMS flows (Playwright headless trên stack rebuild)
audience: dev
created: 2026-06-14
scope: Flow Verification Campaign — tầng G1-browser (rung trên BE-contract walk 2026-06-14). Walk FE thật (`:3000` KC) qua Playwright headless để bắt FE-runtime bug (render crash / blank / redirect loop / wrong API / missing nav) TRƯỚC human G2★.
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md
  - .claude/rules/g1-browser-walk-before-flip.md
  - documents/05-guides/operations/2026-06-14-g2-recipe-rbac-role-shell.md
verdict: 12/12 walkable checks PASS · 0 FE-runtime bug · 1 flow BLOCKED (SSO — thiếu KH owner credential) → GAP-1305
status_note: KHÔNG flip gap nào → DONE. Human G2★ vẫn bắt buộc (per feature-ship-runtime-walk-mandate). G1-FE = rung dưới G2★.
---

# G1 FE Browser Walk — RBAC + LMS (2026-06-14)

> **Đây là tầng G1-browser** (Claude-side, headless Playwright trên FE thật), KHÔNG phải human G2★. KHÔNG flip gap → DONE.
> Rung trên là BE-contract walk (`2026-06-14-g1-runtime-walk-rbac-lms.md`, gateway curl) — đã PASS 6/6 + bắt GAP-1297/1298/1299/1300/1301. Walk này thêm tầng FE render + FE↔gateway wiring + click route mà BE-contract không cover.

## 1. Setup + môi trường

| Mục | Giá trị |
|---|---|
| Stack | Rebuild `kiteclass-frontend` + `kitehub-frontend` (image cũ 3-4 ngày, pre-RBAC/LMS commit) qua `kitehub/scripts/rebuild.sh` → cả 2 healthy |
| Browser | Playwright headless Chromium (`@playwright/test`, chromium-1217) |
| KC walk URL | `http://skytest.127.0.0.1.nip.io:3000` — **subdomain Host THẬT (nip.io)** per `g1-browser-walk-before-flip.md` §3.1 (KHÔNG `?tenant=`/localhost-thuần) |
| KH walk URL | `http://localhost:3001` (apex KH portal) |
| Tenant | `skytest` (id `aaaabbbb-0000-0000-0000-000000000001`, ACTIVE) — DB `kiteclass_shared`. Public resolve `/api/v1/public/tenants/by-subdomain/skytest` → 200 ACTIVE. CORS gateway cho phép cả `localhost:3000` lẫn nip.io origin. |
| Credentials (BE-only setup) | admin@test.com/Test@12345 (ADMIN→/dashboard) · teacher_a@test.com/Test@12345 (TEACHER→/teacher) · parent-walk@test.com/Test@12345 (PARENT→/parent, reset pw) · student_walk@test.com/Test@12345 (STUDENT→/student, provision mới entity_id=164) |
| Screenshots | `.walk-artifacts/screenshots/` (gitignored, agent worktree) |

**Test-setup mutations trên dev DB `kiteclass_shared` (skytest tenant):** (a) INSERT credential STUDENT `student_walk@test.com` entity_id=164 (skytest enrolled student); (b) reset password `parent-walk@test.com` về `Test@12345` (hash cũ unknown). Đều là dev test cred (mirror pattern `kitehub/scripts/seed-toan10a1-demo.sql`), KHÔNG production.

## 2. Verdict per-flow

| # | Flow | Bước walk | URL cuối | Console err | Net 4xx | Verdict | Screenshot |
|---|---|---|---|:---:|:---:|:---:|---|
| 1a | RBAC role-redirect OWNER/ADMIN | login admin → role-home | `/dashboard` | 0 | 0 | ✅ PASS | rbac-admin-02-home.png |
| 1b | RBAC role-redirect TEACHER | login teacher → role-home | `/teacher/dashboard` | 0 | 0 | ✅ PASS | (rbac-teacher) |
| 1c | RBAC role-redirect STUDENT | login student → role-home | `/student/today` | 0 | 0 | ✅ PASS | (rbac-student) |
| 1d | RBAC role-redirect PARENT | login parent → role-home | `/parent` | 0 | 0 | ✅ PASS | rbac-parent-02-home.png |
| 2a | RoleGuard teacher → /admin/roles | gõ URL admin route | redirect `/teacher/dashboard` (chặn) | — | — | ✅ PASS | guard-teacher |
| 2b | RoleGuard student → /teacher | gõ URL teacher route | redirect `/student/today` (chặn) | — | — | ✅ PASS | guard-student |
| 2c | RoleGuard parent → /admin/roles | gõ URL admin route | redirect `/parent` (chặn) | — | — | ✅ PASS | guard-parent |
| 3 | RBAC `/admin/roles` render (admin) | owner mở trang phân quyền | `/admin/roles` | 0 | 0 | ✅ PASS | roles-admin-page.png |
| 4a | LMS teacher `/courses/1` content tab | teacher mở course | `/courses/1` (tab "Nội dung") | 0 | 0 | ✅ PASS | lms-teacher-course.png |
| 4b | LMS guest `/catalog` | ẩn danh mở catalog | `/catalog` (course + paywall CTA) | 0 | 0 | ✅ PASS | lms-guest-catalog-deep.png |
| 5a | LMS student `/student/learning` | student mở học tập | `/student/learning` (Test Course card) | 0 | 0 | ✅ PASS | lms-student-learning-fixed.png |
| 5b | LMS student `/student/assignments` | student mở bài tập | `/student/assignments` | 0 | 0 | ✅ PASS | lms-student-assignments-fixed.png |
| 6 | SSO KH→KC | login KH `:3001` | bị đá `/login` (KH) | — | — | 🔴 **BLOCKED** | sso-kh-dashboard.png |

**Tổng:** 12/12 walkable check ✅ PASS · 0 FE-runtime bug · 1 BLOCKED (SSO — không có KH owner credential).

## 3. Chi tiết các flow chính

### 3.1 RBAC role-redirect (PASS 4/4)
4 role login qua FE `:3000` browser → FE tự inject auth token + tenant header → redirect đúng role-home: ADMIN/OWNER `/dashboard`, TEACHER `/teacher/dashboard`, STUDENT `/student/today`, PARENT `/parent`. `roles.ts` `ROLE_HOME` + `normalizeRole` hoạt động đúng qua browser. Console clean, 0 net 4xx. Bắt FE↔gateway wiring (login → JWT → role-home) — đúng phần BE-contract không cover.

### 3.2 RoleGuard cross-role (PASS 3/3)
3 hướng cross-role direct-URL đều bị chặn (redirect về role-home của actor, KHÔNG render trang ngoài quyền). RoleGuard `(dashboard)/admin/layout.tsx` không leak.

### 3.3 RBAC `/admin/roles` render (PASS)
Trang "Phân quyền" render đầy đủ: 5 mẫu vai trò cố định (Chủ trung tâm OWNER / Nhân viên STAFF / Giáo viên TEACHER / Phụ huynh PARENT / Học sinh STUDENT), mỗi mẫu có badge role + trạng thái "Chưa khởi tạo" + nút "Khởi tạo mẫu vai trò" — khớp spec GAP-1119 (fixed-curated, KHÔNG có UI sửa permission per-role). Sidebar admin nav đầy đủ. Mô tả "5 mẫu vai trò cố định". **G1-FE render PASS;** mutation seed-templates → assign → revoke = G2★ (xem §4 chưa-walk).

### 3.4 LMS teacher + guest catalog (PASS)
- Teacher `/courses/1`: render shell + tab "Nội dung" (CourseContentManager) hiện.
- Guest `/catalog` (ẩn danh): render danh sách khóa ("Tìm lớp học phù hợp cho con") + card "Test Course" + paywall CTA "Liên hệ tư vấn miễn phí". Public courses `GET /api/v1/courses?status=PUBLISHED` → 200 (host-derived base URL nip.io per GAP-1207, by-design). **Lưu ý:** walk regex ban đầu báo "error text" (false-positive — page render đúng, không lỗi); screenshot xác nhận.

### 3.5 LMS student (PASS sau khi sửa setup)
- `/student/learning`: render student PWA shell + card "Test Course" (khóa đã ghi danh) + bottom nav (Hôm nay/Học tập/Lớp học/Điểm/Thông báo/Cá nhân). `GET /api/v1/enrollments/me` → 200 + 1 enrollment (Lớp Toán 10A1).
- `/student/assignments`: render đúng route, 0 net 4xx.

**Lưu ý quan trọng (KHÔNG phải bug sản phẩm):** Lần walk đầu `/student/learning` hiện empty "Bạn chưa ghi danh khóa học nào." dù credential map tới student có enrollment. Root cause = **setup error của Claude**: credential ban đầu map tới student id=4 (thuộc tenant `0edaee10` sky-education), KHÔNG phải skytest (`aaaabbbb`). RLS đã **đúng** chặn cross-tenant → `enrollments/me` trả empty (JWT tenantId=skytest, student data ở tenant khác). Đây là **tín hiệu tích cực: RLS cross-tenant isolation hoạt động đúng**. Sau khi re-map credential → student id=164 (skytest enrolled) → `enrollments/me` trả 1 enrollment + trang render đúng.

### 3.6 SSO KH→KC (BLOCKED — GAP-1305)
KH `:3001/dashboard` → bị đá `/login` (cần KH owner login trước). Nút "Mở quản lý trường" (`OpenSchoolManagementButton`) chỉ render sau KH login. **KHÔNG có KiteHub owner credential seeded** (KH login = kitehub-subscription auth, tách biệt KC tenant-auth; không account KH owner local). → KHÔNG drive được browser SSO walk ở G1-FE. File GAP-1305.

## 4. Chưa-walk (defer G2★ human) — KHÔNG phải bug

G1-FE này verify **render + routing + guard + happy-path data fetch**. Các mutation/deep-interaction sau cần G2★ human (KHÔNG mutate dev data trong G1-FE để tránh side-effect):

| Chưa-walk | Lý do defer | Recipe G2 |
|---|---|---|
| Assign/revoke role (`/admin/roles`) | Cần seed 5 mẫu trước ("Chưa khởi tạo") + mutation; G2★ human | rbac-role-shell §5 |
| Teacher content CRUD (tạo module/lesson, reorder, xóa) | Mutation course content; G2★ | lms-teacher-catalog §2-4 |
| Lesson-player mark-complete + progress | Test Course content tối thiểu (1 module, 0 lesson visible); cần content + click; G2★ | lms-student §2-3 |
| Assignment submit | Mutation; assignment count=0 trong seed; G2★ | lms-student §4 |
| STAFF scope (enrollment/attendance allow, payroll/branding 403) | Cần session STAFF (qua SSO — đang BLOCKED); G2★ | rbac-role-shell §6 |
| SSO replay-reject + tenant-scope | BLOCKED upstream (no KH login); G2★ | sso-kh-kc §2-5 |

## 5. Bug found/fixed

- **0 FE-runtime bug** (render crash / blank page / redirect loop / wrong API call / missing nav): không phát hiện. Mọi trang FE render đúng + console clean + 0 net 4xx trên happy path.
- **0 inline fix cần thiết.**
- "Student learning empty" = Claude setup error (cross-tenant credential), KHÔNG phải product bug — đã sửa setup + re-walk PASS. RLS isolation chạy đúng.
- "Catalog error text" = false-positive walk regex — screenshot xác nhận render đúng.

## 6. Discoveries filed (per discovery-to-gap-inline-filing.md §3)

- **GAP-1305**: SSO KH→KC không G1-FE-browser-walk được local — thiếu KiteHub owner credential seeded (P2, Frontend/DevOps test-infra). Block flip GAP-1138 ngay cả ở tầng G1-FE.

## 7. Trạng thái flip (per g1-browser-walk-before-flip.md)

- **KHÔNG flip gap nào → DONE** (human G2★ bắt buộc per `feature-ship-runtime-walk-mandate.md`).
- Campaign §4: thêm Log entry G1-FE (xem flow-verification-campaign.md). RBAC/LMS không có §4 row riêng (track qua gap GAP-1119/1113/1277/1285) — giữ PARTIAL + thêm note G1-FE.
- 7 gap PARTIAL (1138/1119/1113/1277/1285/1297/1298) cập nhật note "G1-FE browser walk: PASS/BLOCKED" — giữ PARTIAL.

## 8. Ready for human G2★

✅ Sẵn sàng G2★ (FE render + routing + guard + happy-data đã PASS qua browser thật):
- RBAC role-redirect (4 role) + RoleGuard cross-role
- `/admin/roles` render (mutation assign/revoke = G2★)
- LMS teacher course content tab + guest catalog
- LMS student learning + assignments (happy data)

🔴 Blocker G2★:
- **SSO KH→KC**: cần seed KiteHub owner credential trước (GAP-1305) thì human mới walk được luồng "Mở quản lý trường" → KC no-relogin.
- STAFF scope walk phụ thuộc SSO (cần session STAFF) — unblock sau khi có KH login.
