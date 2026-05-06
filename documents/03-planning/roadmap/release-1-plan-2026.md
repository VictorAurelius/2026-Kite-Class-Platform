---
title: Release Lần 1 Plan 2026 — Phased Rollout (Beta → Paid → P3 → K-12)
status: active
created: 2026-05-06
updated: 2026-05-06
phases: [1-beta, 1.5-paid, 2, 3]
release: 1
---

# Release Lần 1 Plan 2026 — Phased Rollout (Option β — Soft Beta + Iterate)

**Trạng thái:** ACTIVE — chốt 2026-05-06 sau Wave 24 closure + production-ready simulation.

**Định nghĩa "Release Lần 1":** Phase 1 BETA + Phase 1.5 PAID gộp lại = **Release Lần 1** = first deployable, production-grade SaaS launch tới public paid market. Phase 2 (P3 add) + Phase 3 (K-12) là các releases sau (Release 2, Release 3 — name TBD).

**Mục tiêu Release Lần 1:** Ship Kite SaaS tới production paid launch via **soft beta first** (invite-only, free), sau đó iterate close BLOCKING gaps → public paid launch. Tối ưu learning-loop + giảm rủi ro pháp lý + có time refine từ beta feedback.

**Decision update 2026-05-06 (Option β confirmed):** Production-ready simulation revealed 5 BLOCKING gaps + 4 STRONGLY recommend gaps không cover trong original Phase 1 scope. Thay vì extend Phase 1 (Option α: 12-16 weeks), chia làm **Phase 1 BETA** (9-12 weeks invite-only) + **Phase 1.5 PAID** (+4-6 weeks close BLOCKING) → Release Lần 1 production paid launch. Tổng Release 1 timeline tăng ~3-4 tuần so với Option α nhưng giảm rủi ro launch failure đáng kể.

---

## 1. Decision Context (chốt 2026-05-06)

### 1.1 Solo-dev mode + cadence đo thực tế

- Solo dev (1 người), code 12-18h/ngày
- 7 ngày qua đo được: 163 PRs merged / 5 active days / 10 waves shipped / 17 gaps DONE + 27 gaps PARTIAL
- Throughput thực: **~6 tuần single-dev work / active-day** (5× parallel-agent leverage typical wave-pack)
- Backlog hiện tại: **224 active gaps** (176 OPEN + 48 PARTIAL)

### 1.2 Legal counsel — chưa engage

- KHÔNG có legal counsel engaged tính tới 2026-05-06
- ETA engagement chưa xác định
- Search luật sư VN có chuyên môn PDPL/Luật Trẻ em/Luật Giáo dục mất ~2-4 tuần
- Document review timeline ~4-6 tuần sau engagement
- **Total counsel timeline: ~6-10 tuần** kể từ khi quyết định engage

### 1.3 Risk tolerance — Moderate

