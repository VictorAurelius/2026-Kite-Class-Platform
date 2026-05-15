---
title: Wave 86 Bucket A — External Benchmark Outside-In (VN SaaS Edu Industry)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 86
bucket: A
audit_type: outside-in / external-benchmark
methodology: WebSearch industry references (9 queries) + cross-reference Wave 86 inside-out scope
related_gaps: [GAP-440, GAP-537c, GAP-412]
related_rules: [outside-in-coverage-trigger.md, dev-readable-doc-language.md]
---

# Wave 86 — External Benchmark Outside-In (VN SaaS Edu)

## 1. Scope

Wave 86 = consolidation wave pre-rc1 (8 buckets: outside-in audit / Spring Boot bump / P2-P3 user manual screenshots / AWS Activate resubmit / 5 pre-launch hardening verify / tag `v1.0.0-rc.1` / first 5 beta cohort invite / monitoring + incident response). Bucket A audit này focus góc nhìn **external benchmark VN SaaS edu industry** — bù trừ với persona simulation (Agent 1) và failure-mode matrix (Agent 3) trong cùng Bucket A.

**Outside-in trigger fired** per `outside-in-coverage-trigger.md` §3 vì Wave 86 chạm user-facing path: signup flow (anonymous), onboarding wizard (P2 Owner), invite accept (P3 Manager), trust signals (TOS, Privacy, status page, refund policy).

**Mục tiêu:** surface gaps Wave 86 inside-out scope (5 pre-launch checklist + tag + invite) có thể MISS, đặc biệt signup conversion + first-30-day retention + trust expectations từ VN B2B SaaS user.

---

## 2. Methodology

**Tools:** WebSearch (9 queries).
**Query targets:**

1. `Misa AMIS VN SaaS signup conversion rate trial paid pricing tier 2026`
2. `Edupia Hocmai Vietnam edtech SaaS onboarding wizard conversion benchmark`
3. `B2B SaaS first 30 day retention churn benchmark education vertical 2026`
4. `Vietnam PDPL Decree 13 2023 cookie consent banner SaaS compliance UX best practice`
5. `SaaS email verification link expiry timeout 15 minutes 24 hours trust benchmark`
6. `VN SaaS B2B pricing tier public hidden behind signup psychology benchmark Misa Base`
7. `SaaS status page beta cohort visibility customer trust transparency benchmark`
8. `B2B SaaS support first response SLA Vietnam Zalo OA email phone benchmark`
9. `SaaS onboarding wizard steps optimal number drop-off conversion benchmark 5 7 steps`
10. `SaaS refund policy 30 day money back guarantee B2B small business benchmark`

**Limitations:**
- VN edu SaaS (Misa, Edupia, Hocmai) **không public** signup conversion / first-30-day retention metrics → phải fallback global B2B SaaS industry benchmarks
- VN-specific Zalo OA SLA benchmark data thiếu — chỉ có context "87% VN smartphone users on Zalo"
- General SaaS benchmark data 2026 đầy đủ qua First Page Sage / Kirro / ChartMogul / Artisan Strategies

---

## 3. Findings — 10 questions vs industry standards

### Q1. Signup conversion baseline (visitor → signup)

| Source | Benchmark |
|---|---|
| **B2B SaaS overall visitor-to-paid** | 2-3% (Kalungi, Userpilot) |
| **B2B SaaS visitor-to-signup** | typical 4-7% landing page (no hard VN edu number) |
| **Edge case Edupia trajectory** | 5M users / 400k paying → ~8% user-to-paid (unique but consumer K-12 not B2B) |

**Wave 86 acceptable threshold:** với 5-tenant manual invite (Bucket G), conversion rate **không relevant pre-rc1**. Pre-public-launch sẽ relevant. **Verdict: Bucket G out-of-scope conversion measurement; chỉ measure invite-accept rate.**

### Q2. First-30-day retention (early churn)

| Source | Benchmark |
|---|---|
| **70% of churn xảy ra 90 ngày đầu** (Userpilot 2026) | Critical period |
| **Companies với time-to-first-value <7 days → 50% lower churn** | Activation matters |
| **20%+ voluntary churn linked to poor onboarding** | First-impression damage |
| **B2B SMB monthly churn** | 3-5% (best-in-class <1%) |

