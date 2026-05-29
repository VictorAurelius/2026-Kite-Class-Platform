---
audience: dev
title: RST walk findings — demo-trio GAP-804/805/807 (Sky Education demo tenant)
status: complete
created: 2026-05-29
gaps: [GAP-804, GAP-805, GAP-807]
---

# RST walk — demo-trio (GAP-804/805/807) Sky Education

**Tóm tắt:** Walk runtime end-to-end 3 gap demo tùy biến UI (branding theme + logo upload + KPI real-data) trên local stack production-equivalent per `feature-ship-runtime-walk-mandate.md`. 3 gap shipped PARTIAL với `mvn test`/`build` PASS nhưng **chưa từng walk live** — walk surface **18 bug** (toàn bộ demo data pipeline + 1 core FE bug khiến GAP-807 chưa từng hoạt động trên browser). Sau fix-forward: cả 3 verified 3 lớp (BE curl → API e2e gateway → visual browser).

## Bug catalog (18) — catalog-then-batch per §3.4

| # | Bug | Loại | Fix |
|---|---|---|---|
| 1 | kiteclass-core thiếu `SPRING_PROFILES_ACTIVE: dev` → BrandingDataSeeder (@Profile dev) không chạy → instance Sky không tồn tại | compose | + `SPRING_PROFILES_ACTIVE: dev` |
| 2 | seed-thesis gán `classes.teacher_id`(UUID) = `teachers.id`(bigint) | seed SQL | `NULL::uuid` |
| 3 | seed-thesis class status `'ongoing'` ∉ `chk_classes_status` | seed SQL | `'IN_PROGRESS'` |
| 4 | seed-enrich heredoc `<<'PLPGSQL'` quoted → `${SKY_ID}` không expand | seed bash | hardcode UUID |
| 5 | `grades` unique index `(student_id,class_id)` thiếu `grade_type` → chặn 3 loại điểm/HS | entity+schema | Grade.java `@UniqueConstraint` + V74 migration |
| 8 | seed-enrich lặp Bug #2 (classes.teacher_id) | seed SQL | `NULL::uuid` |
| 9 | seed-enrich lặp Bug #3 (class status) | seed SQL | `'IN_PROGRESS'` |
| 12 | MinIO bucket `kite-branding-assets` chưa provision → logo upload 500 NoSuchBucket | infra | tạo bucket (cần auto-create/init follow-up) |
| 13 | S3 presigned logo URL dùng internal endpoint `kite-minio:9000` (virtual-host) → browser không resolve | code+config | `StorageProperties.publicEndpoint` + `S3Config.brandingPresigner` (public endpoint + path-style); compose `STORAGE_S3_PUBLIC_ENDPOINT=localhost:9100` |
| 14 | courses seed status `'ACTIVE'` ∉ `CourseStatus` enum (DRAFT/PUBLISHED/ARCHIVED) → courses API 500 lúc đọc | seed SQL | `'PUBLISHED'` (5 occurrence) + DB UPDATE |
| — | **Tenancy mismatch**: demo seed `instance_id=a5e00000` (UUID bịa) ≠ gateway `instances.id=e8ff87e1` cho subdomain sky-education → browser→gateway→core trả rỗng | architecture | audit-confirmed canonical shared-DB+RLS; align-seed-to-gateway: re-point a5e00000→e8ff87e1 (11 bảng) + seeders + owner.sky |
| 16 | **GAP-807 envelope-unwrap (core bug)**: `brandingApi.get()` `return data` trả raw `{success,data,timestamp}` thay vì `data.data` → `branding.primaryColor` undefined → theme không apply | FE | unwrap 4 method theo convention `studentsApi` (`ApiResponse<T>` + `.data.data!`) |
| 17 | theme `hexToHslString`/`hexToRgb` crash trên hex undefined → React error boundary "Application error" toàn dashboard | FE | null-guard 3 hàm (BrandingProvider + utils + ThemeSync) |

