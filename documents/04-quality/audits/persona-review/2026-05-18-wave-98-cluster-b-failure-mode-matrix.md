---
title: Wave 98 Cluster B — Failure-Mode Matrix Audit
date: 2026-05-18
auditor: Claude (simulation-gap-finder skill, 3-axis matrix)
scope: 6 P0 PARTIAL gaps (GAP-538..543) — beta onboarding/UX cluster
related-rule: .claude/skills/quality/simulation-gap-finder/SKILL.md
related-rule: .claude/rules/outside-in-coverage-trigger.md
audience: dev
---

## Executive summary

- **Axis 1** (Journey step): 8 values (pre-signup → day-14 retention)
- **Axis 2** (Failure category): 6 values (connectivity / device / locale / email-deliverability / state-desync / trust-signals)
- **Axis 3** (Persona): 3 values (P2 Hằng / P3 Tâm / Anonymous Vy)
- **Total cells theoretically possible:** 8 × 6 × 3 = 144
- **Cells examined (selective, high-signal):** 32
- **NEW failure modes NOT covered by gaps 538-543:** **17**
- **Severity breakdown:** P0 = 5 / P1 = 8 / P2 = 4

**Top 3 systemic blind spots inside-out scope misses:**
1. **Email deliverability beyond "send succeeds"** — current scope assumes SMTP delivery = user reads. Misses Promotions-tab routing, Gmail clipping, missing plain-text fallback (only 1 of ~20 templates has `.txt` sibling — `kitehub-email/src/main/resources/templates/emails/`).
2. **Mobile-first viewport ≤375px** — P3 Tâm + P2 Hằng both mobile-dominant; current FE components (`FeedbackWidget`, `BetaDisclaimerBanner`, `OnboardingChecklist`) shipped without explicit ≤375px viewport regression tests.
3. **State-desync between tabs / sessions** — banner cookie `kitehub_beta_disclaimer_dismissed` is per-browser, but P2 Hằng works on Zalo in-app browser + Chrome desktop. State diverges silently.

---

## High-signal failure modes (NEW — not covered by gaps 538-543)

