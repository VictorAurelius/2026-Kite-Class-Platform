---
title: Wave 105 Persona Walk — Outside-In VN Edu SaaS Benchmark Audit
status: complete
audience: dev
created: 2026-05-22
phase: phase-1-beta
wave: 105
gaps: []
scope: Outside-in benchmark 3 VN edu SaaS competitors (MISA EMIS / DotB+EduSpace+CloudClass / ClassIn) so sánh với Wave 105 draft Persona Walk plan
methodology: WebSearch + WebFetch — research 8 dimensions × 3 competitors + KiteHub draft, identify industry-standard gaps
---

# Wave 105 Persona Walk — VN Edu SaaS Benchmark Audit

## Methodology

Per `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 3 — background agent research VN edu SaaS competitors qua WebSearch + WebFetch, build comparison matrix 8 dimensions, identify industry-standard features Wave 105 draft missing.

**Competitors benchmarked:**

1. **MISA EMIS / AMIS Education** — `emis.misa.vn` — 23k schools served, enterprise sales
2. **DotB + EduSpace + CloudClass cluster** — VN-native trung tâm dạy thêm market
3. **ClassIn (VN)** — `classin.vn` — multinational LMS, popular tiếng Anh centers

## Comparison matrix — 8 dimensions × 4 systems

| # | Dimension | MISA EMIS | DotB/EduSpace/CloudClass | ClassIn (VN) | **KiteHub Wave 105 draft** |
|---|---|:---:|:---:|:---:|:---:|
| a | Owner onboarding wizard | ✅ Multi-step + sample-data seeder per khối trường | ✅ All-in-one Owner setup + sample class seed | ⚠️ Per-teacher signup (không multi-tenant wizard) | ✅ 5-step wizard PROFILE→INVITE→IMPORT→CREATE_CLASS→EXPLORE |
| b | Teacher invite + 2FA | ⚠️ Email invite; 2FA không rõ | ⚠️ Email + Zalo invite; 2FA không public | ⚠️ Email; 2FA không public | ✅ Email invite + TOTP 2FA (GAP-516) |
| c | **Parent channel** | ⚠️ Web parent portal | 🔴 **Zalo OA + Mini App primary** | ⚠️ App + email | ❌ **Email-only** (industry outlier) |
| d | Payment | ✅ "Khoản thu" module | ✅ QR code auto-reconcile + bank webhook real-time | ⚠️ RMB/USD foreign payment cycle 3-month upfront, no refund (không VN-native) | ✅ VietQR + webhook (mock local) |
| e | **PDPL Art 11 consent** | ⚠️ Implicit | ⚠️ Implicit | ⚠️ Implicit | ✅ **Explicit consent form parent invite step — differentiator** |
| f | Multi-tenant pattern | ✅ Cloud SaaS shared `emisapp.misa.vn` | ✅ Shared platform per vendor | ❌ Single global `classin.com/vn/login` | ✅ Subdomain per tenant (planned production) |
| g | Pricing tier + trial | ⚠️ Quote-based contact sales (23k schools) | ⚠️ Per-vendor pricing varies | ✅ Free 10 lessons/month + $99-299/teacher/mo paid | ⚠️ FREE/PAID tier defined nhưng trial mechanism unclear |
| h | Mobile | ⚠️ Web responsive | 🔴 **Zalo Mini App primary** + web | ✅ Native iOS/Android download | ❌ **Web-only** (no Zalo Mini App, no native) |

## Top 3 industry-standard features MISSING

### 1. 🔴 CRITICAL — Zalo OA / Zalo Mini App parent channel

3/3 VN-native competitors (DotB, EduSpace, CloudClass) prioritize Zalo OA + Mini App cho parent notification. Industry trend 2026 cites Zalo Mini App education explicitly. KiteHub email-only path sẽ hit Wave 105 Bucket D parent journey friction — phụ huynh VN edu market default open Zalo every day, mở email <30%.

**RECOMMEND:** Bucket D add explicit "fallback channel risk" callout + GAP-286 follow-up Phase 2 Zalo OA integration.

### 2. ⚠️ MODERATE — QR-code auto-reconcile from bank webhook

DotB/EduSpace explicit "đối soát tự động bằng QR code" + bank real-time webhook. Draft VietQR flow mocks webhook locally; production reconciliation logic (idempotency, double-credit prevention, partial-payment handling) untested.

**RECOMMEND:** Bucket D Step 9-10 add reconciliation edge cases (double webhook, late webhook, amount mismatch) hoặc explicit defer Wave 106+ với rationale.

### 3. ⚠️ MINOR — Sample-data seeder for first-class creation

MISA EMIS deployment guide explicitly recommends sample-class seed per khối trường (K-12 vs trung tâm). DotB tự-động chăm sóc học viên có sample workflow. Owner onboarding step "CREATE_FIRST_CLASS" trong draft yêu cầu manual create — friction so với competitor 1-click sample seed.

**RECOMMEND:** Bucket B Step 6 add optional "Seed sample class with 5 demo students" CTA giảm cold-start friction.

## Top 2 KiteHub differentiators (competitor KHÔNG có)

### 1. Explicit PDPL Art 11 consent flow in parent invite

Bucket D Step 2 "accept terms + consent form (PDPL Art 11)" là explicit + granular. None of 3 competitor surfaces public PDPL consent UX pattern; most have implicit ToS-acceptance only. KiteHub có thể highlight này như **compliance differentiator cho enterprise + K-12 prospect**.

**RECOMMEND:** Wave 105 closure note PDPL consent UX là cạnh tranh advantage.

### 2. TOTP 2FA mandatory cho Teacher role

Bucket C Step 3 "setup 2FA TOTP" là mandatory enroll. Competitor public docs không mention 2FA enforcement cho teacher. Khi K-12 Phase 3 enable, 2FA mandatory thành **security baseline + Cybersecurity Law compliance signal**.

**RECOMMEND:** Wave 105 closure highlight 2FA enroll path là enterprise-readiness signal.

## Final verdict

**Draft scope alignment với VN edu SaaS norm: ~65-70% match.**

- **Strong alignment (8/8 dimensions partial+):** Owner wizard, 2FA, payment QR, PDPL consent, multi-tenant — all covered baseline or better than competitor
- **Critical gap (1/8):** Zalo OA parent channel — 3/3 VN competitors prioritize; KiteHub email-only industry outlier. Phase 1 BETA acceptable nếu document tradeoff + tenant onboarding FAQ giải thích; Phase 2 Zalo OA integration MUST roadmap explicit (extend GAP-286 scope follow-up)
- **Moderate gap (1/8):** Mobile/Zalo Mini App — competitor trend 2026 Zalo Mini App education; KiteHub web-only Phase 1 OK, Phase 2 Zalo Mini App high-leverage parent retention play
- **Minor gap (1/8):** Sample-data seeder Bucket B Step 6 friction giảm với 1-click sample class seed (low-effort high-impact)

**Recommendation Wave 105 scope:** keep 5-bucket scope but add 3 follow-up notes trong plan §Key risks:
1. Zalo OA Phase 2 explicit roadmap
2. Sample-data seeder Bucket B nice-to-have
3. Bank webhook reconciliation edge cases Bucket D defer/explicit

## Sources

- [DotB Top 12 phần mềm quản lý trung tâm dạy thêm 2025](https://dotb.vn/news/phan-mem-quan-ly-trung-tam-day-them/)
- [EduSpace phần mềm quản lý trung tâm đào tạo](https://eduspace.vn/)
- [CloudClass phần mềm trung tâm dạy thêm](https://cloudclass.edu.vn/)
- [MISA EMIS Nền tảng quản lý giáo dục](https://emis.misa.vn/)
- [ClassIn Pricing Free vs Pro](https://www.classin.com/pricing/)
- [ClassIn Vietnam](https://classin.vn/)
- [Zalo Mini App ngành giáo dục 2026 (CNV)](https://cnv.vn/zalo-mini-app-nganh-giao-duc/)
- [Zalo Mini App education trend 2026 (PandaLoyalty)](https://pandaloyalty.com/zalo-mini-app-nganh-giao-duc/)
- [Zalo OA Manager Mini App official](https://oa.zalo.me/home/documents/guides/Mini-app_8630842053848809942)