(Bug #6/#7/#10/#11/#15 = walk-continuation aids/curl part-name, không phải code defect — gộp vào numbering walk session.)

## Meta-findings

- **GAP-807 trust-pass**: shipped PARTIAL 75% với "BrandingThemeApplier test 2/2 PASS + build PASS" nhưng envelope-unwrap bug (#16) khiến theme **chưa từng apply + crash dashboard** trên browser thật. Vitest mock `brandingApi.get` trả inner shape → che bug envelope. Đúng class `feature-ship-runtime-walk-mandate.md` §1 (audit/test PASS ≠ feature works).
- **FE↔BE contract drift (pre-existing, KHÔNG fix phiên này)**: FE gọi `GET /api/v1/classes?page=` + `GET /api/v1/invoices?page=` (flat list) nhưng `ClassController` chỉ có `/courses/{id}/classes` + `/classes/{id}`; `InvoiceController` không có `GET /` list. → 404. Sau crash-guard fix các 404 này handle graceful (dashboard không crash) nhưng page `/classes` + `/billing` sẽ rỗng. → follow-up gap.
- **Logo preview trống**: logo_url persisted là PNG test 1×1 ở path `a5e00000` cũ (pre-tenancy-repoint). Upload mechanism proven (curl 200 + presigned browser-reach 200); cosmetic test-artifact, không phải feature bug.

## Walk evidence (3 lớp)

**Lớp 1 — BE (curl, direct core :8088 + X-Tenant-Id=e8ff87e1):**
- GAP-807: PUT branding → 200 + DB row `primary_color=#E8590C`; GET → `#E8590C`
- GAP-804: POST `/logo` multipart → 200 + MinIO object `static/.../logo/sky-logo.png` + presigned URL
- GAP-805: students totalElements=78, courses=5; enrich DB: attendance 450, grades 300, invoices 75, payments 56

**Lớp 2 — API e2e qua gateway (:9000, login owner.sky + X-Instance-Subdomain: sky-education):**
- branding → `#E8590C` cam; students=78; courses=5; presigned URL `localhost:9100/...` GET → 200 (browser-reach)

**Lớp 3 — visual browser (Playwright, `documents/08-thesis/evidence/demo-trio/`):**
- `02-dashboard-overview-kpi-orange.png`: "Trung tâm hiện có **78 học viên · 5 khóa học**" + KPI cards 78/5 (real data, không phải 428/24 literal)
- `03-branding-settings.png`: nút "Mở wizard" CAM + icons cam (theme applied)
- `04-settings.png`: Tên "Trung tâm Anh ngữ Sky Education" + Slogan + **Màu chính `#E8590C`** swatch cam + Màu phụ/nhấn + nút "Lưu thay đổi" cam
- `05-students.png`: trang Học viên — **77 HS tên VN seed thật** (Bùi Văn Dũng / Cao Văn Sơn / Châu Thị Bích / Dương Thị Kim…) + email `@sky-enrich.demo` + SĐT + "Đang học" + nút "+ Thêm học viên" cam
- `06-classes.png`: trang Lớp học course-scoped ("Chọn khóa học…" — khớp BE `/courses/{id}/classes`, không phải bug); `07-courses` / `08-attendance` / `09-billing` / `10-teachers` shell + data per page
- `--primary` CSS var = `21 90% 48%` (HSL của #E8590C) — theme apply confirmed; no client-side crash

## Verdict
- **GAP-804/805/807 → DONE** (visual-verified 3 lớp).
- Tenancy canonical = shared-DB+RLS (audit). Demo reachable browser→gateway→core qua subdomain sky-education (instance e8ff87e1).
- Owner demo: `owner.sky@test.vn` / `Test@1234` (tenant e8ff87e1).

## Public tenant homepage chain (GAP-808 — DONE same session)

User clarify muốn **trang chủ public** (không phải dashboard). Walk surface chuỗi 6 bug khiến `/?tenant=` render generic KiteClass xanh thay vì Sky:
1. `landing_pages` table thiếu migration → API 500 → V75 tạo bảng.
2. `getOrCreateDefault` hardcode default → inherit branding tenant.
3. `getLandingPage` `@Transactional(readOnly=true)` + INSERT → writable tx.
4. Gateway `/tenants/{id}/landing` catch-all TenantResolver 400 no-subdomain → public route skip TenantResolver.
5. FE `public.ts` baseURL localhost:9000 cho SSR → ECONNREFUSED → SSR-aware INTERNAL_API_URL.
6. `ThemeSync` chỉ `--theme-*` RGB → thêm `--primary` HSL (shadcn buttons branded).
→ Homepage fully Sky-branded: hero "Trung tâm Anh ngữ Sky Education" + `--primary: 21 90% 48%` cam toàn theme (`12-public-homepage-sky-branded.png`). Residual: layout nav header hardcode "KiteClass" (TemplateRenderer hero/theme branded, nav not tenant-driven — minor follow-up).

## Public layout nav tenant-branded (GAP-808 extension — DONE same session)

User-flagged: `(public)/layout.tsx` nav header + footer hardcode "KiteClass" + "Powered by Claude Code". Fixed: layout async + `getTenantIdentity()` fetches landing (default tenant via `NEXT_PUBLIC_TENANT_ID` — production model = per-tenant FE deploy) → renders displayName + logo (fallback "KiteClass"); removed Claude Code project-internal ref; `NEXT_PUBLIC_TENANT_ID=e8ff87e1` set in compose (Sky demo deploy). Verified: nav header "Trung tâm Anh ngữ Sky Education" + footer branded + theme cam (`13-public-homepage-nav-branded.png`).

## Follow-ups
- **GAP-809** (P2): FE↔BE contract drift `/classes` + `/invoices` flat-list 404 (BE course/student-scoped only).
- MinIO `kite-branding-assets` auto-create on startup (Bug #12 — provision thủ công phiên này).
- Logo re-upload real file (preview 1×1 test PNG; path a5e00000 cũ pre-repoint).
- `STORAGE_S3_PUBLIC_ENDPOINT` production value (CDN/public domain) khi GA.