**Wave 86 implication:** Bucket H monitoring SHOULD measure **D7 + D14 + D30 activation milestones cho 5 cohort** (không chỉ D1 churn). Wave 86 §3 Bucket H hiện tại nói "first-day churn rate" — **GAP**: thiếu D7/D14/D30 cohort tracking framework.

### Q3. Pricing presentation (public vs behind signup)

| Source | Benchmark |
|---|---|
| **Tiered pricing avg 3.5 packages** (low/mid/high) | Standard |
| **"Most Popular" badge** reduces decision anxiety | Social proof lever |
| **Hidden pricing → bounce** "khó khăn signup → tìm chỗ khác" | Anti-pattern |

**Wave 86 implication:** Pricing already public per Wave 85 (xác nhận từ wave plan §3 ref). **Verdict: OK pre-rc1. Future: add "Most Popular" badge cho Gói Trial trên pricing.**

### Q4. Onboarding wizard length

| Source | Benchmark |
|---|---|
| **Optimal 4-7 steps** (Arcade, Chameleon) | Range |
| **Checklist 3-5 items > 8+ items completion** | Tighter is better |
| **Users complete <50% steps in 14d → 3× churn rate** | Drop-off cost |
| **Onboarding completion → 5× more likely convert** (Intercom) | High leverage |

**Wave 86 implication:** P2 Center Owner onboarding wizard GAP-537c capture 8 screens. **Check: nếu wizard >7 steps → BLOCKING** (cần audit wizard step count Wave 86 Bucket C). **Findings + AC**: Bucket C MUST verify wizard ≤7 steps; ngoài ra Bucket A persona Agent 1 sẽ catch UX detail.

### Q5. Email verification link expiry

| Source | Benchmark |
|---|---|
| **Standard email verification: 24h** | Industry norm |
| **Magic links / 2FA: 10-15 min** | Security-sensitive |
| **Password reset: 24h** | Standard |

**Wave 86 implication:** Beta invite email link expiry → no current published spec trong wave plan. **GAP P2: tag `v1.0.0-rc.1` PHẢI lock invite email link expiry policy: 24h cho first invite + 15min cho password reset.** Verify Wave 86 Bucket G invite email implementation match this. Cross-check `pre-launch-auth-hardening-checklist.md` Cat 4 §2.8 token rotation rule.

### Q6. 2FA mandatory cho Owner role

| Source | Benchmark |
|---|---|
| **VN B2B SaaS general** | Optional + nudge most common |
| **Top-performing SaaS** | Mandatory cho admin/owner roles |
| **Misa AMIS / Base.vn** | Optional verified accounts |

**Wave 86 implication:** Phase 1 BETA scope = OK với optional 2FA + dashboard nudge. **GAP P2: post-rc1 Wave 87+ mandate 2FA cho P2 Owner role (security audit Cat 4 row).** Not blocking rc1 tag.

### Q7. Cookie consent UX (PDPL Decree 13)

| Source | Benchmark |
|---|---|
| **Decree 13/2023 effective 2023-07-01** | Active law |
| **Granular consent required** (every distinct purpose) | Bundled = invalid |
| **No dark patterns** | Banned |
| **No-action = no consent** | Strict |
| **Cookie banner click-rate** (industry): 60-75% accept-all khi banner clear |

**Wave 86 implication:** Critical compliance check. Wave 86 wave plan §3 Bucket E nói "CSP report-only acceptable v1" nhưng KHÔNG mention cookie consent banner. **🚨 GAP P0 BLOCKING: pre-rc1 PHẢI verify cookie consent banner shipped + granular consent per purpose + no dark pattern + consent log retained.** Cross-check security audit Cat 9 PDPL row. **Recommendation:** add Bucket E sub-item E.6 "Cookie consent banner PDPL Decree 13 compliance verification".

### Q8. Status page visibility (beta cohort)

| Source | Benchmark |
|---|---|
| **Atlassian Statuspage** | Industry standard tool |
| **81% customers consider trust key purchase factor** (HubSpot 2022) | Trust lever |
| **Public roadmap + status page** | Bootstrapped SaaS growth lever |

**Wave 86 implication:** Wave 84 GAP-424 đã ship Statuspage VN overlay (per output-review-mandate.md §3 line 84 audit). **Verify Bucket G invite email link to status page URL.** Tốt nếu beta tenants nhận status page URL trong welcome email — surface gap nếu thiếu.

### Q9. Support response SLA (first response)