User đã chốt 2026-05-06:
- ✅ **Accept "v1 pending counsel review" disclaimer** trên Privacy/TOS/Cookies cho **non-K-12 production tenants**
- ❌ **KHÔNG accept** ship K-12 LEGAL surfaces (child protection / parent portal full LEGAL / period attendance MOET-grade) cho paid tenants pre-counsel
- Wave 23 đã ship pattern này (PR #821): 6 production legal pages với "v1 — đang chờ legal counsel review" disclaimer

### 1.4 Hard deadlines

| Constraint | Hard date | Status |
|---|---|---|
| **PDPL 2023 effective date** | 2026-07-01 | ~8 tuần countdown; Phase 1 ✅ shipped Wave 23; Phase 2 (GAP-353b/c/d) trong Phase 1 BETA |
| **K-12 school year start** (nếu launch K-12) | 2026-08 niên khóa mới | Phase 3 dependent |
| **Marketing/funding deadline** | None reported | Không pressure cụ thể |

### 1.5 Track 2 (production rebuild theo UI kit specs) — **Release-1-CRITICAL**

User confirmed 2026-05-06 (Option α): **Full Track 2 Phase 1 = 8 ports** ship trong Release Lần 1 Phase 1 BETA.

Lý do: Production FE hiện tại pre-kit (older inconsistent design); UI kits Round 2/3 = canon design spec. Marketing screenshots dùng kit polished; không port = brand mismatch first impression.

---

## 2. Phase Structure (Option β — Soft Beta + Iterate)

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1 BETA (9-12 calendar weeks)                           │
│ Target: P1 + P2 — INVITE-ONLY, 10-20 trusted tenants         │
│ Free during beta period                                       │
│ Disclaimer: "Beta — not for production-critical use"         │
│ Scope: ~50-60 gaps + 8 Track 2 ports (original Phase 1)      │
│ NO payment processing, NO public signup                       │
│ Goal: real-world feedback loop + bug discovery               │
├─────────────────────────────────────────────────────────────┤
│ Phase 1.5 PAID (+4-6 calendar weeks)                         │
│ Target: PUBLIC PAID LAUNCH P1 + P2                           │
│ Close 5 BLOCKING + 4 STRONGLY recommend gaps                  │
│ Scope: ~12 gaps (payment + refund/AUP/DSAR/RTBF + pen test + │
│        deploy runbook + monitoring dashboards + SLO)          │
│ Trigger: Beta feedback stabilized + tenants happy            │
├─────────────────────────────────────────────────────────────┤
│ Phase 2 (+4-6 calendar weeks)                                │
│ Target: + P3 (medium center 50-200 HS)                       │
│ Scope: ~25 gaps + 3 Track 2 ports                            │
│ Counsel engagement starts in this phase (4-6 tuần ETA)       │
├─────────────────────────────────────────────────────────────┤
│ Phase 3 (post-counsel, ~8-12 calendar weeks)                 │
│ Target: + P5 (K-12 schools) with FULL counsel + MPS A05      │
│ Scope: ~30 gaps + 4 Track 2 ports                            │
│ K-12 LEGAL trio production-grade (DPO + DPIA + 7y retention) │
└─────────────────────────────────────────────────────────────┘
```

**Total timeline:** ~25-34 calendar weeks ≈ **6-8 tháng** từ 2026-05-06 → ~2026-11/12 full K-12 GA.

| Milestone | Calendar week | Release |
|---|---|---|
| **Phase 1 BETA launch** (invite-only, free) | Week 9-12 | Release Lần 1 (beta) |
| **Phase 1.5 PAID launch** (public paid) | Week 13-18 | **🎯 Release Lần 1 (PRODUCTION)** |
| **Phase 2 P3 add** | Week 17-24 | Release 2 |
| **Phase 3 K-12 GA** | Week 25-34 | Release 3 |

---

## 3. Phase 1 BETA — P1 + P2 Invite-Only (9-12 tuần)

### 3.0 Beta launch model

- **Invite-only:** 10-20 trusted tenants (dạy thêm + trung tâm nhỏ; user-curated list)
- **Free during beta:** không charge payment trong beta period
- **Beta disclaimer trên signup + dashboard banner:**
  > "Kite is currently in BETA. Features may change, occasional issues expected. Free during beta period (~4-6 weeks). Your feedback shapes the production launch."
- **No public signup** — disable signup form on public marketing pages; replace với "Request Beta Access" form (collect email + contact, manual approval)
- **No payment processor required** trong beta (defer to Phase 1.5)
- **Goal:** real-world feedback loop, bug discovery, UX validation, performance baseline với real data

### 3.1 Target users

- **P1 — Solo teacher** (giáo viên dạy thêm 1-1, dạy nhóm nhỏ <10 HS)
- **P2 — Small center owner** (chủ trung tâm nhỏ, ≤50 HS, ≤5 GV)

### 3.3 Gap clusters (target ~50-60 gaps)

#### Cluster 3.3.1 — PDPL Phase 2 (close legal compliance loop) [3 gaps]
- **GAP-353b** server consent API + audit-log link (~12-16h, P1)
- **GAP-353c** DSAR self-service intake form (~6-8h, P2)
- **GAP-353d** DPIA Decree 13/2023 Art 24-30 docs (~4-6h, P2)

**Wave-pack candidate:** GAP-353b + GAP-353c sequential bucket — same `kitehub-subscription` module sub-package isolation.

**Hard deadline:** PDPL effective 2026-07-01 → ship trước hoặc shortly after acceptable since LocalStorage Phase 1 shipped Wave 23 covers Art 11+13.

#### Cluster 3.3.2 — Critical infra / Observability [8-10 gaps]

Backend infrastructure cho production-grade ops:
- **GAP-114** structured JSON logging
- **GAP-115** log aggregation pipeline (Loki/ELK)
- **GAP-116** PII scrubbing logs
- **GAP-112** distributed tracing (OpenTelemetry)
- **GAP-113** FE error tracking (Sentry-equivalent)
- **GAP-144** alertmanager production receivers (PARTIAL)
- **GAP-145** Loki tracing stack
- **GAP-117** restore drill test (PARTIAL — quarterly cadence)
- **GAP-204** npm security backlog (PARTIAL — pre-Release-1 fixes)
- **GAP-218** PDF font missing runbook

**Why Release-1:** production launch không có tracing + logs + alerts = blind ops. Nếu incident xảy ra, không debug được + không recover được.

#### Cluster 3.3.3 — Trial → Paid flow + billing [4-5 gaps]

- **GAP-192** trial-to-paid zero-downtime migration (PARTIAL)
- **GAP-260** gateway tier multiplier enforcement
- **GAP-259** gateway rate limit per tenant key (PARTIAL)
- **GAP-185** billing terms VAT TCT compliance (PARTIAL)
- **GAP-108** payment-invoice config externalization

**Why Release-1:** không có trial→paid migration = không có revenue path. Rate limit + tier enforcement = không có monetization control.

#### Cluster 3.3.4 — Bulk import + onboarding [3-4 gaps]

- **GAP-137** bulk import frontend UI missing
- **GAP-325** parent-student bulk import link (P2 setup)
- **GAP-287** branding wizard skip default (P1+P2 fast onboarding)
- **GAP-288** onboarding tour solo teacher (P1 first-time setup)

**Why Release-1:** P2 small center onboarding cần bulk import HS/GV cho first-time setup. P1 cần onboarding tour.

#### Cluster 3.3.5 — Track 2 ports (8 ports — Full α) [8 gaps]

Per user decision Option α 2026-05-06:

| # | Gap | Port | Effort raw | Critical? |
|---|---|---|---:|---|
| 1 | **GAP-273** | 12 components shared lib (`@kite/shared-ui`) | ~1.5 tuần | BLOCKING — all other ports depend |
| 2 | **GAP-274** | KC public marketing port | ~1-2 tuần | First impression |
| 3 | **GAP-275** | KH public marketing + blog port | ~1-2 tuần | Same |
| 4 | **GAP-276** | Auth flows (login/signup/forgot/social) | ~1 tuần | Universal |
| 5 | **GAP-277** | Error pages (404/500/timeout) | ~0.5 tuần | Polish |
| 6 | **GAP-280** | Onboarding wizard | ~1.5 tuần | First-time setup |
| 7 | **GAP-266** | kiteclass-pro-v2 owner dashboard | ~2 tuần | P2 daily-use |
| 8 | **GAP-268** | kiteclass-teacher dashboard | ~1.5 tuần | P1 daily-use |
| 9 | **GAP-270** | kitehub-pro-v2 (KH owner subscription/billing) | ~1.5 tuần | KH owner |

Total Track 2 raw: ~12-15 tuần FE work. Parallel-agent leverage ~2.5× (FE less parallelizable than BE):
- Calendar wall-clock: ~5-6 tuần với 4-5 parallel ports per wave-pack

**Strategy:** ship GAP-273 (shared lib) FIRST — week 1-2; then 4-5 ports parallel/wave; final 3-4 ports parallel/wave.

#### Cluster 3.3.6 — UI Kits polish (close Wave 22 follow-ups) [4 gaps]

- **GAP-363b** kiteclass-student external re-audit + delta-to-≥105 (~10-15h)
- **GAP-364b** kitehub-admin cross-screen polish (~23h)
- **GAP-366** frontend-standards.md kit-as-source-of-truth
- **GAP-367** quality/kit-production-parity skill

**Why Release-1:** Track 2 ports (cluster 5) cần parity-checked vs kit. GAP-366/367 là enforcement framework. GAP-363b/364b polish kits trước khi port.

#### Cluster 3.3.7 — Tech-debt critical [3-4 gaps]

- **GAP-357** deprecated ctor migration Phase 1 (PARTIAL — 17 modules remaining)
- **GAP-204** npm security (PARTIAL — pre-Release-1 CVE sweep)
- **GAP-261** Werror flipday (compile warnings → errors)
- **GAP-245** CI enforce IDE warnings (PARTIAL)

**Why Release-1:** code quality baseline trước production scale-out. Tech-debt accrues fast post-Release-1.

#### Cluster 3.3.8 — Persona reviews + business correctness [3 gaps]

- **GAP-049** business logic correctness review (PARTIAL — Phase 2 audit 45 rules.md files)
- **GAP-050** persona-based business review (PARTIAL)
- **GAP-156** business rules compliance audit (PARTIAL)

**Why Release-1:** existing 5-attribute review framework shipped (Wave Business Correctness 2026-04-29); Phase 2 = run audit on existing 45 rules.md files. Quarterly cadence post-Release-1.

#### Cluster 3.3.9 — KiteHub control + admin [4-5 gaps]

- **GAP-067** KiteHub instance control plane
- **GAP-068** KiteHub admin branding console
- **GAP-040** support impersonation tools
- **GAP-066** KiteHub unified reports dashboard

**Why Release-1:** KH admin needs visibility/control khi paid tenants signup. Without these, support impossible.

#### Cluster 3.3.10 — AI Branding minimum (subset of GAP-225 cluster) [4-5 gaps]

Wave 23 already shipped ConsentBanner + GAP-368 legal pages. AI Branding minimum cho Release Lần 1:
- **GAP-014** wave mock include AI branding (PARTIAL)
- **GAP-220** branding version snapshot JSONB (PARTIAL)
- **GAP-215** branding service cacheable
- **GAP-272** AI Branding wizard v2 port — WAIT: this is Phase 2 if AI Branding Phase 1 minimal

Decision: AI Branding Phase 1 BETA = **basic branding (logo upload + color theme picker)** without full AI generation. Defer GAP-003 (multi-tier image), GAP-004 (template-based image), GAP-006 (Gemma upgrade) to Phase 2.

### 3.3 Phase 1 critical path

```
Week 1-2:
  ├─ GAP-273 components shared lib (BLOCKING for all FE ports)
  ├─ GAP-353b PDPL server consent API (PARTIAL deadline 2026-07-01)
  ├─ Cluster 3.3.2 Critical infra (parallel BE wave-pack)
  └─ Cluster 3.3.7 Tech-debt critical

Week 3-5:
  ├─ Track 2 ports wave-pack 1: GAP-274 + GAP-275 + GAP-276 + GAP-277 (4 parallel FE)
  ├─ Cluster 3.3.3 Trial→Paid + billing (BE wave-pack)
  └─ Cluster 3.3.6 UI Kits polish (GAP-363b/364b/366/367)

Week 6-8:
  ├─ Track 2 ports wave-pack 2: GAP-266 + GAP-268 + GAP-270 + GAP-280 (4 parallel FE)
  ├─ Cluster 3.3.4 Bulk import + onboarding
  └─ Cluster 3.3.9 KiteHub control + admin

Week 9-10:
  ├─ Cluster 3.3.10 AI Branding minimum
  ├─ Cluster 3.3.8 Persona reviews + business correctness Phase 2
  └─ Wave 35-37 FE polish + integration testing

Week 11-12:
  ├─ End-to-end QA + Playwright E2E
  ├─ Production stabilization (deploy infra, DNS, monitoring)
  └─ Phase 1 SOFT LAUNCH 🚀
```

### 3.4 Phase 1 deliverables (success criteria)

- [ ] PDPL Phase 1 + Phase 2 ✅ shipped (banner + legal pages + server API + DSAR + DPIA docs)
- [ ] 8 Track 2 ports shipped — production FE matches kit canon
- [ ] Production observability: logs aggregated + traces + alerts + restore drill validated
- [ ] Trial → Paid flow tested end-to-end với real payment processor (VNPay/MoMo sandbox)
- [ ] Bulk import HS/GV/Parent functional cho P2 small center
- [ ] KiteHub admin tooling: support impersonation + reports dashboard
- [ ] AI Branding minimum: logo upload + color theme picker working
- [ ] First 5-10 beta tenants signed up + onboarded
- [ ] Quality audit /100 ≥ 80 score
- [ ] No P0 incidents trong 2 tuần stabilization

---

### 3.6 Phase 1 BETA deliverables (success criteria → trigger Phase 1.5)

Trigger để move Phase 1.5 (close BLOCKING gaps → public paid launch):
- [ ] 8 Track 2 ports shipped — production FE matches kit canon
- [ ] Production observability: logs aggregated + traces + alerts + restore drill
- [ ] 10-20 invite-only beta tenants signed up + onboarded
- [ ] **Beta period 4-6 weeks completed** với:
  - 0 P0 incidents 2 weeks running
  - User feedback collected (qualitative interviews + bug reports)
  - Performance baseline measured (P95 latency, error rates)
  - Quality audit /100 ≥ 80
- [ ] No K-12 tenants accepted (P5 defer Phase 3)
- [ ] PDPL Phase 1 + Phase 2 ✅ shipped (banner + legal pages + server API)
- [ ] AI Branding minimum: logo upload + color theme picker functional

---

## 4. Phase 1.5 PAID — Close BLOCKING gaps → Public Paid Launch (+4-6 tuần)

### 4.1 Trigger conditions

Phase 1.5 START khi đủ:
1. ✅ Phase 1 BETA deliverables met (§3.6)
2. ✅ Beta tenants stable (no critical bugs unresolved)
3. ✅ Decision to scale to public paid market (user confirms)

### 4.2 BLOCKING gaps cluster (~5-7 gaps)

Phải close cho public paid launch — legal/financial/security exposure if missing:

| Gap | Scope | Effort | Why BLOCKING |
|---|---|---|---|
| **GAP-NEW-payment-processor-init** | VNPay/MoMo init flow integration (sandbox + production keys + checkout webhook) | ~1 tuần | Không có thì không thu tiền được |
| **GAP-183 close** | Refund / dispute resolution policy (PARTIAL → DONE) | ~3 ngày | Paid tenants có quyền refund; không có policy = legal exposure |
| **GAP-181 close** | Acceptable Use Policy (PARTIAL → DONE) | ~2 ngày | Must-have với ToS cho paid market |
| **GAP-353c close** | DSAR self-service intake form (P2 defer → Phase 1.5 promote) | ~6-8h | PDPL Art 14 mandate; manual email-based ban đầu OK nhưng risky public scale |
| **GAP-073 close** | Account deletion / RTBF endpoint + UI (post-Release-1 defer → Phase 1.5 promote) | ~1 tuần | PDPL Art 14 erasure right; user yêu cầu xóa account = vi phạm nếu không có |
| **GAP-185 close** | VAT eInvoice TT 78/2021 NĐ 123/2020 production-ready | ~3-5 ngày | Tax compliance cho paid invoices |

### 4.3 STRONGLY recommend cluster (~4-5 gaps)

Should close cho production-grade launch — non-blocking nhưng risk visible:

| Gap | Scope | Effort | Why |
|---|---|---|---|
| **GAP-NEW-pen-test-light** | OWASP top 10 audit + security headers (CSP/HSTS/X-Frame-Options/CSRF config) + secrets management review | ~1 tuần | Standard SaaS security baseline |
| **GAP-NEW-deploy-runbook** | Production deploy runbook + go-live checklist + Oracle Cloud activation | ~3-5 ngày | Need clear procedure cho production deploy |
| **GAP-NEW-monitoring-dashboards** | Grafana setup + alerts wired tới on-call channel + dashboard creation | ~3-5 ngày | Production launch không có dashboard = blind ops |
| **GAP-135 close** | API P95 latency SLO definitions (PARTIAL → DONE) | ~2 ngày | Need explicit SLOs cho monitoring + ops escalation |

### 4.4 Phase 1.5 deliverables (trigger Phase 2)

- [ ] All 5-7 BLOCKING gaps closed
- [ ] All 4-5 STRONGLY recommend gaps closed
- [ ] Public signup form re-enabled trên marketing pages
- [ ] Payment processor live (VNPay/MoMo)
- [ ] First 5-10 paid tenants signed up + onboarded successfully
- [ ] Refund flow tested end-to-end
- [ ] Account deletion flow tested
- [ ] DSAR intake form functional + ticketing
- [ ] Pen test report committed
- [ ] Production monitoring dashboards live
- [ ] SLO targets documented + alerts wired
- [ ] Quality audit /100 ≥ 85 (higher bar than Phase 1 BETA)
- [ ] No P0 incidents 4 weeks running

---

## 5. Phase 2 — P3 Medium-Center Add-On (+4-6 tuần)

### 4.1 Target users

- **P3 — Medium center director** (50-200 HS, 5-15 GV, multi-class scheduling, commission engine, P&L reports)

### 4.2 Gap clusters (~25 gaps + 3 Track 2 ports)

#### Cluster 4.2.1 — P3 commission + scheduling + RBAC [6 gaps]
- **GAP-306** P3 teacher commission engine (BHXH/BHYT/TNCN)
- **GAP-307** P3 multi-class schedule conflict 3-axis
- **GAP-308** P3 RBAC audit log unauthorized 403
- **GAP-310** P3 substitute teacher leave workflow
- **GAP-311** P3 room/resource management
- **GAP-329** substitute teacher RBAC time-bound

#### Cluster 4.2.2 — P3 financial + reports [6 gaps]
- **GAP-309** P3 multi-scale gradebook unified view
- **GAP-312** P3 director daily ops dashboard
- **GAP-313** P3 bank MT940 + VNPay/MoMo reconcile
- **GAP-314** P3 monthly P&L financial reports
- **GAP-315** P3 VAT eInvoice ND123 TCT integration
- **GAP-316** P3 complaint workflow SLA escalation

#### Cluster 4.2.3 — P3 features (offboard, stress, certs) [4 gaps]
- **GAP-317** P3 staff offboard wizard
- **GAP-318** P3 stress test peak enrollment
- **GAP-319** P3 WORM audit log 10-year retention
- **GAP-320** P3 completion certificate transcript QR

#### Cluster 4.2.4 — Lesson management + payment edge cases [6 gaps]
- **GAP-291** reschedule lesson session
- **GAP-292** per-session pricing model
- **GAP-293** monthly income summary dashboard
- **GAP-294** attendance no-show status
- **GAP-295** late-cancel policy workflow
- **GAP-296** substitute teacher attribution

#### Cluster 4.2.5 — AI Branding Phase 2 [4 gaps]
- **GAP-272** AI Branding wizard v2 port (Track 2)
- **GAP-003** AI multi-tier image generation
- **GAP-004** template-based image composition
- **GAP-006** upgrade to Gemma 4 (model swap)

#### Cluster 4.2.6 — Track 2 Phase 2 ports [3 ports]
- **GAP-271** kitehub-admin port (P3 admin — cần GAP-364b polish first from Phase 1)
- **GAP-272** AI Branding wizard v2 (already in 4.2.5)
- **GAP-279** modals/dialogs catalog port

### 4.3 Phase 2 special concern — Counsel engagement

Phase 2 **start counsel engagement search** (parallel với code work):
- Tuần 1-2: search + contact luật sư VN với PDPL/Education chuyên môn
- Tuần 3-4: engage + send TOS/Privacy/PDPL Phase 1 docs cho review
- Tuần 5-6: receive review feedback + plan Phase 3 K-12 LEGAL surfaces

→ Khi Phase 2 ship done, counsel review process đã bắt đầu, ready cho Phase 3.

---

## 6. Phase 3 — K-12 Schools (post-counsel, ~8-12 tuần)

### 5.1 Trigger conditions

K-12 launch CHỈ start khi đủ 4 điều kiện:
1. ✅ Legal counsel engaged + signed-off TOS/Privacy/Cookie/DPA documents (production-grade, không "v1 pending")
2. ✅ DPO designated chính thức (acting solo-dev → external DPO contracted)
3. ✅ MPS A05 registration submitted per Decree 13/2023 Art 28 (if processing >100k subjects threshold)
4. ✅ DPIA documentation completed per Decree 13/2023 Art 24-30 (GAP-353d Phase 2)

ETA: 6-10 tuần sau Phase 2 starts counsel engagement.

### 5.2 Gap clusters (~30 gaps + 4 Track 2 ports)

#### Cluster 5.2.1 — K-12 LEGAL trio production-grade [12-15 gaps]
- **GAP-321** parent portal v1 (PARTIAL Phase 1A) → Phase 1B + 1C COMPLETE production
- **GAP-322** child protection workflow (PARTIAL Phase 1A) → Phase 1B + 1C COMPLETE
- **GAP-323** period attendance + multi-subject gradebook (PARTIAL Phase 1A)
- **GAP-359** child protection Phase 1C remainder (PARTIAL — Wave 24 Bucket A in flight 359.1+359.5)
- **GAP-360** multi-subject gradebook Phase 1C remainder (PARTIAL — Wave 24 Bucket B)
- **GAP-361** parent portal Phase 1C remainder (PARTIAL — Wave 24 Bucket C)
- **GAP-321b-1-fees-instalment-payment-history** P2 follow-up
- **GAP-321b-1-notifications-engine-wiring** P1 (depends GAP-063b)
- **GAP-345** Wave 17 K-12 LEGAL trio state-check audit (PARTIAL)
- **GAP-186** child protection policy (PARTIAL)

#### Cluster 5.2.2 — K-12 features (MOET, exam, hocba) [10-12 gaps]
- **GAP-055** official report card VN (MOET format)
- **GAP-056** homeroom teacher GVCN
- **GAP-057** payroll teacher commission (PARTIAL)
- **GAP-057b** payroll Phase 2 types/tax/BHXH/PDF/bank
- **GAP-059** student conduct tracking
- **GAP-060** period-based attendance
- **GAP-061** promotion-retention logic
- **GAP-063** SMS/Zalo notification (PARTIAL)
- **GAP-063b** notification Phase 2 quiet hours fallback
- **GAP-064** SCORM xAPI compliance

#### Cluster 5.2.3 — K-12 admin / MOET integrations [8-10 gaps]
- **GAP-324** K-12 role hierarchy bulk onboarding
- **GAP-326** MOET school license verification
- **GAP-327** MOET subject taxonomy seed
- **GAP-328** exam workflow approval chain
- **GAP-330** classroom resource scheduling
- **GAP-331** exam invigilation roster
- **GAP-332** homework module
- **GAP-333** sổ đầu bài digital
- **GAP-334** multi-fee structure
- **GAP-335** public-private fee compliance
- **GAP-336** MOET financial report TT107/TT200
- **GAP-337** emergency broadcast multi-channel
- **GAP-338** parent-teacher meeting coordination
- **GAP-339** complaint escalation 4-level (BLOCKER for 359.3 + 361.A)
- **GAP-340** MOET inter-school transfer API
- **GAP-341** phổ cập escalation mandatory
- **GAP-342** exam retake workflow
- **GAP-343** học bạ + bằng tốt nghiệp sealed PDF QR
- **GAP-344** school closure 30y archive

#### Cluster 5.2.4 — Track 2 Phase 3 ports [4 ports]
- **GAP-269** kiteclass-student port (K-12 mobile PWA — BLOCKED on GAP-363+365)
- **GAP-267** kiteclass-parent port (K-12 parent portal)
- **GAP-278** kitehub platform admin port (K-12 multi-tenant management)

#### Cluster 5.2.5 — Trial/marketing for K-12 segment [3-5 gaps]
- **GAP-274** + **GAP-275** marketing port (already Phase 1) — extend with K-12 messaging
- **GAP-200** school MIS integration (PARTIAL)
- **GAP-281** secondary persona AC phase 2 P1 cells
- **GAP-282** secondary persona AC phase 3 P2 cells

---

## 7. Post-Release-1 defer list (~80 gaps explicit defer)

Sau khi 3 phases ship, các gaps này tracked trong backlog nhưng KHÔNG block GA:

### 6.1 AI Branding nice-to-have [10-15 gaps]
- GAP-017 AI usage billing integration
- GAP-019 AI observability cost monitoring
- GAP-022 template analytics optimization
- GAP-023 admin moderation tools
- GAP-024 asset lifecycle storage cleanup
- GAP-025 mobile-first wizard UX
- GAP-027 multi-brand per tenant
- GAP-028 model versioning migration strategy
- GAP-029 quality gate calibration
- GAP-030 disaster recovery AI Branding
- GAP-034 branding export pack
- GAP-035 wizard team collaboration
- GAP-045 template marketplace
- GAP-074 AI alt-text accessibility
- GAP-072 scheduled rebrand academic year refresh

### 6.2 Marketing growth + Dev API [5-7 gaps]
- GAP-026 trial freemium AI mechanics
- GAP-036 tier upgrade reveal UX
- GAP-038 developer API docs/SDK
- GAP-039 webhook reliability versioning
- GAP-044 synthetic monitoring + feature flags

### 6.3 Tech-debt cleanup full [5-8 gaps]
- GAP-357 Phase 1 full migration (~17 modules remaining sau Phase 1a)
- GAP-246 delete unused UI calendar
- GAP-247 hcaptcha lazy load forwardRef
- GAP-248 KC auth layout provider hoist
- GAP-256 rule read README before grep
- GAP-261 Werror flipday (defer if Phase 1 minimum acceptable)
- GAP-362 tenant isolation IT flake (test-only)

### 6.4 Backend infrastructure nice-to-have [8-10 gaps]
- GAP-123 HPA kitehub services
- GAP-124 PDB networkpolicy hardening
- GAP-125 canary deployment infra
- GAP-127 frontend code splitting bundle analyzer
- GAP-138-142 misc UI bugs (parent dashboard placeholder, form locale, native select etc.)
- GAP-110 Ollama model inter-service inconsistent
- GAP-191 domain registration DNS strategy (PARTIAL)
- GAP-217 document endpoints alert rules
- GAP-220 branding version snapshot JSONB (PARTIAL)
- GAP-221 GitNexus pilot evaluation
- GAP-222 outbox bypass policy (PARTIAL)
- GAP-223 AI branding migration verification governance (PARTIAL)
- GAP-226-228 real WCAG/visual regression/ML classifier (depends infra)
- GAP-231-233 API contract zero-doc (3 services)
- GAP-239 API SLO coverage completion
- GAP-257 restore drill phase 3 quarterly

### 6.5 Track 2 coverage extras [4-5 gaps]
- GAP-273 components shared lib FULL (Phase 1 ships only critical 5 of 12)
- GAP-279 modals/dialogs catalog full (Phase 1 ships subset)

### 6.6 Misc [5-8 gaps]
- GAP-080 KiteHub dashboard loading UX
- GAP-099 structured class schedule (PARTIAL)
- GAP-102 guides completion ADR kickoff (PARTIAL)
- GAP-176 UI/UX pro-max skill integration
- GAP-181 acceptable use policy (PARTIAL — covered via GAP-180 TOS Phase 1)
- GAP-183 refund-dispute resolution policy (PARTIAL)
- GAP-198 FE-BE mock contract tests (PARTIAL)
- GAP-200 school MIS integration (PARTIAL)
- GAP-212 URL allowlist test flaky DNS
- GAP-213 Spring Cloud BOM resolution
- GAP-216 PDF p95 micro-benchmark
- GAP-219 Wave 5 audit followups P1/P2
- GAP-262 starter kit upstream retro sync (PARTIAL)
- GAP-358 dev workstation Oracle Cloud migration

---

## 8. Forever-defer / out-of-scope (~25 gaps)

Gaps explicit KHÔNG ship trong any phase v1.0; revisit post-GA hoặc skip:

### 7.1 Over-scoped features
- **GAP-176** UI/UX pro-max skill integration — meta tooling, không cần MVP
- **GAP-261** Werror flipday — cosmetic, defer indefinite
- **GAP-256** rule read README before grep — meta-doc, low value
- **GAP-221** GitNexus pilot — research, không production-critical
- **GAP-358** dev workstation Oracle Cloud migration — dev infra, không user-facing

### 7.2 Not P1+P2+P3 use case
- **GAP-200** school MIS integration — only K-12 schools, not center model
- **GAP-064** SCORM xAPI compliance — enterprise LMS feature, không center MVP
- **GAP-045** template marketplace — community feature, post-GA

### 7.3 Premature optimization
- **GAP-318** P3 stress test peak enrollment — only when scale demands
- **GAP-127** frontend code splitting bundle analyzer — when bundle size >2MB
- **GAP-216** PDF p95 micro-benchmark — only when PDF latency P95 reported issue

---

## 9. Stop-doing list rationale

Why explicit defer (không phải "later" — actually KHÔNG ship pre-GA):

| Pattern | Examples | Lý do |
|---|---|---|
| **Premature optimization** | Bundle analyzer, stress test, PDF benchmark | Chưa có scale issue thực |
| **Enterprise/community features** | Template marketplace, SCORM, MIS integration | P1+P2+P3 SMB market không cần |
| **Meta-tooling overhead** | Pro-max skill, Werror flipday, GitNexus pilot | Không user-facing impact |
| **Tech-debt cleanup full sweep** | All 17 deprecated ctor modules, all unused UI components | Phase 1a partial enough; full sweep post-GA |
| **AI Branding advanced** | Multi-brand per tenant, marketplace, scheduled rebrand | Phase 2 minimum AI Branding (logo + color) đủ MVP |

---

## 10. Wave roadmap Phase 1 + 1.5 (next 6-8 waves outline)

### Wave 25 (current + 1) — PDPL Phase 2 + Critical infra start
- **Bucket A:** GAP-353b server consent API + audit-log link
- **Bucket B:** GAP-114 + GAP-115 + GAP-116 structured logging + aggregation + PII scrubbing
- **Bucket C:** GAP-117 restore drill validation + GAP-204 npm CVE sweep
- Wall-clock: ~45-60 min agent
- Hard target: PDPL Phase 2 done

### Wave 26 — Track 2 shared lib + first FE wave-pack
- **Bucket A:** GAP-273 12 components shared lib BLOCKING (~1.5 tuần raw)
- **Bucket B:** GAP-274 KC public marketing port (~1-2 tuần)
- **Bucket C:** GAP-275 KH public marketing port (~1-2 tuần)
- **Bucket D:** GAP-276 + GAP-277 auth flows + error pages
- Wall-clock: ~60-90 min (FE-heavier; longest bucket A 1.5 tuần raw)

### Wave 27 — Track 2 dashboards + onboarding + UI Kits polish
- **Bucket A:** GAP-266 kiteclass-pro-v2 owner dashboard
- **Bucket B:** GAP-268 kiteclass-teacher dashboard
- **Bucket C:** GAP-270 kitehub-pro-v2
- **Bucket D:** GAP-280 onboarding wizard + GAP-363b/364b polish
- Wall-clock: ~90 min

### Wave 28 — Trial→Paid + Bulk import + KH admin
- **Bucket A:** GAP-192 trial-to-paid migration + GAP-260 tier multiplier + GAP-259 rate limit
- **Bucket B:** GAP-137 bulk import FE + GAP-325 parent-student bulk
- **Bucket C:** GAP-067 KiteHub instance control + GAP-068 admin branding console + GAP-040 impersonation
- **Bucket D:** GAP-287 + GAP-288 onboarding tour + branding wizard skip
- Wall-clock: ~60 min

### Wave 29 — AI Branding minimum + Persona reviews + Tech-debt
- **Bucket A:** AI Branding minimum (logo + color theme picker) — subset of GAP-014/220/215
- **Bucket B:** GAP-049/050/156 persona + business correctness audits Phase 2
- **Bucket C:** GAP-357 Phase 1b (~5-7 modules deprecated ctor migration)
- Wall-clock: ~45 min

### Wave 30+ — Phase 1 BETA stabilization + invite + measure
- Playwright E2E suite
- Production deploy infra (DNS, CI/CD, monitoring) — minimum viable
- 10-20 invite-only beta tenants onboarding
- Beta feedback collection (interviews, bug tracking)
- Quality audit /100 baseline ≥80
- Phase 1 BETA SOFT LAUNCH 🚀
- 4-6 tuần beta period running

**Wave 25-30 estimated: 9-12 calendar weeks Phase 1 BETA total.**

### Wave 31-34 — Phase 1.5 BLOCKING + STRONGLY recommend gaps
- Wave 31: payment processor init (VNPay/MoMo) + GAP-185 close eInvoice
- Wave 32: GAP-183 + GAP-181 close (refund + AUP) + GAP-353c DSAR + GAP-073 RTBF
- Wave 33: pen-test light + security headers + GAP-135 SLO
- Wave 34: deploy runbook + monitoring dashboards activation + paid launch prep

### Wave 35 — Phase 1.5 PUBLIC PAID LAUNCH
- Public signup re-enabled
- Payment processor live
- Quality audit /100 ≥85 + 0 P0 incidents 4 weeks
- Phase 1.5 PAID LAUNCH 🚀

**Wave 31-35 estimated: 4-6 calendar weeks Phase 1.5 total.**

**Combined Phase 1 + Phase 1.5: 13-18 calendar weeks → public paid launch.**

---

## 11. Tracking + reviews

### 11.1 Per-phase milestones

| Phase | Milestone gates | Review cadence |
|---|---|---|
| **Phase 1 BETA** | 8 Track 2 ports + observability + 10-20 invite tenants live + 0 P0 incidents 2 tuần + Quality audit /100 ≥80 | Weekly + post-wave audit |
| **Phase 1.5 PAID** | 5 BLOCKING + 4 STRONGLY-recommend gaps closed + payment live + 5-10 paid tenants + Quality audit /100 ≥85 + 0 P0 incidents 4 tuần | Weekly |
| **Phase 2** | Counsel engagement started + P3 commission engine prod | Weekly + counsel-engagement check |
| **Phase 3** | Counsel sign-off + MPS A05 + DPIA + K-12 LEGAL trio prod-grade | Weekly + legal review per surface |

### 10.2 Decision points (re-evaluate plan)

- **Wave 30 Phase 1 launch:** review actual cadence vs estimate; adjust Phase 2 scope nếu cadence < expected
- **Phase 2 mid:** counsel ETA confirmation → finalize Phase 3 timeline
- **Phase 3 trigger:** all 4 conditions §5.1 met → spawn first K-12 wave

### 10.3 Scope creep handling

- Mọi simulation-gap-finder run trong Phase 1: file gaps as P3 unless directly blocking MVP
- Audit findings: split into "block Phase 1 launch" vs "post-Release-1" — prefer post-Release-1 unless P0 LEGAL/security
- User-flagged misses: pipeline qua `incident-to-rule-pipeline.md` — apply scope discipline

---

## 12. Open items / clarifications

Items chưa quyết định, cần user xác nhận khi gặp:

- [ ] **Counsel budget** — 50-200M VND fixed-fee review hoặc retainer? Định guidance để decide khi Phase 2 mid.
- [ ] **Track 2 mobile PWA** — GAP-269 student mobile in Phase 3 K-12 OR earlier nếu P2 small center có student-facing?
- [ ] **AI Branding Phase 2 scope** — full multi-tier image generation (GAP-003/004/006) hay chỉ template-based?
- [ ] **Marketing/funding deadline** — nếu có deadline cụ thể, có thể compress Phase 1 bằng cách reduce Track 2 ports xuống 5 (Option β) thay vì 8.
- [ ] **K-12 launch market** — ngay khi Phase 3 ready, hay đợi MOET school year (tháng 8)?

---

## 13. Log

- **2026-05-06 (initial):** Plan created. User confirmed:
  - Solo dev mode + no counsel engaged
  - Risk tolerance Moderate ("v1 pending" OK non-K-12)
  - Track 2 Option α (full 8 ports Phase 1)
- 3-phase rollout: P1+P2 (9-12 tuần) → P3 (+4-6 tuần) → K-12 post-counsel (+8-12 tuần)
- Total Release Lần 1 timeline 5-7 tháng calendar
- ~50-60 gaps Phase 1 critical + 8 Track 2 ports + ~25 P3 Phase 2 + ~30 K-12 Phase 3 + ~80 post-Release-1 defer + ~25 forever-defer
- Wave 25-30 Phase 1 outline drafted

- **2026-05-06 (revised — Option β):** Production-ready simulation revealed 5 BLOCKING + 4 STRONGLY recommend gaps không cover Phase 1 (payment processor, refund/AUP, DSAR/RTBF, pen-test light, deploy runbook, monitoring dashboards, SLO). User chose **Option β — Soft Beta + Iterate** thay vì Option α extend Phase 1.
- Plan revised với **Phase 1.5 PAID** insertion giữa Phase 1 BETA và Phase 2:
  - Phase 1 BETA = invite-only 10-20 trusted tenants free; 9-12 tuần
  - Phase 1.5 PAID = close BLOCKING gaps → public paid launch; +4-6 tuần
  - Combined Phase 1 + 1.5 = 13-18 calendar weeks → public paid launch
  - Total Release Lần 1 timeline 6-8 tháng calendar (was 5-7)
- Wave 25-30 vẫn cho Phase 1 BETA; Wave 31-35 mới cho Phase 1.5 PAID
- Beta launch model documented (§3.0): invite-only signup form replaces public signup, free during beta, beta disclaimer trên dashboard, no payment processor required
- BLOCKING + STRONGLY recommend gaps documented in §4.2 + §4.3 với effort estimates
- Phase 1 + 1.5 success criteria split rõ (§3.6 + §4.4)
- User feedback: "release lần 1 mang tính chất là 1 MVP ổn định để deploy thôi nên option B là hợp lý nhỉ"

---

## Related

- Backlog source: `documents/04-quality/gaps/ROADMAP.md` + 224 active GAP-*.md files
- Wave history: `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl`
- Existing roadmaps:
  - `documents/03-planning/roadmap/wave-roadmap-p0.md` — original P0 wave roadmap
  - `documents/03-planning/roadmap/parallel-execution-strategy.md`
  - `documents/03-planning/roadmap/kitehub-saas-implementation-plan.md`
  - `documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md` — Track 2 detailed roadmap
- Rules:
  - `meta-gap-priority.md` — priority ordering Meta > Business-Logic > Feature
  - `gap-done-discipline.md` §3 PARTIAL exit ramp
  - `audit-to-gap-pipeline.md` — gap filing pipeline
  - `business-logic-review.md` — 5-attribute mandate
- Memory:
  - `feedback_parallel_agent_strategy.md` — wave-pack pattern
  - `feedback_post_merge_doc_sync.md` — closure PR pattern
