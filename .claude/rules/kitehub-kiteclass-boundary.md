---
paths:
  - "kitehub/kitehub-frontend/**"
  - "kiteclass/kiteclass-frontend/**"
  - "documents/03-planning/waves/wave-*-flow-*.md"
  - "documents/03-planning/roadmap/flow-verification-campaign.md"
  - "documents/05-guides/operations/*g2-recipe*.md"
  - "documents/02-architecture/**"
  - ".claude/rules/kitehub-kiteclass-boundary.md"
---

# KiteHub vs KiteClass Boundary — canonical KH/KC distinction (FE port / service / domain / concern)

**Priority:** 🟠 MANDATORY — product-boundary disambiguation governance
**Version:** 1.0.0
**Created:** 2026-06-08
**Last-Reviewed:** 2026-06-08
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (canonical matrix §2 + reviewer-checklist + worked self-test on 2026-06-09 KH-3/KC-8 recipe port drift) per §6.5 Enforcement Parity Mandate; no constraint loosening — codify previously-informal KH/KC distinction (chỉ ở CLAUDE.md prose, không enforce) thành canonical mapping + hard guard; META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi artifact / quyết định cần phân biệt KiteHub (KH) vs KiteClass (KC): FE port reference, service routing, domain, flow assignment (KH-* vs KC-*), G2/G1 recipe, wave-flow plan, architecture doc, frontend code. Out-of-scope: shared infrastructure (`kite-*` prefix — postgres/redis/rabbitmq/minio/gateway dùng chung).

---

## 1. The Rule

> **KiteHub (KH) và KiteClass (KC) là HAI sản phẩm tách biệt — KHÔNG được lẫn FE port, service, domain, hay concern. Mọi reference tới port/FE/domain/flow PHẢI khớp §2 canonical matrix.** Khi 1 khái niệm tồn tại ở cả hai (vd "billing", "branding", "login") PHẢI nêu rõ thuộc KH hay KC theo §2.1.

KiteHub = SaaS platform quản lý lifecycle (trial / subscription / billing-SaaS / domain / AI branding / admin). KiteClass = multi-tenant education platform xử lý nghiệp vụ trường (course / student / attendance / grade / invoice-học-phí / parent-portal). Lẫn hai = sai port (`:3000` vs `:3001`), sai service routing, sai flow assignment → recipe gửi người walk tới nhầm app, gap gán nhầm domain.

Recurrence 2026-06-09: 2 G2 recipe ghi sai FE port (KH-3 subscription ghi `:3000` thay vì `:3001`; KC-8 parent portal ghi `:3001` thay vì `:3000`) — chứng tỏ nhận thức KH/KC chưa cứng ở meta. Rule này = canonical reference + hard guard.

---

## 2. Canonical distinction matrix

| Chiều | **KiteHub (KH)** | **KiteClass (KC)** |
|---|---|---|
| **Vai trò** | SaaS platform — quản lý instance lifecycle | Multi-tenant education — nghiệp vụ trường, mỗi tenant = 1 trường |
| **FE container** | `kitehub-frontend` | `kiteclass-frontend` |
| **FE port (local)** | **`:3001`** | **`:3000`** |
| **Backend service(s)** | `kitehub-{platform,subscription,branding,email,admin}` + `kitehub-gateway` | `kiteclass-core` |
| **Domain (prod)** | apex `kitehub.me` (marketing + customer portal) | `{slug}` subdomain per-tenant (school app) |
| **API prefix chính** | `/api/platform/*`, `/api/auth/*` (kitehub-subscription auth) | `/api/v1/*`, `/api/v1/tenant-auth/*` (kiteclass-core) |
| **Login** | `:3001/login` (`(auth)/login`, `POST /api/auth/login`) | `:3000` tenant app (`POST /api/v1/tenant-auth/login`) |
| **Flow IDs** | KH-1 .. KH-10 | KC-1 .. KC-12 |
| **Concern** | trial, subscription, billing-SaaS, domain mgmt, AI branding wizard, admin console, beta funnel, off-boarding/PDPL, notification platform | course/class/schedule, student enrollment, attendance, grade/report-card, invoice học phí, parent/student portal, per-tenant branding, reschedule/payroll |
| **Gateway** | shared `kite-gateway` `:9000` (route theo path-prefix tới đúng service) | shared `kite-gateway` `:9000` |