| Source | Benchmark |
|---|---|
| **B2B SaaS email target** | 4-6 hours typical, top-tier <1h |
| **Strategic accounts** | 2-4 hours |
| **Web chat** | <1 min, AI-assisted |
| **Zalo OA VN** | 87% smartphone users active → expect <1h response window |

**Wave 86 implication:** Pre-rc1 5-tenant cohort manual SLA = no automated tooling. **GAP P2: pre-rc1 PHẢI define + publish first-response SLA cho 5 beta cohort.** Recommend "<4h business hours via email + <1h via Zalo OA (Phase 1.5+)". Currently `user-manual-content-standard.md` §2 row 5 mention Zalo OA defer Phase 1.5+ → ALIGNED. **Verdict: explicit SLA doc in Bucket G + Bucket H runbook needed.**

### Q10. Refund policy 30-day money-back

| Source | Benchmark |
|---|---|
| **30-day money-back guarantee → 21% sales lift** | High leverage |
| **Refund request rate** ~12% | Trade-off |
| **Industry practice** | Microsoft Azure 30d cancel; common SaaS pattern |
| **Complex products** | 30-day appropriate (vs 7d for simple) |

**Wave 86 implication:** Pre-rc1 beta cohort = FREE invite (no payment) → refund N/A. **Post-rc1 (v1.0.0 GA)** PHẢI lock refund policy doc. **GAP P2 follow-up: refund policy 30-day money-back để post-rc1 wave (Wave 88+ launch readiness).** Not blocking rc1.

---

## 4. Gap analysis — Wave 86 hiện tại vs VN industry

| Aspect | Wave 86 scope | Industry benchmark | Verdict |
|---|---|---|---|
| Conversion measurement (Bucket G/H) | "first-hour signup completion + first-day churn" | D7/D14/D30 milestone tracking | ⚠️ BELOW — thiếu cohort milestones |
| Pricing presentation | Public (Wave 85 ship) | Tiered avg 3.5 + Most Popular badge | ✅ MATCH — minor enhancement Wave 87+ |
| Onboarding wizard | P2 capture 8 screens (GAP-537c) | 4-7 steps optimal | ⚠️ VERIFY step count ≤7 |
| Email verification link | Wave 86 không explicit | 24h standard | ⚠️ BELOW — không spec'd |
| 2FA Owner role | Optional v1 | Optional+nudge common | ✅ MATCH Phase 1 |
| Cookie consent banner | Không mention Bucket E | PDPL Decree 13 mandate | 🚨 BELOW — BLOCKING gap |
| Status page link in invite | Not specified Bucket G | Trust lever benchmark | ⚠️ BELOW — verify |
| Support SLA explicit | Implicit Bucket H runbook | 4-6h email industry | ⚠️ BELOW — không publish'd |
| Refund policy | N/A (beta free) | Post-launch lock | ⚠️ DEFER Wave 88+ |
| First-30d retention monitoring | "first-day churn" only | D7/D14/D30 70% churn window | ⚠️ BELOW — thiếu framework |

---

## 5. AC additions suggested per bucket

### Bucket E — Pre-launch hardening verification

- [ ] **E.6 NEW** — Cookie consent banner PDPL Decree 13 compliance:
  - Granular consent per purpose (analytics / marketing / functional split)
  - No dark pattern (no pre-checked boxes, equal-weight Accept/Reject buttons)
  - Withdraw consent mechanism trong footer settings
  - Consent log retained ≥3 năm per Decree 13 retention
  - Self-test: `curl -sI https://kitehub.me/ | grep -i 'set-cookie'` không có analytics cookie SET trước user explicit accept

- [ ] **E.7 NEW** — Email verification link expiry policy:
  - First signup invite: 24h
  - Password reset: 15 min
  - Owner role 2FA backup code: 10 min
  - Verify implementation match `pre-launch-auth-hardening-checklist.md` §2.8

### Bucket G — Beta cohort invite

- [ ] **G.5 NEW** — Invite email content:
  - Link tới `/status` (Statuspage URL từ Wave 84 GAP-424)
  - Link tới support channel (email + Zalo OA defer note)
  - First-response SLA explicit "<4h business hours via email"
  - TOS + Privacy + Beta disclaimer (đã có)
  - Welcome page guide URL `/help/p1-solo-teacher` or `/help/p2-owner` per persona (GAP-537c)

### Bucket H — Monitoring + incident response