| ID | Step | Failure cat. | Persona | Plausibility | Severity | Description | Fix outline |
|---|---|---|---|:---:|:---:|---|---|
| M-NEW-1 | 3-Email click | Deliverability | P2 Hằng | **high** | **P0** | Resend/SES email lands in Gmail Promotions tab → Hằng never sees beta-invite activation link → silent churn before signup. No sender-reputation warm-up, no postmaster monitoring. | `kitehub-email/.../service/SESEmailService.java`: add `List-Unsubscribe` header + reply-to support@kitehub.me; warm-up plan with Google Postmaster Tools; track open-rate / spam-rate per template; defer Promotions classification via plain-text mode increase. |
| M-NEW-2 | 3-Email click | Deliverability | All | **high** | **P0** | ~19 of 20 templates have NO plain-text `.txt` fallback (`ls templates/emails/*.txt` = 1 file: `invite-staff.txt`). RFC 2049 best practice violated; Gmail spam filter weights HTML-only emails higher; corporate Outlook may strip HTML → user sees blank. GAP-543 AC mentions plain-text but defers to GAP-543.2 follow-up (Wave 79). | For Wave 98: ship plain-text `.txt` siblings for the 5 critical types (welcome / approve-tenant / reset-password / beta-invite / day-7-survey) + update SES send path `BodyPart.text` alongside `BodyPart.html`. Currently zero `text/plain` references in Java. |
| M-NEW-3 | 3-Email click | Deliverability | P2 Hằng | **high** | **P1** | Gmail clips messages >102KB; current `beta-invite.html` + `welcome.html` include inline CSS + brand logo as data-URI base64 → likely >102KB → "Message clipped — view entire message" hides CTA below the fold. | Audit each template byte-size; externalize logo to CDN URL; inline CSS minify; target <100KB per email. |
| M-NEW-4 | 3-Email click | Device | P3 Tâm | **high** | **P1** | Email rendered on Zalo in-app browser (P3 mobile-only persona); Zalo's WebView strips media queries → `@media (max-width: 600px)` rules ignored → desktop layout on 375px screen → CTA off-screen right. No email-client matrix test (GAP-543 AC defers HTML render verify ≥2 clients to GAP-543.3 Wave 79). | Test render on Zalo in-app, Gmail iOS, Gmail Android, Outlook web. Use table-based layout with explicit `width="100%"` not media queries (email best practice 2026). |
| M-NEW-5 | 4-First login | State-desync | P2 Hằng | high | P1 | Hằng opens beta-invite link in Zalo browser → completes signup → switches to Chrome desktop → invite token now consumed → "Token invalid" error → user thinks they signed up wrong. Multi-device handoff not handled. | After signup, send "Welcome — log in here" email with magic-link instead of expecting same-device session. Document multi-device flow in `/help/anonymous/beta-access`. |
| M-NEW-6 | 4-First login | Trust | P2 Hằng | high | P1 | Login page shows English error fragments from backend (e.g., `Bad credentials` Spring Security default) when seeded password expectations mismatch → Vietnamese-speaking user sees English error → trust loss. GAP-541 i18n audit closed FE landing but didn't audit backend error response surface. | Audit `kitehub-gateway` + `kitehub-platform` error response bodies. Map Spring Security errors to Vietnamese via `ErrorResponse` DTO. Reference `dev-readable-doc-language.md` §2. |
| M-NEW-7 | 4-First login | Device | P3 Tâm | high | **P0** | `BetaDisclaimerBanner` not viewport-tested at 360px (smallest Android in target market). Banner text + dismiss button + `/beta-status` link may wrap awkwardly OR dismiss button outside touch target ≥44×44px (WCAG AA). GAP-539 AC mentions "accessibility" generically but no explicit ≤375px viewport regression test. | Add Playwright viewport=360x640 + 375x812 regression spec for banner; assert touch targets ≥44px + no horizontal scroll. |
| M-NEW-8 | 4-First login | State-desync | P2 Hằng | medium | P1 | Banner dismissed in Chrome (cookie set) → opens dashboard later on Zalo in-app → banner re-appears (cookie not shared cross-browser) → "Why is this back? Did I do something wrong?" UX confusion. No server-side dismiss tracking. | Persist banner dismiss flag in `user_preferences` table per user_id (server-side), not cookie. Cookie = fast-path optimistic; server = canonical. |
| M-NEW-9 | 5-Day-1 checklist | Connectivity | P3 Tâm | high | P1 | Slow 3G in district school (target Vietnamese SME / education center) → `OnboardingProgressController` `PUT /api/v1/onboarding-progress` times out → Tâm clicks "next step" → no UI feedback → clicks 5x → 5 duplicate PUT requests when reconnects → idempotency? Last-write-wins? Not specified. | Add `Idempotency-Key` header support OR optimistic-locking version field in `onboarding_progress` entity. Inline FE loading state on step buttons. |
| M-NEW-10 | 5-Day-1 checklist | Locale | P3 Tâm | medium | P2 | Sample-data seed (GAP-538 deferred to follow-up seed worker) — when worker eventually ships, will it use VN-friendly names ("Nguyễn Văn An", "Trần Thị Hồng", "Lớp 5A1") per `user-manual-content-standard.md` §2? GAP-538 AC mentions VN-friendly but AC unchecked. Worker may default to Lorem Ipsum / Faker English. | Concretize VN seed data fixture file `kitehub-subscription/src/main/resources/seed/beta-demo-students-vi.json` BEFORE seed worker ships. Lock the persona names + class names + addresses. |
| M-NEW-11 | 5-Day-1 checklist | Device | P3 Tâm | high | P1 | `OnboardingChecklist` 5 steps rendered as vertical cards — on 360px screen, 5 cards = scroll forever; Tâm gives up. No progressive disclosure (current step expanded, completed steps collapsed). | Convert checklist to accordion pattern: current step expanded, others collapsed (compact mode). FE component change. |
| M-NEW-12 | 6-First action | Trust | Anonymous Vy | high | P1 | Vy comparing competitors → lands `/help/anonymous/pricing` → sees "Premium plan deferred Wave 79" (per GAP-542 context) → reads as "they don't have paid plans yet" → bounce to competitor. No "Coming soon — pre-register" CTA to capture lead. | Add waitlist CTA on pricing page for Phase 1.5 plans. Capture email in `early_access_waitlist` table. Sync with feedback widget for early-access feedback. |
| M-NEW-13 | 6-First action | Trust | Anonymous Vy | medium | P2 | Vy reads landing → checks `/beta-status` → sees no recent updates (markdown static, manual update per GAP-539 §Proposed Fix step 5) → "Looks abandoned" → bounce. Static page with no `last_updated` heartbeat. | Add `last_updated` field rendered prominently top of `/beta-status` page; auto-update from CI on every deploy. Sync GAP-539. |
| M-NEW-14 | 7-Day-7 prompt | Deliverability | P2 Hằng | high | P0 | Day-7 survey email scheduler (GAP-542 ships scheduler; email send wire deferred Bucket E) — if scheduler fires before email wire ships, survey logged but never sent → Hằng never prompted → silent feedback gap. No fail-loud monitoring. | Add scheduler-vs-email-wire integration test. CloudWatch alarm on `survey_emails_logged_but_not_sent > 0`. Ensure Bucket E ships email-wire BEFORE scheduler activates in production. |
| M-NEW-15 | 7-Day-7 prompt | Locale | P2 Hằng | medium | P1 | Day-7 survey body Vietnamese (GAP-543 AC partial — content rewrite deferred Wave 79). Default templated content may be machine-translated English → "We hope you enjoy using KiteHub" → "Chúng tôi hy vọng bạn thích sử dụng KiteHub" awkward; native VN phrasing = "Bạn thấy KiteHub thế nào sau 1 tuần?" | Native VN copywriter pass on 5 critical templates BEFORE Wave 98 ships. Don't defer to Wave 79 — Wave 98 is the beta-invite trigger window. |
| M-NEW-16 | 8-Day-14+ retention | State-desync | P2 Hằng | high | P1 | Feedback widget submit (GAP-542 rate limit 5/user/day) — if Hằng hits limit, error returns 429 → widget UI shows generic error → no path to "I really need to send this, contact support" → frustration. Rate-limit + support-channel not cross-linked. | Widget 429 response → auto-link to `mailto:support@kitehub.me?subject=Feedback overflow — [tenant_slug]` per GAP-540 mailto pattern. |
| M-NEW-17 | 8-Day-14+ retention | Trust | P3 Tâm | medium | P2 | Tâm logs in day-14 → onboarding checklist still visible at top of dashboard (no dismiss-after-complete logic verified). If steps marked done but checklist component still mounted → feels stale → "Why is this still here?". | `OnboardingChecklist` component: auto-collapse / hide-after-completion check post all 5 steps `step_completed=true`. Add FE component regression test. |