> Gateway `:9000` + infra `kite-*` (postgres/redis/rabbitmq/minio) **dùng chung** — KHÔNG thuộc riêng KH hay KC.

### 2.1 Khái niệm trùng tên — bẫy thường gặp

Một số từ tồn tại ở CẢ hai nhưng nghĩa khác hẳn — PHẢI nêu rõ KH hay KC:

| Từ | KiteHub (`:3001`) | KiteClass (`:3000`) |
|---|---|---|
| **billing** | Subscription SaaS payment — tenant trả tiền cho KiteHub (`(customer)/billing/upgrade`, `POST /api/platform/subscriptions`, gói FREE/BASIC/PREMIUM) | Invoice học phí — trường thu tiền học sinh (`(dashboard)/billing`, `useInvoices`, KC-7) |
| **branding** | AI Branding wizard (KH-6, kitehub-branding generate→apply) | Per-tenant branding apply (KC-10, kiteclass landing theme) |
| **login** | Owner/Admin platform login (`/api/auth/login`) | Tenant member login (`/api/v1/tenant-auth/login` — teacher/parent/student) |
| **payment** | SaaS subscription payment (SePay → subscription PAID) | Học phí payment record/reconcile (KC-7 invoice) |
| **dashboard** | Customer/Admin portal home `:3001` | Tenant school dashboard `:3000` |
| **settings** | Subscription/domain/account settings (KH) | Tenant config/academic settings (KC-1) |

Khi viết recipe / gap / doc đụng các từ này → BẮT BUỘC prefix "KH" hoặc "KC" + port đúng.

---

## 3. Trigger pattern — khi nào rule fires

| Pattern | Ví dụ |
|---|---|
| Viết/sửa G2/G1 recipe có FE port reference | `:3000` vs `:3001` cho flow |
| Gán flow vào wave-flow plan (KH-* vs KC-*) | wave-flow plan §3 scope |
| Reference domain / service / API trong doc | architecture, deploy runbook |
| Frontend code touch (route, API call, env) | kitehub-frontend vs kiteclass-frontend |
| Dùng từ trùng tên (billing/branding/login/payment/dashboard/settings) | recipe step, gap problem |
| Assign gap vào domain (KH service vs KC service) | gap filing |

Rule **KHÔNG** fires khi: shared infra (`kite-*`), gateway routing config (path-prefix dùng chung), docs thuần non-product (CI, meta-rule không đụng KH/KC).

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Ghi `localhost:3000` cho subscription/billing-SaaS/domain/admin (KH) | `:3001` — KH = kitehub-frontend |
| Ghi `localhost:3001` cho course/attendance/grade/parent-portal/tenant-settings (KC) | `:3000` — KC = kiteclass-frontend |
| "FE Next.js của KiteHub" + port `:3000` (mâu thuẫn) | KiteHub = `:3001`; `:3000` = KiteClass |
| "billing" trống không nêu KH hay KC | "billing SaaS (KH `:3001`)" hoặc "invoice học phí (KC `:3000`)" |
| Gán KC-* flow vào kitehub-frontend route | KC-* (course/student/attendance/...) ở kiteclass-frontend trừ khi platform-side (vd KC-2 staff = kitehub-subscription) |
| Gọi `/api/v1/*` cho subscription | subscription = `/api/platform/subscriptions` (KH) |
| Assume parent/student login = `/api/auth/login` | tenant member = `/api/v1/tenant-auth/login` (KC) |
| Tạo gap "fix billing" không rõ sản phẩm | "fix subscription billing (KH-3)" hoặc "fix invoice billing (KC-7)" |

### 4.1 Lưu ý exception platform-side KC flows

