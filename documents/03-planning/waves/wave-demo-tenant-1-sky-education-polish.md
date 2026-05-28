---
audience: dev
tag_primary: demo-tenant
tags_secondary: [kiteclass, branding, seed, thesis]
status: in-progress
created: 2026-05-28
---

# Wave demo-tenant-1 — Sky Education polished demo tenant

**Mục tiêu:** Dựng 1 tenant demo KiteClass "Trung tâm Anh ngữ Sky Education" **đẹp hoàn chỉnh + tùy biến UI rõ rệt + dữ liệu phong phú**, đủ thuyết phục prospect chủ trung tâm + hội đồng bảo vệ đồ án rằng đây là multi-tenant SaaS thật (không phải mockup), chứng minh năng lực tùy biến UI của KiteClass.

## §1. Brainstorm (inside-out + outside-in)

### Inside-out (dev)
- Seed hiện có trên main: `seed-thesis-demo-tenants.sh` (2 tenant Sky + Quang Minh, depth chuẩn) + `BrandingDataSeeder.java` (chỉ seed tenant `thanglong`). `seed-sky-education-demo.sh` ở PR #1952 (pending).
- KiteClass branding capability ĐÃ CÓ: `BrandingController`/`BrandingService` (getBranding + getThemeConfig **public no-auth**), `BrandingWizard` 6-bước FE, `BrandingVersionController` (history/rollback), `branding-settings.tsx`.
- Blocker: GAP-798b chặn upload asset qua StorageController (KHÔNG chặn branding — đã verify). GAP-804 logo upload contract drift (workaround: seed logoUrl direct). GAP-803 /reset-password resolved + 3 env deadlink.

### Outside-in findings (2 agent: persona-sim + failure-mode, 2026-05-28)
**P0 (không làm → hội đồng/prospect thấy ngay là giả):**
1. **Theme mặc định** — `BrandingDataSeeder` chỉ seed `thanglong`; tenant Sky dùng theme xanh shadcn default → "tùy biến UI" KHÔNG thể hiện.
2. **Dashboard overview KPI HARDCODE** literal ("12 buổi · 3 lớp chờ điểm danh · doanh thu tuần") không khớp DB → soi ra ngay là fake.
3. **Attendance + grade + payment TRỐNG** — seed không tạo → 3 surface nghiệp vụ cốt lõi rỗng.

**P1:** data nông (2-4 record); logo/cover/hero ảnh vỡ (GAP-798b); avatar fallback "KC"; public hero null.

**Must-have đã verify capability sẵn sàng:** tên+3 màu+tagline apply live (CSS vars + public theme endpoint), wizard VN, version history, multi-tenant isolation (2 tenant tách data).

## §2. Scope — P0/P1

| Bucket | Scope | Đề xuất file |
|---|---|---|
| **A — Branding seed** | Seed branding cho tenant Sky (tên/3 màu VN edu palette/tagline/logoUrl direct) → theme tùy biến hiển thị live, logo show (né GAP-804) | `BrandingDataSeeder.java` (kiteclass-core) |
| **B — Deep data seed** | Thêm attendance + grade + payment + enrich (3-4 lớp, ~20-30 HS/lớp tên VN) cho Sky tenant | seed script (bash, build trên `seed-thesis-demo-tenants.sh` pattern) |
| **C — KPI + asset** | Fix overview KPI hardcode (wire API thật OR derive từ data) + asset seed-direct MinIO (logo/cover/hero) OR gradient placeholder | `kiteclass-frontend` overview + `scripts/seed-demo-assets.sh` |

#3 E2E + GAP-804 logo-upload real fix: out-of-scope wave này (deferred).

## §3. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `BrandingDataSeeder.java` | ✅ exists | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/dev/seeder/BrandingDataSeeder.java` |
| `seed-thesis-demo-tenants.sh` | ✅ exists | `scripts/seed-thesis-demo-tenants.sh` |
| `BrandingController` getBranding/getThemeConfig public | ✅ exists | `kiteclass-core .../module/settings/controller/BrandingController.java` |
| overview KPI hardcode | ✅ exists (per failure-mode agent `overview/page.tsx`) | bucket C verify at execution |
| `seed-sky-education-demo.sh` | 🟡 PR #1952 pending (not on main) | dependency — build trên `seed-thesis-demo-tenants.sh` thay thế |
| attendance/grade/payment API | 🆕 verify-at-exec (GradeController exists per failure-mode agent) | bucket B owns verification |

## §4. Live-walk note (per feature-ship-runtime-walk-mandate §5)

Stack hiện DOWN (3 EC2 + RDS stopped, local docker chưa up). Build code wave này KHÔNG thể live-walk ngay → demo-tenant gap (GAP-805) giữ **PARTIAL** tới khi stack up + walk (owner login → thấy branding tùy biến + dashboard KPI khớp + attendance/grade/payment có data). Defer hợp lệ per §5 env-constraint.

## §5. Gap

- **GAP-805** — Sky Education demo-tenant polish (P0 branding+KPI+data seed). Wave này deliver.