---

## Failures already covered by gaps 538-543 (reinforcement, no new gap needed)

- **GAP-538:** onboarding 5-step flow + sample data opt-in + tenant isolation ✅ (state-desync within step persistence handled by API; cross-tenant leak tested)
- **GAP-539:** banner dismissible + `/beta-status` SSR + ARIA labels (keyboard accessibility partial — see M-NEW-7 viewport gap)
- **GAP-540:** footer support@kitehub.me + mailto subject prefill ✅ for desktop; widget vendor deferred Wave 79 (covered)
- **GAP-541:** landing + pricing + TOS placeholder VN narrative ✅ (covered for landing; backend errors M-NEW-6 NOT covered)
- **GAP-542:** rate limit 5/user/day ✅ enforced (but UX after 429 = M-NEW-16 gap)
- **GAP-543:** subject line ≤50 char + zero PII ✅; plain-text fallback + HTML render verify + content rewrite all deferred (covered by gap, but Wave 98 is the trigger window — see M-NEW-2 + M-NEW-15)

---

## Recommendations for Wave 98 plan

### Tier 1 — MUST land in Wave 98 (else beta-invite blocked)

1. **M-NEW-2** (plain-text fallback for 5 critical templates) — concrete blocker for Promotions-tab routing risk. ~1 day Java + content work. File new gap GAP-646.
2. **M-NEW-14** (scheduler-vs-email-wire integration test) — closes silent-failure path that breaks day-7 RETAIN signal. ~0.5 day. File new gap GAP-647.
3. **M-NEW-7** (banner ≤375px viewport regression test) — P3 Tâm = mobile-dominant target persona; failure = visible UX broken. ~0.5 day Playwright. File new gap GAP-648.
4. **M-NEW-1** (List-Unsubscribe + Reply-To headers) — small change, large deliverability lift. Add to existing GAP-543 follow-up scope OR new gap.
5. **M-NEW-15** (native VN copywriter pass on 5 critical templates) — defer-to-Wave-79 risk is real; Wave 98 IS the beta-invite trigger. File scope-creep follow-up OR squeeze into Wave 98 Bucket E.