Vài KC-* flow chạy **platform-side** (backend = kitehub-subscription, FE = kitehub-frontend `:3001`) dù mang nhãn "KC":
- **KC-2 staff invitation** — FE `kitehub-frontend (public)/staff/accept-invite` + `(admin)/admin/staff` (`:3001`); backend kitehub-subscription. (Per `flow-verification-campaign.md` §3 "platform-side, decoupled từ KC-1".)

Khi gặp KC-* flow nghi platform-side → state-check FE route thật (`find */src/app -ipath`) trước khi gán port, KHÔNG assume theo prefix KH/KC.

---

## 5. Worked self-test — KH-3 / KC-8 recipe port drift (2026-06-09, originating incident)

**Scenario:** 2 G2 recipe ghi sai FE port:
- KH-3 subscription recipe dòng 135: `http://localhost:3000/login (FE Next.js của KiteHub)` — mâu thuẫn (`:3000`=KC nhưng nhãn KH).
- KC-8 parent portal recipe dòng 37: `http://localhost:3001/parent` — sai (`:3001`=KH nhưng parent portal=KC).

**Apply §2 matrix retroactively:**
- KH-3 = subscription = KiteHub concern → FE = kitehub-frontend = **`:3001`**. State-check confirm `kitehub-frontend (customer)/billing/upgrade` → `POST /api/platform/subscriptions`. Recipe `:3000` SAI. ✅ rule bắt.
- KC-8 = parent portal = KiteClass concern → FE = kiteclass-frontend = **`:3000`**. State-check confirm `kiteclass-frontend (dashboard)/parent/*`. Recipe `:3001` SAI. ✅ rule bắt.
- KC-2 staff = platform-side exception §4.1 → FE kitehub-frontend `:3001` ĐÚNG. ✅ rule không false-positive.

**Counterfactual với rule active từ đầu:** recipe author đọc §2 matrix → KH-3 dùng `:3001`, KC-8 dùng `:3000` ngay từ đầu → 0 user round-trip (thay vì user push flag + sweep + fix 2 file).

**Verdict:** Rule fires correctly trên originating incident (bắt 2 lỗi + không false-positive KC-2). Self-test PASS ✅. Prospective: mọi recipe/gap/doc subsequent reference port/service theo canonical matrix → eliminate KH/KC confusion class.

---

## 6. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 6.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching frontend code / flow doc / recipe / architecture doc:

- [ ] FE port reference khớp §2 matrix (KH=`:3001`, KC=`:3000`)?
- [ ] API prefix khớp (KH `/api/platform`+`/api/auth`, KC `/api/v1`+`/api/v1/tenant-auth`)?
- [ ] Từ trùng tên (billing/branding/login/payment/dashboard/settings) có prefix KH/KC rõ per §2.1?
- [ ] Flow gán đúng FE (KC-* platform-side exception §4.1 đã state-check FE route thật)?
- [ ] Domain reference khớp (KH apex `kitehub.me`, KC `{slug}` subdomain)?

### 6.2 Cross-reference enforcement (paired same-PR)

- `g2-handoff-md-mandate.md` §8.1 + `g1-browser-walk-before-flip.md` §7.1 — thêm reviewer-checklist row "FE port khớp flow per `kitehub-kiteclass-boundary.md` §2".

### 6.3 Memory auto-load (paired same-PR)

Memory `feedback_kitehub_kiteclass_boundary.md` reminds canonical matrix tại session start.

### 6.4 Override mechanism

Genuine exception (vd shared component, gateway config dùng chung):

