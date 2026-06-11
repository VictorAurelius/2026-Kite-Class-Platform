---
title: Wave beta-prep-1 outside-in — External benchmark VN edu SaaS
created: 2026-05-26
phase: phase-1-beta
type: outside-in-benchmark
wave: beta-prep-1
auditor: external-benchmark-agent (outside-in #2/3)
related_audits:
  - 2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md (prior VN edu SaaS benchmark Wave 86 prep)
sources_consulted:
  - https://emis.misa.vn/bao-gia/ (MISA EMIS pricing — 30,000 schools)
  - https://easyedu.vn/ (Easy Edu — 1,400+ centers, 5M+ users)
  - https://flyer.vn/phan-mem-quan-ly-trung-tam-tieng-anh/ (comparison 11 VN center mgmt tools 2025)
  - https://ileader.vn/ (iLeader — 800+ centers)
  - https://mona.software/edutech/ (Mona EduTech)
  - https://www.vietnam-briefing.com/news/vietnam-personal-data-protection-regulation-decree-356.html/ (PDPL Decree 356/2025)
  - https://www.dfdl.com/insights/legal-and-tax-updates/vietnam-personal-data-protection-2026-what-foreign-organizations-need-to-know/ (DFDL PDPL 2026 guide)
  - https://centercode.com/blog/2015/10/how-to-use-net-promoter-score-in-your-beta-program (Centercode beta NPS pattern)
  - https://customergauge.com/benchmarks/blog/nps-saas-net-promoter-score-benchmarks (SaaS NPS 2025)
---

# Wave beta-prep-1 outside-in — External benchmark VN edu SaaS

## Benchmark methodology

**Scope:** Challenge proposed Wave beta-prep-1 scope (6 buckets A-F: PDPL compliance-min / Security-beta-min / Ops-beta-min / GAP-727 class-teacher-fix / GAP-730 idempotency-finish / Beta invite + onboarding) bằng external benchmark VN edu SaaS launch pattern + global SaaS beta-to-GA convention.

**Sources consulted (10 surfaces):**
- 5 vendor sites direct: MISA EMIS / Easy Edu / iLeader / Mona EduTech / VnResource (via comparison aggregator)
- 1 aggregator comparison: Flyer.vn 11-tool review 2025
- 2 PDPL legal compliance sources: Vietnam-Briefing (Decree 356/2025) + DFDL legal alert
- 2 SaaS beta-pattern sources: Centercode (beta NPS) + CustomerGauge (SaaS NPS 2025 benchmarks)

**Tools:** WebSearch (8 queries) + WebFetch (3 deep dives on vendor pages).

**Limitations:**
- VN vendors KHÔNG transparent về internal beta-to-GA criteria (consultative sales model — pricing "Liên hệ" mặc định cho 60-70% tools)
- Customer satisfaction data (NPS/churn benchmarks Vietnam-specific) chưa publicly available — globally inferred từ B2B SaaS benchmarks
- PDPL 2026 ban hành 1/1/2026 — startup exemption pattern còn chưa fully tested trong VN SaaS ecosystem

---

## Per-vendor findings

### 1. MISA EMIS (`emis.misa.vn`) — Enterprise reference (30,000 schools)

| Aspect | Pattern |
|---|---|
| **Beta cohort scale** | N/A — KHÔNG có beta phase công khai; ship trực tiếp commercial. Direct payment + 12-month warranty model. |
| **Onboarding** | "Đăng ký dùng thử" form (no time-limit specified); paid setup fee mandatory (6M-10M VNĐ per module setup) + training service separately priced 2M-8M VNĐ |
| **Pricing during beta** | KHÔNG có beta discount công khai; commercial từ ngày 1: setup 6M-10M VNĐ + 2M-3M VNĐ/year renewal per module |
| **Feedback mechanism** | Support forum + YouTube tutorials + free consultation hotline; KHÔNG có structured NPS/feedback survey công khai |
| **PDPL approach** | "Kết nối CSDL Giáo dục Quốc gia" + electronic invoice compliance — enterprise-grade legal posture from day 1 |
| **Support model** | Hotline + email + video tutorials + support forum — multi-channel; large dedicated CS team |
| **Beta-to-GA** | N/A — MISA EMIS skipped beta as established player. Scale-from-day-1 model. |

**Key signal:** Enterprise VN edu vendors KHÔNG dùng "beta" terminology công khai — direct commercial launch với "dùng thử" demo gate optional.

### 2. Easy Edu (`easyedu.vn`) — Growth-stage reference (1,400+ centers, 5M+ users, 8 years)

| Aspect | Pattern |
|---|---|
| **Beta cohort scale** | KHÔNG advertise current beta program; mature platform với 8 năm scaling. Historical beta cohort không có data công khai. |
| **Onboarding** | "Dùng thử miễn phí" CTA repeat — registration form (name + phone + email + center name) → sales team contact; KHÔNG self-serve signup |
| **Pricing during beta** | "2.000đ/ngày/User" pricing transparent (~60.000đ/user/month) — freemium trial → paid conversion; KHÔNG có beta-specific discount |
| **Feedback mechanism** | Hotline 2 lines (sales + CS) + Facebook + YouTube + web chat + mobile app chat; "365 ngày/năm 7:00-22:00" SLA |
| **PDPL approach** | Zalo OA integration launched as feature for education business → cộng đồng phụ huynh + thông báo học sinh; PDPL-aware nhưng chưa public DPO listing |
| **Support model** | High-touch consultative sales → onboarding → multi-channel ongoing support. 2 hotlines (sales 0968291655 + CS 0846891655). |
| **Beta-to-GA** | N/A — graduated long ago. Current adoption pattern: trial → demo call → paid in <30 days. |

**Key signal:** Growth-stage VN edu SaaS = high-touch sales-led onboarding (NOT self-serve). Hotline + Zalo group là norm. Trial KHÔNG có time-limit công khai — flexible follow-up.

### 3. iLeader (`ileader.vn`) — Mid-market reference (800+ centers)

| Aspect | Pattern |
|---|---|
| **Beta cohort scale** | KHÔNG advertise beta; 800+ centers production-grade. |
| **Onboarding** | "Bản dùng thử miễn phí có thời hạn" — time-bound trial (duration KHÔNG specify on landing); 99% requirements satisfaction claim. |
| **Pricing** | "Liên hệ" mặc định — consultative sales model. |
| **Feedback** | Team expert consulting + technical support team — high-touch. |
| **PDPL approach** | Implicit; chưa public DPO/DPIA evidence. |
| **Support model** | Dedicated consulting team + technical staff (multi-tier support). |
| **Beta-to-GA** | N/A — production scale long achieved. |

**Key signal:** "Consultative sales" model dominant. Public pricing transparency hiếm; trial gate qua sales conversation thay vì self-serve.

### 4. Mona EduTech (`mona.software/edutech`) — SMB reference (~180+ customers per Flyer comparison)

| Aspect | Pattern |
|---|---|
| **Beta cohort scale** | KHÔNG có beta phase công khai. |
| **Onboarding** | "Bản dùng thử MIỄN PHÍ" — call/email sales team để access demo system; KHÔNG self-serve. |
| **Pricing** | 7M-30M VNĐ range (per Flyer.vn aggregator data) — wide variance suggests project-customized pricing. |
| **Feedback mechanism** | Direct sales contact channel; KHÔNG public NPS/structured feedback. |
| **PDPL approach** | KHÔNG public PDPL evidence. |
| **Support model** | 11 năm operation; technical staff support. |
| **Beta-to-GA** | N/A. |

**Key signal:** SMB-tier VN edu SaaS = project-customized model; high variance pricing (4x range 7M→30M); sales-led onboarding identical pattern.

### 5. EDUSPACE + Ayotree (transparent pricing — outliers)

| Vendor | Pricing transparency | Trial pattern |
|---|---|---|
| **EDUSPACE** | 390.000đ/month minimum, 5 package tiers | Monthly subscription self-serve hơn |
| **Ayotree** | $22/month + 1 month free trial | Web-based, no mobile (limitation) |

**Key signal:** Minority of VN edu SaaS có pricing transparency + self-serve trial. 1-month free trial là pattern cao nhất công khai (Ayotree). EDUSPACE entry tier 390k/month ≈ $16 — competitive với Asian SaaS standards.

### 6. Centercode + CustomerGauge (global SaaS beta-pattern reference)

| Source | Insight |
|---|---|
| **Centercode** | Beta NPS = 1 trong 5-7 quantitative launch criteria; KHÔNG nên là sole go/no-go; min 100 responses cho directional confidence, 200+ cho strategic decisions |
| **CustomerGauge SaaS 2025** | B2B SaaS NPS average ≈ 36; startups +28, growth +33, enterprise +35. NPS >50 → 40% lower churn. 63% SaaS churn happens trong first 90 days post-purchase. |
| **Beta graduation criteria typical** | (1) min NPS score, (2) max open bug count by severity, (3) min feature adoption rate, (4) min feedback coverage % enrolled testers, (5-7) custom per product. |

**Key signal:** Global beta-to-GA convention = 5-7 quantitative criteria, NPS chỉ 1 axis. KiteHub proposed scope thiếu explicit beta-to-GA criteria definition.

### 7. PDPL Decree 356/2025 (Vietnam-Briefing + DFDL legal alerts)

| Aspect | Requirement |
|---|---|
| **Effective date** | PDPL Law 1/1/2026; Decree 356/2025 guiding regs ban hành 31/12/2025 |
| **Startup exemption** | 5 năm exemption DPIA + DPO appointment cho startup/SMB **TRỪ KHI** doanh nghiệp là "data processing service provider" — SaaS thường disqualify |
| **Consent mandate** | Explicit + informed + freely given; blanket/passive KHÔNG còn được phép; verifiable mechanism + clear evidence (when/how/purpose) |
| **DPO** | Medium-large mandatory; SMB exempt 5 năm trừ khi là processor — **KiteHub là processor cho tenant data → likely NOT exempt** |
| **DPIA** | Submit within 60 days post-processing-start; data mapping + audit prerequisite |
| **Cross-border** | Impact assessments + MPS notifications mandatory (cloud servers Singapore/US/etc) |

**Key signal critical:** **KiteHub là multi-tenant SaaS data processor cho schools → KHÔNG eligible startup exemption per Decree 356**. PDPL compliance-min phải bao gồm: explicit consent UI + verifiable consent storage + DPO appointment + DPIA submission 60-day window + cross-border transfer assessment (if AWS Singapore primary).

---

## Pattern aggregate (consensus across vendors)

| Aspect | VN edu SaaS norm | KiteHub proposed (Wave beta-prep-1) | Gap? |
|---|---|---|---|
| **Beta terminology** | KHÔNG dùng "beta" công khai — "dùng thử miễn phí" / "demo" / "trial" | "Beta cohort" + invite mechanism (Bucket F) | ⚠️ Mismatch terminology — VN users không quen "beta" framing; có thể giảm sign-up conversion vs "dùng thử" framing |
| **Trial duration** | 7-30 days time-limited HOẶC flexible follow-up sales-led | KHÔNG specify trong scope | 🔴 GAP — Bucket F thiếu trial duration policy |
| **Pricing during beta** | Standard pricing với optional setup discount; KHÔNG "free credit" model dominant | KHÔNG specify | 🔴 GAP — beta pricing strategy chưa explicit (free? discounted? full price?) |
| **Onboarding model** | High-touch sales-led: form → call → demo → setup → training | "Onboarding script" (Bucket F) — script-only chưa rõ touch level | ⚠️ Bucket F scope thiếu touch-level decision (self-serve vs sales-led vs hybrid) |
| **Setup fee** | Mandatory 6M-10M VNĐ (MISA) OR included in monthly fee (Easy Edu/EDUSPACE) | KHÔNG specify | ⚠️ Setup-fee policy gap (revenue + complexity tradeoff) |
| **Support channels** | Hotline (mandatory) + Zalo OA + Facebook + YouTube + email + web chat + mobile app chat | Status page + P0 alerts (Bucket C ops-beta-min) — operational ops, NOT customer support | 🔴 GAP — Bucket C scope = ops monitoring ≠ customer support channels. KiteHub missing hotline policy + Zalo OA setup |
| **Support hours** | "365 ngày 7:00-22:00" standard cho top vendors | KHÔNG specify | 🔴 GAP — support SLA undefined |
| **PDPL compliance** | Implicit ở older vendors; Decree 356 forcing explicit DPO + DPIA + verifiable consent post-1/1/2026 | Bucket A: Privacy + Consent + Audit log + Retention + Breach SOP + DSAR endpoint | ✅ MOSTLY ALIGNED — but missing **DPO appointment** + **DPIA submission** explicit deliverables |
| **NPS / feedback mechanism** | Hotline-driven feedback common; structured NPS rare in VN edu | KHÔNG có NPS/structured feedback (Bucket F mention "onboarding script" only) | 🔴 GAP — beta program needs structured feedback (NPS + bug tracking + feature adoption metrics) for beta-to-GA criteria |
| **Beta-to-GA criteria** | KHÔNG vendor công khai criteria — KiteHub có cơ hội define explicit gate | KHÔNG specify | 🔴 GAP — Phase 1 → Phase 2 trigger ("Quality audit ≥80, 5 tenants live, 0 P0 incidents 2 weeks" per CLAUDE.md) chưa map vào Wave beta-prep-1 deliverables |
| **Cohort size** | Easy Edu/iLeader growth từ <100 ban đầu tới 800-1400; KHÔNG vendor công khai initial cohort | "5 beta tenants" implicit per CLAUDE.md (Phase 1 → Phase 2 trigger) | ⚠️ Cohort size scope thiếu Bucket F explicit acceptance |
| **Beta duration** | Phase 1 9-12 weeks per CLAUDE.md = 2-3 months — global beta convention 3-6 months | 9-12 weeks per CLAUDE.md (locked) | ✅ ALIGNED — duration within global SaaS beta convention |
| **Zalo OA integration** | Easy Edu launched as feature ("Tính năng Zalo OA chính thức ra mắt") — VN edu cultural must-have | KHÔNG có trong scope (per inside-out queue: Zalo OA defer Phase 1.5+) | ⚠️ MISS — competitive disadvantage if defer; nhưng explicit defer documented per `vn-localization-audit-checklist.md` §4 |
| **Tax invoice (eInvoice VAT)** | MISA EMIS native integration; standard mandatory cho >1 tỷ VNĐ revenue | KHÔNG có trong scope (per CLAUDE.md inside-out queue: MISA MeInvoice partnership defer Phase 2+) | ⚠️ MISS — but explicit defer documented per GAP-185 re-scope; OK cho beta cohort <1 tỷ revenue scale |
| **Phụ huynh communication** | Zalo group chat dominant cho parent ↔ center; SMS backup; email secondary | Email-only signup (per GAP-286 Phase 1.5 decision) | ⚠️ MISS — Bucket F onboarding script phải document Zalo group chat workflow alternative cho parent communication; pure email-only invite + onboarding sẽ miss VN edu cultural reality |
| **Setup wizard / hand-holding** | Easy Edu/iLeader high-touch consulting; Ayotree self-serve outlier | Bucket F "onboarding script" — chưa rõ self-serve vs hand-holding | 🔴 GAP — touch-level decision unclear (impacts CSM headcount + cost projection) |

---

## Blind spots surfaced (NOT in proposed Wave beta-prep-1 scope A-F)

| # | Severity | Vendor evidence | Description | Suggested bucket / fix |
|---|---|---|---|---|
| **1** | 🔴 P0 | DFDL legal alert: "DPO required for processors regardless of startup status" | **DPO appointment KHÔNG có trong Bucket A** — KiteHub là SaaS data processor → NOT exempt per Decree 356. Phase 1 BETA invite without DPO designated = legal risk + tenant trust gap | Add to Bucket A: **DPO appointment (formal designation + contact published)** trước 1 invite go-out. ETA: 1-2 days founder-as-acting-DPO + public listing |
| **2** | 🔴 P0 | Vietnam-Briefing: "DPIA submit within 60 days post-processing-start" | **DPIA submission deliverable KHÔNG explicit Bucket A** — Decree 356 yêu cầu submit DPIA trong 60 ngày sau processing start; với beta tenant onboarding 1-N=processing start, DPIA timeline starts | Add to Bucket A: **DPIA template + submission tracker** với 60-day countdown trigger từ first tenant signup |
| **3** | 🔴 P0 | Easy Edu / iLeader pattern: 2 hotlines (sales + CS) + Zalo + Facebook + multi-channel | **Customer support channels KHÔNG có scope Wave beta-prep-1** — Bucket C ops-beta-min covers ops monitoring (status page + P0 alerts + restore drill), NOT customer-facing support | Add new bucket OR extend Bucket F: **Beta tenant support channels** = hotline (1 line minimum) + Zalo OA support group + email support@ + SLA "<24h response" cho beta cohort |
| **4** | 🟠 P1 | Centercode + Customer Gauge global SaaS pattern | **Beta-to-GA criteria undefined explicit deliverable** — CLAUDE.md có Phase 1→2 trigger ("Quality audit ≥80 / 5 tenants live / 0 P0 incidents 2 weeks") nhưng không có dashboard / tracking artifact trong Wave beta-prep-1 | Add to Bucket F: **Beta graduation dashboard** — Phase 1→2 trigger metrics tracked (audit score + active tenant count + incident streak) — visible to user + auditor |
| **5** | 🟠 P1 | Easy Edu Zalo OA feature launch; VN edu cultural norm parent communication | **Zalo group chat workflow KHÔNG document** — onboarding script (Bucket F) thiếu Zalo OA setup playbook cho beta tenant để communicate với phụ huynh (cultural must-have per Wave 100 Bucket D VN-localization audit) | Extend Bucket F: **Zalo group chat onboarding playbook** cho tenant — "How to setup Zalo group chat for parent communication" doc (defer Zalo OA platform integration to Phase 1.5+, but tenant-side manual workflow documented now) |
| **6** | 🟠 P1 | Decree 356: explicit + verifiable consent | **Consent storage verifiability KHÔNG explicit Bucket A** — Bucket A mentions "Consent" but chưa rõ implementation level (UI checkbox? Audit log immutable evidence? Withdrawal mechanism?) | Sharpen Bucket A scope: **Consent = explicit checkbox + immutable storage (per `output-review-mandate.md` §3 admin_audit_logs PDPL Art 11) + withdrawal endpoint + evidence export per DSAR** — should pair với GAP-577 cookie consent shipped Wave 86 |
| **7** | 🟠 P1 | MISA / Easy Edu: high-touch sales-led onboarding (NOT self-serve) | **Touch-level decision unclear Bucket F** — "onboarding script" ambiguous between (a) self-serve wizard UI, (b) sales-led video call script, (c) hybrid. VN edu norm = sales-led (high-touch) | Force Bucket F decision: **Choose onboarding touch level explicitly** — recommend hybrid: self-serve signup + scheduled 30-min onboarding call cho beta cohort (sales-led để collect feedback + close NPS gap) |
| **8** | 🟡 P2 | EDUSPACE / Ayotree transparent pricing outliers | **Beta pricing strategy KHÔNG defined** — Wave beta-prep-1 scope thiếu pricing decision (free? discounted? full?) | Add to Bucket F: **Beta cohort pricing decision** — recommend "free for first 30 days then transitioned to standard pricing or extended trial" — match Ayotree pattern; document trong invite landing page |
| **9** | 🟡 P2 | Centercode beta NPS pattern: 100+ responses minimum | **NPS / structured feedback mechanism missing** — Bucket F scope chỉ "onboarding script" — không có post-onboarding survey + ongoing NPS tracking | Add to Bucket F: **Structured feedback tool** — 1 NPS survey post-week-1 + 1 NPS survey post-week-4 + monthly Zalo group office hours (Centercode pattern adapted to VN context) |
| **10** | 🟡 P2 | Easy Edu: 365-day support 7:00-22:00 | **Support SLA undefined** — Bucket F + Bucket C thiếu commitment thời gian response cho beta tenant | Add to Bucket F: **Beta SLA published** — recommend "weekday 9:00-18:00 response <8h, weekend best-effort" (realistic solo-dev + tenant-trust signal) |
| **11** | 🟡 P2 | Hotline 0xxxx mandatory cho mọi competitor công khai | **Phone hotline KHÔNG có Bucket C/F** — VN edu user expectation = phone hotline available; KiteHub email-only = competitive disadvantage signal | Defer-with-rationale: document Phase 1 BETA hotline = founder personal number cho cohort <10 tenants, scale up Phase 2 (NOT add to Wave beta-prep-1 scope but document explicit defer in invite landing) |
| **12** | 🟢 P3 | MISA EMIS 12-month warranty + electronic invoice native | **eInvoice VAT integration defer documented OK** — per CLAUDE.md inside-out queue MISA MeInvoice partnership Phase 2+ — no action needed Wave beta-prep-1 IF beta cohort <1 tỷ VNĐ aggregate annual revenue | Document trong beta invite: "VAT eInvoice integration roadmap Phase 2"; beta cohort revenue projection check (5 tenants × 1.5M VNĐ × 12 months = 90M VNĐ << 1 tỷ threshold — OK) |

**Verdict:** 12 blind spots surfaced — **3 P0 critical (DPO + DPIA + customer support channels)**, 4 P1 important (beta-to-GA criteria + Zalo workflow + consent verifiability + touch-level), 4 P2 nice-to-have (pricing + NPS + SLA + hotline), 1 P3 documented defer.

---

## Validate existing scope (A-F) vs industry pattern

| Bucket | Industry norm | KiteHub proposed | Verdict |
|---|---|---|---|
| **A — PDPL compliance-min (5 items)** | Decree 356/2025 mandates: explicit consent + DPO + DPIA + breach SOP + DSAR + cross-border assessment | Privacy + Consent + Audit log + Retention + Breach SOP + DSAR endpoint | ⚠️ INCOMPLETE — missing DPO + DPIA + cross-border assessment (AWS Singapore primary). Expand from 5→8 items |
| **B — Security-beta-min (P0 CVE only)** | Trivy + Dependabot mainstream; OWASP Top 10 baseline cho VN edu vendors implicit | P0 CVE only | ✅ PRAGMATIC — P0 scope OK cho beta cohort; defer OWASP full audit Phase 2 acceptable per `release-fix-retry-budget.md` §3 pattern (pre-release softer gates) |
| **C — Ops-beta-min (status page + P0 alerts + restore drill)** | Easy Edu 365-day 7:00-22:00 + multi-channel; restore drill standard | Status page + P0 alerts + restore drill | ⚠️ MISLABELED — Bucket C scope = ops/SRE infrastructure NOT customer support. Industry norm bao gồm customer support channels SEPARATELY. Need explicit clarification — Bucket C ≠ tenant support |
| **D — GAP-727 class-teacher-fix** | Feature fix, not beta-specific | GAP-727 | ✅ OK — feature gate, beta dependency check passes |
| **E — GAP-730 idempotency-finish** | Feature fix, not beta-specific | GAP-730 | ✅ OK — feature gate |
| **F — Beta invite mechanism + onboarding script** | Hotline-driven sales-led onboarding + Zalo OA + structured feedback + clear pricing | Beta invite + onboarding script | 🔴 UNDERSCOPED — missing touch level decision + Zalo workflow + structured feedback (NPS) + pricing + SLA + support channels |

---

## Recommended scope adjustment

### Recommended scope structure (revised 6→7 buckets)

| Bucket | Original scope | Recommended adjustment |
|---|---|---|
| **A — PDPL compliance-FULL** (expand from "min") | Privacy + Consent + Audit log + Retention + Breach SOP + DSAR | **+ DPO formal appointment + DPIA template + 60-day submission tracker + cross-border transfer assessment (AWS Singapore)** = 8 items total. Pair with GAP-577 (cookie consent shipped Wave 86) + GAP-585 (Decree 13 audit shipped Wave 86) |
| **B — Security-beta-min** (no change) | P0 CVE only | ✅ Keep as-is — pragmatic pre-release gate per `release-fix-retry-budget.md` §3 |
| **C — Ops-beta-min** (rename for clarity) | status page + P0 alerts + restore drill | **Rename to "Infrastructure ops baseline"** — clarify scope = ops/SRE NOT tenant support; same deliverables |
| **D — GAP-727 class-teacher-fix** (no change) | Feature fix | ✅ Keep |
| **E — GAP-730 idempotency-finish** (no change) | Feature fix | ✅ Keep |
| **F — Beta invite + onboarding (EXPANDED)** | Beta invite mechanism + onboarding script | **Expand:** (1) Beta invite landing page với pricing/duration/SLA explicit, (2) Onboarding playbook hybrid touch (self-serve signup + 30-min sales-led video call), (3) Zalo group chat playbook tenant-side, (4) Beta-to-GA dashboard (3 metrics: audit score + tenant count + incident streak), (5) NPS structured feedback (week 1 + week 4 surveys), (6) Beta SLA published (weekday 9-18h response <8h) |
| **G — Tenant support channels** (NEW BUCKET) | — | **NEW:** Hotline (founder personal number cho <10 tenants), Zalo OA support group setup, support@kitehub.me email response infra, public defer-rationale doc cho future scaling. Sister bucket complementary to F. |

### Decision matrix for user

**Option Recommended (Min-viable + add 1 bucket):** Accept all 7 buckets (A expanded + C renamed + F expanded + G new) — increases scope ~30% but closes 3 P0 blind spots before invite go-out.

**Option Aggressive (defer P0 blind spots):** Keep 6 original buckets, file follow-up gaps cho 3 P0 blind spots (DPO + DPIA + customer support) — risk legal exposure post-1/1/2026 PDPL active + tenant trust gap if hotline missing.

**Option Conservative (extend timeline):** Split Wave beta-prep-1 into 2 sub-waves: beta-prep-1a (A expanded + C + D + E) shipping first, beta-prep-1b (B + F expanded + G) shipping 1-2 weeks later — sequential reduces concurrent agent count.

**Recommendation:** **Option Recommended (7 buckets)** — PDPL deadline 2026-07-01 is 6 weeks away (per CLAUDE.md); deferring DPO + DPIA = directly compromise that hard deadline + Phase 1 BETA legal posture.

---

## Sources

- [MISA EMIS Pricing](https://emis.misa.vn/bao-gia/)
- [Easy Edu Platform](https://easyedu.vn/)
- [Flyer.vn English center mgmt comparison](https://flyer.vn/phan-mem-quan-ly-trung-tam-tieng-anh/)
- [iLeader landing](https://ileader.vn/)
- [Mona EduTech](https://mona.software/edutech/)
- [Vietnam-Briefing Decree 356/2025](https://www.vietnam-briefing.com/news/vietnam-personal-data-protection-regulation-decree-356.html/)
- [DFDL PDPL 2026 alert](https://www.dfdl.com/insights/legal-and-tax-updates/vietnam-personal-data-protection-2026-what-foreign-organizations-need-to-know/)
- [Centercode beta NPS pattern](https://centercode.com/blog/2015/10/how-to-use-net-promoter-score-in-your-beta-program)
- [CustomerGauge SaaS NPS 2025](https://customergauge.com/benchmarks/blog/nps-saas-net-promoter-score-benchmarks)
- [Lexology Decree 356 key points](https://www.lexology.com/library/detail.aspx?g=d210dc97-5b76-41f4-8a43-4a7e2f00c30d)