- [ ] **H.5 NEW** — Cohort retention tracking framework:
  - D7 activation milestone: % của 5 tenants đã tạo ≥1 lớp / mời ≥1 student
  - D14 retention: % vẫn active login ≥1× tuần qua
  - D30 retention: % vẫn active OR có churn reason captured
  - Dashboard hoặc spreadsheet manual cho 5 cohort (Phase 1 BETA size cho phép manual)
  - Activation milestone trigger Zalo OA outreach proactive nếu D7 activation <50%

- [ ] **H.6 NEW** — First-response SLA published + tracked:
  - SLA doc trong `documents/05-guides/operations/support-sla-phase-1-beta.md`
  - Tracking spreadsheet response time per ticket
  - Pattern >4h response → file follow-up gap

---

## 6. NEW gap proposals — Wave 86 propose

### GAP-NEW-1: Cookie consent banner PDPL Decree 13 compliance verification

- **Priority:** P0 BLOCKING tag `v1.0.0-rc.1`
- **Phase:** phase-1-beta
- **Rationale:** Decree 13/2023 active 2023-07-01; granular consent + no dark pattern + consent log mandate. Tag rc1 trước khi verify = compliance risk + first beta tenants có thể flag immediately.
- **Acceptance:** Bucket E.6 verification PASS (5 sub-items §5 above)
- **Owner:** coordinator + security review

### GAP-NEW-2: Email verification + password reset link expiry policy spec

- **Priority:** P1
- **Phase:** phase-1-beta
- **Rationale:** Industry standard 24h verification + 15min reset chưa được explicit document trong wave plan; cross-check `pre-launch-auth-hardening-checklist.md` §2.8 token rotation rule.
- **Acceptance:** spec doc + implementation match + integration test verify expiry honored
- **Owner:** coordinator

### GAP-NEW-3: Cohort retention tracking framework D7/D14/D30

- **Priority:** P1
- **Phase:** phase-1-beta
- **Rationale:** 70% churn xảy ra 90d đầu (industry); Wave 86 §3 Bucket H chỉ track "first-day churn" — thiếu D7/D14/D30 activation milestone framework. Force-multiplier cho Phase 1.5+ scaling (>5 cohort).
- **Acceptance:** dashboard or spreadsheet template + ZNS proactive trigger rule + churn reason capture form
- **Owner:** coordinator + future Bucket H

### GAP-NEW-4: First-response SLA published doc cho Phase 1 BETA

- **Priority:** P2
- **Phase:** phase-1-beta
- **Rationale:** Industry 4-6h email standard; 5-tenant cohort manual hiện chưa publish SLA → ambiguous expectation → trust risk.
- **Acceptance:** `documents/05-guides/operations/support-sla-phase-1-beta.md` doc + invite email cite SLA + tracking spreadsheet
- **Owner:** coordinator + Bucket G email content

### GAP-NEW-5: Onboarding wizard step count ≤7 audit

- **Priority:** P2
- **Phase:** phase-1-beta
- **Rationale:** Industry optimal 4-7 steps; >7 → 3× churn rate. Bucket C GAP-537c capture 8 screens (gross step count = ?); verify wizard ≤7 actionable steps (vs 8 screens including success page).
- **Acceptance:** step count audit trong P2 user manual + UI screenshot review confirm ≤7 form interaction
- **Owner:** Bucket C coordinator + Playwright

### GAP-NEW-6: Most Popular pricing badge UX enhancement

- **Priority:** P3 (defer Wave 87+)
- **Phase:** phase-1-beta / phase-1.5
- **Rationale:** Industry social proof lever — "Most Popular" badge reduces decision anxiety; not blocking rc1.
- **Acceptance:** badge component + applied to Gói Trial tier
- **Owner:** future FE wave

### GAP-NEW-7: Refund policy 30-day money-back doc (post-launch)

- **Priority:** P2 (defer Wave 88+ launch)
- **Phase:** phase-1.5
- **Rationale:** Industry 30-day money-back → +21% sales; beta = free, post-rc1 GA needs policy lock.
- **Acceptance:** policy doc + TOS reference + ops process refund handling
- **Owner:** future Wave 88+

---

## 7. Verdict — Wave 86 competitive position pre-rc1

**Overall:** ⚠️ **AT-RISK** — Wave 86 inside-out scope MISS 1 P0 BLOCKING (cookie consent PDPL) + 2 P1 (email expiry spec + cohort retention framework) + 3 P2 (SLA published / wizard step audit / Most Popular badge). Tag `v1.0.0-rc.1` không nên ship trước khi resolve P0 + P1 gaps.