```
git commit -m "...
KH_KC_BOUNDARY_OVERRIDE: <reason — e.g. 'shared gateway route config, không thuộc riêng KH/KC'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review.

### 6.5 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)

- **Complexity:** detect port-vs-flow mismatch cần map flow-id → expected-FE → verify port reference; moderate (cần flow registry + KC platform-side exception handling).
- **Recurrence:** 2 today (KH-3 + KC-8 same incident).
- **FP risk:** moderate (platform-side KC exception §4.1 → naive grep flags KC-2 :3001 false-positive).
- **Decision:** reviewer-checklist §6.1 + cross-ref §6.2 + memory §6.3 + worked self-test §5 sufficient cho v1.0.0; revisit detector khi recurrence ≥3 post-rule.

---

## 7. Relationship to other rules

- **`g1-browser-walk-before-flip.md`** v1.1.0 — browser-walk evidence cần port đúng; rule này cung cấp canonical port mapping. Cross-ref §7.1.
- **`g2-handoff-md-mandate.md`** v1.0.0 — G2 recipe format; rule này cung cấp port-correctness check. Cross-ref §8.1.
- **`cross-flow-bug-class-sweep.md`** v1.1.0 — port-drift là bug-class; sweep mọi recipe khi 1 site sai (đã apply 2026-06-09 sweep bắt KC-8).
- **`design-first-investigation-order.md`** v1.0.0 — state-check FE route thật (design/code) trước khi gán port, đặc biệt KC platform-side exception §4.1.
- **`flow-verification-campaign.md`** §3 — flow dependency graph + platform-side KC-2 note; rule này canonical-hóa port mapping per flow.
- **CLAUDE.md §Project Overview + §Docker Naming** — prose KH/KC distinction; rule này codify thành enforceable matrix + guard.
- **`incident-to-rule-pipeline.md`** v1.1 — rule này = direct output 2026-06-09 KH-3/KC-8 port drift qua 5-stage.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + memory + worked self-test §5 + rules-index.csv row + output-review-mandate §3 row + cross-ref updates all paired same PR.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 canonical matrix → mọi reference subsequent auto-comply).
- **`context-budget-mandate.md`** §3.2 — path-scoped (frontend code + flow/recipe/campaign + architecture) — 0 base-context cost.
- **`feedback_kitehub_kiteclass_boundary.md`** (memory, paired same-PR).

---

## 8. Log

- **2026-06-09 (v1.0.0):** Rule created in response to user-flagged 2026-06-09 "nhận thức meta cấp chưa đủ cứng rắn để phân biệt rõ KH và KC, nên update" — sau khi 2 G2 recipe ghi sai FE port (KH-3 subscription `:3000` thay vì `:3001`; KC-8 parent portal `:3001` thay vì `:3000`). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged port drift + recurrence ≥2) → Classify ✓ (no existing rule codifies KH/KC canonical distinction; CLAUDE.md §Project Overview prose-only, không enforce port/FE/API mapping; `flow-verification-campaign.md` §3 mentions platform-side KC-2 nhưng không canonical-hóa) → Rule+Enforce ✓ (this file + §2 canonical matrix + §2.1 trùng-tên trap + reviewer-checklist §6.1 + cross-ref g2-handoff-md-mandate + g1-browser-walk + memory `feedback_kitehub_kiteclass_boundary.md` + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§5 worked example — bắt 2 lỗi KH-3/KC-8 + không false-positive KC-2 platform-side; counterfactual 0 round-trip) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 canonical matrix → mọi recipe/gap/doc/frontend reference subsequent auto-comply prospectively → eliminate KH/KC confusion class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-informal KH/KC distinction; no constraint loosening; existing artifacts grandfathered (re-verify khi touch); rule applies prospectively từ this PR forward 2026-06-09). Atomic-unique-bar §5.1: ✅ atomic (single concept: KH/KC product boundary) + ✅ unique (CLAUDE.md prose-only, no rule canonical-hóa) + ✅ widely applicable (mọi reference cần phân biệt KH/KC) + ✅ body §1 ≤2 conjunction. Path-scoped per `context-budget-mandate.md` §3.2 (frontend + flow/recipe/campaign + architecture) — always-load band giữ 13 (OK <18); path-scoped band +1. Detector (§6.5) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 2, FP risk KC platform-side exception); reviewer-checklist + cross-ref + memory + worked self-test sufficient cho v1.0.0.
