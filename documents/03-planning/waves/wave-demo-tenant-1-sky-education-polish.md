---
audience: dev
title: Wave demo-tenant-1 — Sky Education polished demo tenant
status: in-progress
created: 2026-05-28
waves: [demo-tenant-1]
tag_primary: demo-tenant
tags_secondary: [kiteclass, branding, seed, thesis]
---

# Wave demo-tenant-1 — Sky Education polished demo tenant

**Mục tiêu:** Dựng 1 tenant demo KiteClass "Trung tâm Anh ngữ Sky Education" **đẹp hoàn chỉnh + tùy biến UI rõ rệt + dữ liệu phong phú**, đủ thuyết phục prospect chủ trung tâm + hội đồng bảo vệ đồ án rằng đây là multi-tenant SaaS thật (không phải mockup), chứng minh năng lực tùy biến UI của KiteClass.

## 1. Brainstorm (inside-out + outside-in)

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

## 2. Task Breakdown

3 bucket disjoint, 3 agent Opus song song (worktree):
- **A — Branding seed** (`BrandingDataSeeder.java`) ~1h: seed branding Sky tenant.
- **B — Deep data seed** (`scripts/seed-sky-demo-enrich.sh`) ~1.5h: attendance/grade/payment + enrich.
- **C — KPI + asset** (`overview/page.tsx` + `scripts/seed-demo-assets.sh`) ~1.5h: fix KPI hardcode + asset MinIO.
- Coordinator: integrate disjoint files + reconcile tenant id + PR.

## 3. Scope

| Bucket | Scope | File |
|---|---|---|
| **A** | Seed branding Sky (tên/3 màu VN edu palette/tagline/logoUrl direct) → theme custom live, logo show (né GAP-804) | `BrandingDataSeeder.java` |
| **B** | attendance + grade + payment + enrich (3-4 lớp, ~20-30 HS/lớp tên VN) | `scripts/seed-sky-demo-enrich.sh` |
| **C** | Fix overview KPI hardcode (wire API thật/derive) + asset seed-direct MinIO | `overview/page.tsx` + `scripts/seed-demo-assets.sh` |

Out-of-scope wave này: #3 E2E full-flow, GAP-804 logo-upload real fix.

## 4. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `BrandingDataSeeder.java` | ✅ exists | `kiteclass/kiteclass-core/.../dev/seeder/BrandingDataSeeder.java` |
| `seed-thesis-demo-tenants.sh` | ✅ exists | `scripts/seed-thesis-demo-tenants.sh` |
| `BrandingController` getBranding/getThemeConfig public | ✅ exists | `kiteclass-core .../module/settings/controller/BrandingController.java` |
| overview KPI hardcode | ✅ exists (failure-mode agent `overview/page.tsx`) | bucket C fixed |
| `seed-sky-education-demo.sh` | 🟡 PR #1952 pending (not on main) | dependency — build trên `seed-thesis-demo-tenants.sh` thay thế |
| attendance/grade/payment schema | ✅ verified (Flyway V1/V64/V69 — bucket B) | `attendance`/`grades`/`payment_records` tables |

## 5. Verification Gates

- A: `./mvnw -o compile` PASS (agent-verified).
- B: `shellcheck seed-sky-demo-enrich.sh` clean; dry-run OK.
- C: `pnpm --filter kiteclass-frontend build` PASS + 3/3 component test; `shellcheck seed-demo-assets.sh` clean.
- Integrated branch: CI #1959 (Build Core Service + Test Core Service + Frontend Tests & Build + E2E + Security all PASS).
- **Live-walk (deferred — stack down, per `feature-ship-runtime-walk-mandate.md` §5):** owner login → branding custom render + KPI khớp DB + attendance/grade/payment có data. Reconcile tenant id trước (xem §7).

## 6. Agent Spawn Pattern

3 agent Opus 4.7 (per `agent-model-opus-default.md`), `isolation: worktree`, background. Disjoint files → coordinator collect (checkout per-file) + integrate. Mismatch tenant id giữa A (branding) và B (data) reconcile ở coordinator/live-walk.

## 7. Closure Protocol

- PR #1959 (feat/gap-805-demo-tenant → main). GAP-805 **PARTIAL** tới khi:
  1. **Reconcile tenant id** A-branding (`a5e0…0001`) ↔ B-data (`1111…`) — branding + data CÙNG tenant (3 options trong GAP-805 §Open item).
  2. Live-walk pass (stack up) → flip GAP-805 DONE.
- Merge #1959: sau review tenant-id approach + CI green (UNSTABLE chỉ do Trivy NEUTRAL — non-blocking).

## 8. Log

- **2026-05-28:** Wave tạo từ user direction "lập wave demo-tenant + spawn agents". Outside-in 2 agent (persona-sim + failure-mode) → scope P0/P1. 3 agent Opus build A/B/C song song → PR #1959. GAP-805 PARTIAL (live-walk + tenant-id reconcile deferred stack down).