**Top 3 benchmark gaps cần address pre-rc1:**

1. **🚨 P0 — Cookie consent banner PDPL Decree 13** — compliance + first-impression damage; add Bucket E.6 verification
2. **⚠️ P1 — Email verification + password reset link expiry policy** — security + UX gap; add Bucket E.7 spec
3. **⚠️ P1 — Cohort retention D7/D14/D30 tracking framework** — 70% churn window industry; add Bucket H.5

**Competitive position trong VN edu SaaS:** match-or-above industry baseline cho pricing public + 2FA Phase 1 + onboarding wizard intent (≤7 steps verify pending) + status page (Wave 84 ship); **below industry cho cookie consent + email expiry + cohort retention framework + explicit SLA**. Path tới ≥80 quality score pre-rc1 = 3-5h work (Bucket E.6/E.7 + Bucket G.5 + Bucket H.5/H.6 same wave).

**Recommendation:** Wave 86 absorbed 6 new sub-items (E.6, E.7, G.5, H.5, H.6 + verify wizard step count Bucket C). Effort delta +3-5h on top of existing 14-20h estimate → still within wave budget.

---

## 8. References

- Wave 86 plan: `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md`
- `outside-in-coverage-trigger.md` v1.0.0
- `dev-readable-doc-language.md` v1.0.1
- `pre-launch-auth-hardening-checklist.md` Cat 4
- `pre-launch-secrets-hardening-checklist.md` Cat 2
- `output-review-mandate.md` §3 (security baseline + ops readiness rows)
- `user-manual-content-standard.md` §2 row 5 (Zalo OA defer Phase 1.5+)

### External sources (WebSearch)

- [SaaS Conversion Rate Benchmarks 2026 — Artisan Strategies](https://www.artisangrowthstrategies.com/blog/saas-conversion-rate-benchmarks-2026-data-1200-companies)
- [Free Trial Conversion Rate Benchmarks 2026 — Kirro](https://kirro.io/free-trial-conversion-rate)
- [Vietnam EdTech 2025 Opportunities — JDI Group](https://jdi.group/vietnam-edtech-opportunities-for-business/)
- [B2B SaaS Benchmarks 2026 — Data-Mania](https://www.data-mania.com/blog/b2b-saas-benchmarks-2026-annual-report/)
- [Customer Onboarding Metrics — Dock](https://www.dock.us/library/customer-onboarding-metrics)
- [Vietnam Personal Data Protection Law — CookieYes](https://www.cookieyes.com/blog/vietnam-personal-data-protection-law/)
- [PDPL Vietnam Cookie Consent — CookieHub](https://www.cookiehub.com/pdpl-vietnam)
- [Email Verification Flows — Medium @AlexanderObregon](https://medium.com/@AlexanderObregon/email-verification-flows-with-spring-boot-and-expiring-tokens-e9b2a238d917)
- [Magic Links Implementation — SuperTokens](https://supertokens.com/blog/magiclinks)
- [B2B SaaS Pricing Models — AI-Bees](https://www.ai-bees.io/post/saas-pricing-models)
- [SaaS Pricing Models & Psychological Hacks — Cobloom](https://www.cobloom.com/blog/saas-pricing-models)
- [Atlassian Statuspage](https://www.atlassian.com/software/statuspage)
- [SaaS Beta Program Management 2026 — US Tech Automations](https://ustechautomations.com/resources/blog/saas-beta-program-management-automation-how-to-2026)
- [B2B Customer Service Response Time Benchmarks 2025 — Thena](https://www.thena.ai/post/b2b-customer-support-response-time-benchmarks)
- [Zalo for Business Guide — Infobip](https://www.infobip.com/blog/zalo-business)
- [SaaS Onboarding Benchmarks 2026 — ProductGrowth](https://productgrowth.in/insights/saas/saas-onboarding-benchmarks/)
- [Customer Onboarding Best Practices SaaS 2026 — Arcade](https://www.arcade.software/post/customer-onboarding-best-practices)
- [Optimize SaaS User Onboarding — Chameleon](https://www.chameleon.io/blog/optimize-saas-user-onboarding)
- [SaaSter Refund Policy Guide](https://www.saastr.com/good-refund-policy-saas-product/)
- [SaaS Refund Policy Build Trust — PayProGlobal](https://payproglobal.com/how-to/set-up-saas-refund-policy/)