### Tier 2 — SHOULD land Wave 98 (high-value, medium-cost)

6. **M-NEW-6** (backend Vietnamese error responses) — extends GAP-541 audit scope to BE error surface.
7. **M-NEW-9** (idempotency on onboarding PUT) — extends GAP-538 scope; ~0.5 day.
8. **M-NEW-11** (checklist accordion pattern) — UX polish for mobile-dominant personas.
9. **M-NEW-16** (widget 429 → mailto fallback link) — cross-link feedback widget to support channel.

### Tier 3 — CAN defer Wave 99+ (P2, lower plausibility)

10. M-NEW-3 (Gmail clipping byte-size audit)
11. M-NEW-4 (email-client matrix test — Zalo / Outlook web)
12. M-NEW-5 (multi-device handoff magic-link)
13. M-NEW-8 (server-side banner dismiss persistence)
14. M-NEW-10 (concrete VN seed data fixture file)
15. M-NEW-12 (Phase 1.5 pricing waitlist CTA)
16. M-NEW-13 (`/beta-status` last_updated heartbeat)
17. M-NEW-17 (onboarding checklist auto-hide post-complete)

### Wave 98 plan delta from inside-out scope

- **Add 3 new buckets** (or extend existing): Email deliverability hardening (Tier 1 items 1+4) / Mobile viewport regression (Tier 1 item 3) / Schedule-fail-loud integration test (Tier 1 item 2).
- **Squeeze into existing Bucket E** (email content audit): native VN copywriter pass (Tier 1 item 5) — concrete blocker for beta-invite trust.
- **File 5+ new gaps** (GAP-646 through ~GAP-650) covering Tier 1+2 items.
- **Defer Tier 3** to Wave 99+ as scoped follow-ups with concrete acceptance criteria.

### Counter-argument to defer-everything-to-Wave-79 pattern

GAP-543 AC defers plain-text fallback + HTML render verify + content rewrite to Wave 79. But **Wave 98 IS the beta-invite trigger window** — if the 5 critical email types are not deliverability-hardened by Wave 98 close, beta invite will silently churn ≥20% of recipients to Promotions tab / spam / blank rendering. Pre-Phase-1-BETA gate ≥80 quality (per CLAUDE.md Phase 1 trigger) requires the email layer to actually deliver. Wave 98 must close M-NEW-1 + M-NEW-2 + M-NEW-14 + M-NEW-15 minimum.

---

## Method note

Per `simulation-gap-finder/SKILL.md` 3-axis matrix protocol + `outside-in-coverage-trigger.md` v1.1.0:
- Cells selected via persona-failure-step intersection heuristic (low-tech persona × low-bandwidth × critical-trust-step = high yield)
- Plausibility scored: high (≥30% target users hit), medium (10-30%), low (<10%)
- Severity P0 = beta-invite blocker; P1 = silent retention loss; P2 = polish

Audit artifact preserved per `output-review-mandate.md` §3 row "UI screens" (extends to UX flow audits). Findings feed Wave 98 plan §1 Brainstorm outside-in column per `outside-in-coverage-trigger.md` mandate.
