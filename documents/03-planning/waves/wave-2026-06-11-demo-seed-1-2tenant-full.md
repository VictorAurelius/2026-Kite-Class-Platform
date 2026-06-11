---
title: Wave demo-seed-1 — Seed đầy đủ 2 tenant thực tế (Hà free + Nhì paid) đúng thesis §4.3/4.4
status: draft
created: 2026-06-11
updated: 2026-06-11
waves: [demo-seed-1]
tag_primary: demo-seed
tags_secondary: [seed, landing-100, thesis]
counter: 1
gaps: [GAP-1190, GAP-1191, GAP-1192, GAP-1193, GAP-1194, GAP-1195]
references:
  - documents/04-quality/audits/2026-06-11-demo-trio-seed-coverage-audit.md
  - documents/08-thesis/chapter-4-deployment-results.md
  - documents/03-planning/waves/wave-2026-06-09-landing-100.md
---

# Wave demo-seed-1 — Seed đầy đủ 2 tenant thực tế (Hà + Nhì)

**Goal:** Tạo đầy đủ file seed (idempotent, `@Profile("dev")`) để 2 tenant **cô Hà (FREE/xanh dương)** + **thầy Nhì (PAID/xanh lá)** có data nghiệp vụ thật khớp thesis §4.3/4.4 — lớp/HV/lịch/**điểm danh**/điểm/**học phí** — thay vì landing toàn empty-state.
**Trigger:** Audit 2026-06-11 (`2026-06-11-demo-trio-seed-coverage-audit.md`, 38/100) phát hiện seed mới ở mức branding+landing-hero; academic data chưa seed + 4 lỗi asset ảnh.
**Estimated wall-clock:** ~3-4h agent (5 bucket parallel), longest-bucket ~60min.

## 1. Brainstorm (5-10 min)

**Inside-out (audit-driven):** `BrandingDataSeeder.seedTrioTenant` chỉ tạo FrontendInstance + Branding + LandingPage hero/tenant. Không lớp/HV/điểm danh/điểm/học phí → landing đẹp vỏ, rỗng ruột; không demo được nghiệp vụ trường như thesis.

**Outside-in (thesis §4.3/4.4 = spec):**
- Hà FREE: GV Toán tiểu học, **giới hạn** lớp/HV, điểm danh giao diện cơ bản, hóa đơn **thủ công** + đối soát chuyển khoản, KHÔNG báo cáo nâng cao.
- Nhì PAID: GV Hóa THCS, **không giới hạn** lớp/HV, **bảng giá nhiều mức**, báo cáo doanh thu + **tỷ lệ điểm danh nâng cao**, AI Branding xanh lá.

**Reconcile target:** seed phải đúng giới hạn gói (FREE quota vs PAID unlimited) để demo đúng phân khúc — KHÔNG seed Hà bằng quy mô Nhì.

**Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 — scope internal seed (data correctness vs thesis spec đã có), không phải user-needs mới. Thesis §4 = AC source per `thesis-as-future-state-mandate`.

## 2. Task Breakdown

| # | Task | Bucket | Effort |
|---|---|---|---|
| T1 | Seed courses + classes + students + enrollments + schedule cho Hà (FREE-limited) | A | M |
| T2 | Seed courses + classes + students + enrollments + schedule cho Nhì (PAID-unlimited, quy mô lớn hơn) | B | M |
| T3 | Seed **điểm danh** (attendance) cho buổi học 2 tenant + **điểm** (grades theo grading_scale V88) | C | M |
| T4 | Seed **học phí**: Hà invoice thủ công + payment chuyển khoản; Nhì bảng giá nhiều mức + payment records | D | M |
| T5 | Seed landing sections (teachers[] / pricing / stats) lấp empty-state — bỏ phụ thuộc onboarding manual | E | S |
| T6 | Fix asset ảnh: webp + `next/image`, gitignore `demo-banners/`, tách logo≠banner, sửa Khánh hero path durable | F | S |

## 3. Scope (compact schema)

| Bucket | Scope | Files chính | Gap | Verdict | Walk |
|---|---|---|---|---|---|
| **A. Hà academic core** | Course (Toán TH) + 2-3 class + ~10-15 student (FREE quota) + enrollment + schedule/session. Idempotent theo tenant `a1100000` | seeder (`DemoAcademicSeeder.java` mới HOẶC mở rộng `BrandingDataSeeder`) | GAP-1190 | 🆕 Greenfield | DB ✅ + G2 |
| **B. Nhì academic core** | Course (Hóa THCS) + 3-5 class + ~30-40 student (PAID unlimited) + enrollment + schedule | seeder | GAP-1190 | 🆕 Greenfield | DB ✅ + G2 |
| **C. Điểm danh + điểm** | Attendance records cho N buổi gần nhất (Nhì tỷ lệ cao hơn để demo "báo cáo nâng cao") + grades theo `grading_scales` (V88 8 bands) | seeder + ref V88 | GAP-1191/1192 | 🆕 Greenfield | DB ✅ |
| **D. Học phí** | Hà: invoice thủ công + payment BANK_TRANSFER. Nhì: pricing tiers nhiều mức + payment records (doanh thu) | seeder + invoice/payment entity | GAP-1193 | 🆕 Greenfield | DB ✅ |
| **E. Landing sections** | teachers[] + pricing + stats data vào LandingPage JSONB (bỏ empty-state khi đã có data thật) | `BrandingDataSeeder` landing JSONB + FE section render | GAP-1194 | 🔨 Delta | FE ✅ + G2 |
| **F. Asset ảnh fix** | Convert demo-banner → webp committed + `next/image`; `.gitignore` thêm `demo-banners/` (gỡ PNG khỏi track); logo riêng (nhỏ) ≠ hero banner; Khánh hero = webp committed (không 404 remote) | `.gitignore` + seeder image URLs + `public/demo-banners/*.webp` | GAP-1195 | 🔨 Delta | FE ✅ + G2 |

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verify | Verdict |
|---|---|---|
| `BrandingDataSeeder.seedTrioTenant` | đã đọc — branding+landing hero only, KHÔNG academic | ✅ exists (mở rộng / seeder mới) |
| `OnboardingServiceImpl.importSampleData` | teacher/course/class/3 student, KHÔNG attendance/grade/invoice | ✅ exists (pattern reuse cho seeder) |
| `grading_scales` (V88 seed 8 bands) | migration đã seed default scales | ✅ exists (ref cho grades) |
| Entity Attendance/Grade/Invoice/Payment + service create | đã grep — entity + service tồn tại (CreateXRequest) | ✅ exists (seeder gọi service) |
| kitehub `instances` demo-trio rows | GAP-1180 — chưa có seeder, by-subdomain 404 | ❌ **prerequisite** (GAP-1180 phải land trước) |
| UUID scheme demo-trio | 2 nguồn mismatch (`a1100000` vs `ad0fa96e`) | ⚠️ reconcile trong GAP-1180 |

## 5. Verification Gates (per bucket)

| Gate | Cách |
|---|---|
| G1 (Claude) | `pnpm --filter kiteclass-frontend build` PASS + `./mvnw -pl kiteclass-core test` PASS; dev profile boot → `psql` assert row count: Hà ≥10 student + ≥N attendance; Nhì ≥30 student + grades + invoices. Idempotent (re-run boot không duplicate). |
| G2 (human) | Recipe riêng sau G1 (per `g2-handoff-md-mandate` + `g1-browser-walk-before-flip` §3.2 nip.io subdomain): walk landing Hà + Nhì qua FE `:3000` → dashboard hiện lớp/HV/điểm danh/học phí thật, KHÔNG empty + KHÔNG demo bịa. |
| G3 (parity) | Đối chiếu Hình 4.3 (Hà xanh dương) + Hình 4.4 (Nhì xanh lá) — branding + data khớp; rubric landing ≥90/100. |

## 6. Agent Spawn Pattern

Parallel agents (Opus per `agent-model-opus-default`, worktree-isolated, ≤5 concurrent, per `agent-background-spawn-default`):

**Đợt 1 (4 agent — file disjoint):**
- Agent A+B: academic core seeder (Hà + Nhì) — cùng file seeder, batch 1 agent tránh conflict
- Agent C: attendance + grades seed
- Agent D: học phí (invoice + payment) seed
- Agent F: asset ảnh fix (`.gitignore` + webp + seeder image URLs) — disjoint

**Đợt 2 (1 agent):**
- Agent E: landing sections JSONB + FE section render (cùng vùng LandingPage/page.tsx)

Mỗi agent: `fe-build-local-verify` + `cross-flow-bug-class-sweep` (seed pattern → sweep mọi tenant) + `contract-first-for-cross-layer` (FE section ↔ LandingPage JSONB shape).

## 7. Closure Protocol

- ⚠️ **Prerequisite hard:** GAP-1180 (kitehub instances seeder + UUID reconcile) PHẢI land trước — by-subdomain resolve thì academic seed mới walk G2. Coordinate với session GAP-1180 (PR #2312).
- File 6 gap GAP-1190..1195 + CSV rows (per `discovery-to-gap-inline-filing` + reserve block 1190-1199 per `multi-session-concurrency-coordination`).
- Flip gap DONE sau G1+G2 (per `feature-ship-runtime-walk-mandate` — academic seed = user-facing feature, walk bắt buộc).
- Scope-Completeness Reconciliation table (per `wave-closure-scope-completeness` §3) khi flip `status: complete`.
- Sync gap-status.csv + ROADMAP + wave-history (per `post-merge-sync-completeness`).
- `bash scripts/prune-merged-worktrees.sh --yes` (per `post-wave-cleanup`).

### Sequencing constraint
⚠️ Wave rebuild kiteclass-core nhiều lần → chạy SAU khi GAP-1180 land + sau khi user xác nhận scope. Pre-walk persona simulation (per `pre-walk-persona-simulation-mandate`) trước G2 walk.

## 8. Log
- **2026-06-11 (draft):** Wave plan drafted từ audit `2026-06-11-demo-trio-seed-coverage-audit.md` (38/100). 6 bucket (academic core Hà/Nhì + điểm danh/điểm + học phí + landing sections + asset fix). Gap block 1190-1199 reserved (disjoint khỏi GAP-1180-1189 session song song). Prerequisite GAP-1180 (kitehub instances resolve). Status `draft` → `complete` ở closure sau 6 bucket ship + G1+G2 PASS. Chờ user xác nhận scope + GAP-1180 land trước khi spawn.
