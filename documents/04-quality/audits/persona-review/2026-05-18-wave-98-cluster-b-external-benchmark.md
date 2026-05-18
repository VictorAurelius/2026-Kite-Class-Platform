---
title: Wave 98 Cluster B — External SaaS Benchmark Audit
date: 2026-05-18
auditor: Claude (external benchmark research agent)
scope: 6 P0 PARTIAL gaps (GAP-538..543) — beta cohort polish wave
method: WebSearch + WebFetch (8 queries, 0 deep code reads — pure external benchmark scope)
audience: dev
---

## Executive summary

- **References reviewed:** 9 distinct (Linear changelog + Linear releases banner, Notion onboarding survey + template, Intercom NPS survey lifecycle, MISA meInvoice (VN), KiotViet Zalo OA integration (VN), Haravan Zalo OA features (VN), Google Android Beta program, Vietnamese formal/informal email tone references)
- **Industry-standard patterns Kite missing:** **7** spread across GAP-538/539/540/542/543 (onboarding personalization survey, version chip + changelog link in banner, status page real-time component health, command-K help search, two-survey timing window not just day-7/14, plain-text email fallback parity, persona-tone split in welcome emails)
- **VN-specific divergence flags:** **5** (Zalo OA mandatory not just email; ZNS template approval workflow blocks survey delivery; PDPL banner consent vs GDPR cookie language; "Kính gửi" formal vs "Chào bạn" informal — wrong choice breaks trust with P2 Owner persona; Resend deliverability VN inboxes weaker than SES + DKIM)
- **NEW gap candidates surfaced:** **3** (B-NEW-1 onboarding persona survey, B-NEW-2 Zalo OA support channel parity, B-NEW-3 welcome email persona-tone split)

Cluster B inside-out scope is solid foundation — fixes table-stakes "you exist, here's the door" gaps. But it stops at table stakes. Industry leaders (Linear, Notion, Intercom) personalize + segment from message 1; VN incumbents (Misa, KiotViet) treat Zalo OA as default not afterthought. Wave 98 has 7-day window before invite — fold ≥3 of the 7 missing patterns into scope or explicitly defer to Wave 99 with rationale.

---

## Per-gap benchmark

### GAP-538 Day-1 onboarding checklist + sample/demo data seed (85% DONE)

**References:**
- **Notion** ([goodux.appcues.com/blog/notions-lightweight-onboarding](https://goodux.appcues.com/blog/notions-lightweight-onboarding), [candu.ai/blog/how-notion-crafts-a-personalized-onboarding-experience](https://www.candu.ai/blog/how-notion-crafts-a-personalized-onboarding-experience-6-lessons-to-guide-new-users)): personalized onboarding survey at signup → 5 templates chosen based on persona (student / personal / engineer / manager / etc.) → hands-on checklist "Type / for slash commands" inside actual editor (not separate page).
- **Notion SaaS onboarding template** ([notion.com/templates/saas-onboarding-checklist](https://www.notion.com/templates/saas-onboarding-checklist)): explicit step tracking, "tracks onboarding steps, ensures nothing gets missed."
- **Misa AMIS onboarding** ([misa.vn/en](https://www.misa.vn/en/)): Vietnamese SaaS standard = MISA Academy linked from signup → role-targeted video walkthroughs (Kế toán / Quản lý / Chủ doanh nghiệp).

**Industry pattern:** Three-layer onboarding — (1) **signup-time persona survey** ("Tôi là Chủ trung tâm / Quản lý / Giáo viên đơn lẻ") → (2) **personalized template/data seed** chosen by persona response (not generic 5-10 students) → (3) **hands-on contextual checklist** anchored inside the actual product surface, not a separate `/onboarding` route.

**Kite current:** 5-step linear checklist with opt-in IMPORT_DATA toggle at step 3. Generic sample data seed (deferred to follow-up worker). Single flow regardless of persona. Route lives at `/onboarding` separate from dashboard.

**Delta:**
1. **No persona survey at signup** → checklist content + seed data identical for P1 Solo Teacher (5 students max) vs P2 Center Owner (200 students realistic). Wrong sample data scale = user dismisses as "demo unrealistic."
2. **Generic seed (deferred)** vs Notion's persona-mapped templates → even when worker ships, content needs persona dimension.
3. **Contextual placement** — Notion teaches `/` slash commands inside the editor; Kite's checklist is on separate route → user opens, closes, never returns.

**VN consideration:** P2 Center Owner persona (chị Hằng) culturally expects "hướng dẫn cụ thể cho chủ trung tâm" not "1 quy trình chung." Misa AMIS Academy mapping per-role is the local benchmark. If Wave 98 ships checklist without persona dimension, P2 will read it as "made for someone else."

**Severity:** P1 (not P0 — current 5-step checklist works as MVP; persona dimension is enhancement)

---

### GAP-539 Beta disclaimer banner + /beta-status page (90% DONE)

**References:**
- **Linear changelog** ([linear.app/changelog](https://linear.app/changelog), [linear.app/changelog/2026-04-30-releases](https://linear.app/changelog/2026-04-30-releases)): every changelog entry has date + version tag + author + clear "what's new" + sometimes "preview" / "beta" chip on entries not yet GA. Changelog itself is the canonical "current state" page — no separate `/beta-status` needed for product state.
- **Vena Solutions** ([venasolutions.com/beta-release-disclaimer](https://www.venasolutions.com/beta-release-disclaimer)): formal "Beta Release Disclaimer" page enumerating: as-is, no warranty, data may be reset, support not guaranteed, feedback welcome. Legal-formal tone separates from marketing.
- **Google Android Beta** ([google.com/android/beta](https://www.google.com/android/beta)): explicit "Beta versions are pre-release. Features may break. Don't use on primary device." + opt-out mechanism prominent.
- **Linear releases banner** ([linear.app/docs/releases](https://linear.app/docs/releases)): shared issues now show banner indicating visibility scope (2026-02 changelog) — proves banner pattern is canonical for "ephemeral context user needs to know."

**Industry pattern:**
- **Top-of-page banner**: dismissable (cookie persist), versioned (`v0.9.0-beta` chip visible), with link to BOTH `/changelog` (what changed) AND `/status` (current health). Linear's chip + Vena's disclaimer are complementary — Kite has the disclaimer-banner-as-current-state but no version chip + no changelog link.
- **Status page**: industry distinguishes (a) **incident status** (Statuspage.io / Atlassian Statuspage — green/yellow/red component health, real-time) from (b) **product state** (changelog + known issues — manually curated). Kite's `/beta-status` is type (b) only.

**Kite current:** Dismissable banner (cookie versioned `kitehub_beta_disclaimer_dismissed`, 1y Max-Age) shows on `/onboarding` only (not yet dashboard-wide). `/beta-status` page is static markdown rendered SSR with 5-min cache from `beta-status.md`. Vietnamese narrative. No version chip. No `/changelog` route mentioned. No real-time component health (no Statuspage.io / no `/health` aggregator UI).

**Delta:**
1. **Missing version chip** in banner — user has no quick "what version is this?" signal. Industry default is `v0.9.0-beta` visible top-right.
2. **No `/changelog`** — banner says "Beta status" but if user wants "what changed last week?" no path exists. Linear treats changelog as primary; status as secondary.
3. **Static-only status** — `/beta-status` is manual edit + redeploy. If KiteHub has a P0 incident at 22:00 weekend, status doesn't update until human edits. Industry minimum = component-level health badge auto-fed by uptime monitor (e.g., aggregate `/actuator/health` across services).
4. **Banner not on dashboard yet** — gap notes "broader dashboard-layout integration deferred (Bucket A FE polish wave)." If Wave 98 is the polish wave, this should land now not deferred again.

**VN consideration:** PDPL banner consent semantics differ from GDPR cookie banner. PDPL Art 11-13 requires explicit consent for personal data processing — beta disclaimer banner is the right surface to also surface "Bằng cách tiếp tục sử dụng Beta, bạn đồng ý xử lý dữ liệu cá nhân theo PDPL" with link to Privacy. Currently no PDPL consent in banner copy → compliance gap on top of UX gap.

**Severity:** P0 — version chip + dashboard-wide mount are 1-day fixes that unlock significantly more trust signal.

---

### GAP-540 Beta support channel discoverability (80% DONE)

**References:**
- **Linear cmd-K help** ([linear.app/docs/releases](https://linear.app/docs/releases)): cmd-K palette → "Help" → contextual answers + escalate to support. Single entry point regardless of where user is stuck.
- **Intercom widget** ([intercom.com/help/en/articles/6068874-send-a-survey-on-your-mobile-app](https://www.intercom.com/help/en/articles/6068874-send-a-survey-on-your-mobile-app)): floating bottom-right widget, expand on click, threaded conversation + bot routing + escalate to human. Stripe + Vercel use same pattern.
- **KiotViet Zalo OA** ([kiotviet.vn/tich-hop-lien-ket-zalo-oa-de-gui-tin-nhan-zns-tren-phan-mem-quan-ly-ban-hang](https://www.kiotviet.vn/tich-hop-lien-ket-zalo-oa-de-gui-tin-nhan-zns-tren-phan-mem-quan-ly-ban-hang/)): VN SaaS standard = Zalo OA as primary 2-way channel + ZNS for transactional + email as fallback. KiotViet exposes Zalo OA in footer + help center.
- **Haravan support** ([support.haravan.com/support/solutions/articles/42000088086](https://support.haravan.com/support/solutions/articles/42000088086-c%C3%A1c-t%C3%ADnh-n%C4%83ng-h%E1%BB%97-tr%E1%BB%A3-tr%C3%AAn-zalo-oa)): explicit Zalo OA features for support — automated greetings + escalation + transcript history. Treated as first-class not afterthought.

**Industry pattern:**
- **Intl:** floating widget bottom-right (Crisp/Tawk/Intercom) + cmd-K palette + footer link + nav `?` button → ≥3 entry points minimum per persona.
- **VN edu/SaaS:** Zalo OA is **primary**, email + widget secondary. ~80% of VN SMB tenants prefer Zalo because admin/staff already use Zalo daily; email is "for invoices only."

**Kite current:** Plan = footer mailto + Help link + floating widget (vendor TBD Wave 79+). Email forwarding verify pending. CLAUDE.md mentions Zalo OA but "defer Phase 1.5+ if Zalo OA chưa active" per GAP-539 banner footer copy.

**Delta:**
1. **No Zalo OA in Cluster B scope** — defer to Phase 1.5 may be wrong. P2 Center Owner persona will treat email-only support as "Western SaaS, không hiểu khách Việt." Even minimal "Zalo OA coming soon, gọi điện trong khi chờ" is better than silence.
2. **No cmd-K palette** — Linear / Notion / Vercel all have it. Single keyboard shortcut from anywhere → search docs + contact support. Eliminates "where do I click for help?" friction. ~1-2 day FE add via library (kbar / cmdk).
3. **mailto: subject prefill** missing per AC — easy ship, no excuse to defer.
4. **support@kitehub.me forwarding verify ⚠️ pending** — this is blocking-level. If invite goes out and forwarding broken, every support email lost. Must verify before Wave 98 invite.

**VN consideration:** Zalo OA setup not just a "nice to have" — competitive table-stakes for VN edu SaaS. If genuinely can't ship Zalo OA Wave 98, the banner copy needs to be explicit: "Hỗ trợ trong Phase 1 Beta qua email support@kitehub.me; Zalo OA sẽ active trong Phase 1.5" so user doesn't feel ghosted.

**Severity:** P0 (forwarding verify) + P1 (Zalo OA roadmap signal) + P2 (cmd-K palette)

---

### GAP-541 Customer-facing Vietnamese i18n audit (60% DONE — landing 100% audited)

**References:**
- **Talkpal Vietnamese formal email** ([talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese](https://talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese/)): formal Vietnamese uses "Kính gửi [Title + Name]," "Trân trọng," avoids contractions. Used when relationship hierarchical or recipient unknown.
- **Travel With Languages email guide** ([travelwithlanguages.com/blog/write-email-or-letter-in-vietnamese](https://travelwithlanguages.com/blog/write-email-or-letter-in-vietnamese.html)): formal ↔ informal distinction is **critical** in VN business culture — wrong choice = "thiếu lịch sự" perception.
- **NativeX VN email structure** ([nativex.edu.vn/tu-hoc/cau-truc-viet-email-tieng-anh](https://nativex.edu.vn/tu-hoc/cau-truc-viet-email-tieng-anh/)): VN business email evaluated as personal-brand signal, not just message delivery.

**Industry pattern:**
- **Tone-split by persona/context**: B2B-to-Owner = formal "Kính gửi anh/chị," B2C-to-end-user = informal "Chào bạn." Mixing register = trust break.
- **Same-tier example: MISA meInvoice** ([meinvoice.vn/en/support](https://www.meinvoice.vn/en/support/)) emails to business customers consistently formal "Kính gửi Quý khách hàng" — never informal. Sets the bar for VN B2B SaaS.

**Kite current:** Landing + pricing + TOS placeholder + signup redirect 100% Vietnamese narrative, mixed-language with English tech tokens natural per `dev-readable-doc-language.md` §4. VND format via `Intl.NumberFormat('vi-VN')`. Dashboard banner + approval email subjects + bodies still pending (Bucket B + E sync). i18n library deferred per CLAUDE.md "EN deferred to GAP-182 Phase 2."

**Delta:**
1. **Tone not yet codified** — gap audits "Vietnamese coverage" but doesn't enforce "formal for P2 Owner emails, informal for P1 Solo Teacher emails." If welcome email uses "Chào bạn" to a 45-year-old Center Owner → trust break. This rolls into GAP-543 email audit.
2. **TOS placeholder content** — "v1 pending counsel review" per CLAUDE.md. Acceptable but the placeholder TEXT needs to be VN-natural, not literally "[v1 placeholder — pending counsel]." Verify the user-visible placeholder reads OK to a VN reader.
3. **Approval email** — likely English-leaning subject (e.g., "Your KiteHub account has been approved!"). Needs audit Bucket B sync.
4. **i18n library deferred** is fine for Phase 1 BETA (VN-only target), but flag for Phase 2 — re-evaluate when first international tenant signs.

**VN consideration:** Per `dev-readable-doc-language.md` codified mandate — narrative VN, identifiers EN, mixed sentences natural. Already aligned. The tone-by-persona dimension is the additive gap.

**Severity:** P1 — landing is solid; emails + tone-by-persona is the remaining work.

---

### GAP-542 Feedback channel — in-app widget + email survey day-7/14 (80% DONE)

**References:**
- **Intercom Surveys** ([intercom.com/blog/how-intercom-does-nps-surveys](https://www.intercom.com/blog/how-intercom-does-nps-surveys/), [userpilot.com/blog/intercom-nps-survey](https://userpilot.com/blog/intercom-nps-survey/)): NPS survey lifecycle — "First Seen = 0 days ago" for onboarding, CSAT after support closure, NPS 60 days before renewal, recurring NPS every 90 days. Multi-touch across lifecycle, not just day-7/14.
- **Intercom mobile surveys** ([intercom.com/help/en/articles/6068874](https://www.intercom.com/help/en/articles/6068874-send-a-survey-on-your-mobile-app)): in-app surveys triggered by audience rule (e.g., "completed onboarding 7 days ago" + persona segment).
- **Zonka Intercom alternatives** ([zonkafeedback.com/blog/survey-tools-for-intercom](https://www.zonkafeedback.com/blog/survey-tools-for-intercom)): standard SaaS NPS pattern = (1) **0-day** welcome survey "What brought you here?" → (2) **7-14 day** activation survey "Got value?" → (3) **30-day** retention survey "Why stay?" → (4) **60-day** NPS.

**Industry pattern:**
- Two-touch (day-7 + day-14) is **lower bound**. Mature SaaS extends to 4-touch lifecycle. Intercom default = activation (day-3-7) + value-realization (day-14-30) + NPS (day-60+).
- In-app widget = floating bottom-right OR top-right (separate position from support widget) with rating 1-5 + free-text + category dropdown. Submit → toast.
- **Tally** integration mentioned in gap is fine — but Tally has limit (free tier 100 submissions/month). For 5 beta tenants × 4 users × 2 surveys = 40 submissions/month → fine for Phase 1 BETA.

**Kite current:** Plan = floating widget + day-7/14 email scheduler. AC mostly unchecked. Email template wire deferred to Bucket E.

**Delta:**
1. **Two-touch may be too sparse** — for beta cohort specifically you want **earlier** signal too. Day-3 "first impression" survey + day-7 + day-14 + day-30 catches drop-off pattern earlier. Day-7 first signal means 7 days of churn risk hidden.
2. **Widget category dropdown** — gap mentions "category enum" but doesn't list values. Industry default: `Bug | Feature request | Compliment | Question | Other`. Without enum locked, free-text becomes triage burden.
3. **Rate limit 5/user/day** — fine. Stripe/Linear use 10/day. Lower bound OK for beta.
4. **No screenshot attach** — Linear / Vercel feedback widgets allow optional screenshot + console log capture. P2 Center Owner reporting "trang lỗi" without screenshot = useless. Even minimal "paste browser console log" textarea helps triage.

**VN consideration:** **Zalo Mini App survey** could be alternative delivery channel for VN. Many P2 Owners check Zalo more than email. Survey link to Zalo Mini App with rating buttons > email link to Tally. But Zalo Mini App = Wave 99+ scope, not Wave 98. For Wave 98, email + in-app is fine; flag Zalo as v2.

**Severity:** P1 — current 2-touch + widget is workable; day-3 add + screenshot capture are enhancements.

---

### GAP-543 Email content audit — 5 critical email types (40% DONE)

**References:**
- **DigiStorms SaaS welcome email** ([digistorms.ai/blog/saas-welcome-email](https://www.digistorms.ai/blog/saas-welcome-email)): "Welcome emails set the tone for your entire onboarding experience" — tone consistency across emails is the primary trust signal. Recommends single-CTA, scannable, persona-aware.
- **Vietnamese formal vs informal** ([talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese](https://talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese/), [travelwithlanguages.com/blog/write-email-or-letter-in-vietnamese](https://travelwithlanguages.com/blog/write-email-or-letter-in-vietnamese.html)): formal = "Kính gửi anh/chị," informal = "Chào bạn." VN business culture penalizes informal tone to authority figures (Owner, Manager).
- **MISA meInvoice support emails** ([helpv4.meinvoice.vn](https://helpv4.meinvoice.vn/)): consistently formal "Kính gửi Quý khách hàng," "Trân trọng" sign-off. Sets B2B VN bar.

**Industry pattern:**
- **HTML + plain-text fallback parity** mandatory per RFC 2049. Modern email clients (Gmail/Outlook) auto-degrade but spam-filter score worse without plain-text.
- **Subject line ≤50 char**, zero PII (user name in subject = spam-filter risk), persona-tone consistent.
- **Footer mandatory**: support contact + unsubscribe (if applicable per CAN-SPAM/PDPL) + physical address (CAN-SPAM US; not required VN but trust signal).
- **Persona tone split**: Owner/Manager = formal "Kính gửi anh/chị Hằng"; Solo Teacher = "Chào bạn Vy" can be acceptable for younger user persona. Single template = wrong for 2 audiences.

**Kite current:** 5 audit notes shipped (welcome / approve-tenant / reset-password / beta-invite / day-7-survey). 3 templates MISSING per gap. Plain-text fallback deferred. Content rewrite deferred to Wave 79.

**Delta:**
1. **3 templates missing** — `beta-invite` + `day-7-survey` + ?. If invite goes out Wave 98+ and beta-invite template doesn't exist, this is blocking. Verify which 3 missing — at least beta-invite MUST exist for Wave 98 invite to ship.
2. **Plain-text fallback deferred** — risk Gmail/Outlook treat as low-trust. For 5 beta tenants probably OK; but plain-text is 30-min copy-paste-strip-HTML per template, no excuse to fully defer.
3. **HTML render verify ≥2 email clients deferred** — Litmus / Email-on-Acid free tier exists. Even a manual "send to gmail.com + send to outlook.com test account" smoke check (15 min) catches the 80% rendering issues.
4. **Tone consistency cross-template** — audit notes per-template don't cross-check "welcome friendly vs approve-tenant formal vs reset-password technical = jarring switch." Need cross-template tone audit.
5. **No tone-by-persona split** — single template per email type, all personas same tone. Industry default = template + persona substitution at send time.

**VN consideration:**
- **Resend deliverability VN** historically weaker than AWS SES (Resend datacenter EU, longer hop to VN ISPs Viettel/VNPT/FPT spam filters; SES Singapore = closer regionally). If GAP-543 emails routed Resend, deliverability VN inbox may be 70-80% vs SES 85-95%. Verify via test send to gmail.com vi-VN locale before invite.
- **DKIM/SPF/DMARC** all-pass mandatory or VN ISPs aggressively spam-bin. `pre-handoff-self-test-completeness.md` §2.3 email-flow checklist should include "send to Gmail VN account + check Inbox not Spam" pre-Wave-98.

**Severity:** P0 — 40% DONE for the email-critical-path is the riskiest gap in cluster. 3 missing templates + no plain-text + no render verify = high probability of inbox-spam-bin or template-rendering breakage at first invite send.

---

## NEW gap candidates (industry norm absent from inside-out scope)

### B-NEW-1: Onboarding persona survey at signup
- **Industry reference:** Notion ([candu.ai/blog/how-notion-crafts-a-personalized-onboarding-experience](https://www.candu.ai/blog/how-notion-crafts-a-personalized-onboarding-experience-6-lessons-to-guide-new-users))
- **Severity:** P1 (Wave 99 candidate, NOT blocking Wave 98)
- **Fix outline:** Add 2-question survey at signup completion before `/onboarding` checklist appears: (1) "Tôi là: [Chủ trung tâm / Quản lý / Giáo viên đơn lẻ / Khác]" (2) "Số học viên dự kiến: [<20 / 20-50 / 50-200 / >200]". Persist to `tenant.metadata.persona_segment` + `tenant.metadata.scale_segment`. Use to personalize: checklist copy, sample data scale (5 vs 50 vs 200 students), email tone (formal Owner / informal Teacher).

### B-NEW-2: Zalo OA support channel parity with VN edu market
- **Industry reference:** KiotViet ([kiotviet.vn/tich-hop-lien-ket-zalo-oa](https://www.kiotviet.vn/tich-hop-lien-ket-zalo-oa-de-gui-tin-nhan-zns-tren-phan-mem-quan-ly-ban-hang/)), Haravan ([support.haravan.com/support/solutions/articles/42000088086](https://support.haravan.com/support/solutions/articles/42000088086-c%C3%A1c-t%C3%ADnh-n%C4%83ng-h%E1%BB%97-tr%E1%BB%A3-tr%C3%AAn-zalo-oa))
- **Severity:** P1 (signal-level for Wave 98 — at minimum land "Zalo OA coming Phase 1.5" copy in banner footer; full Zalo OA Wave 99-100)
- **Fix outline:** **Wave 98 minimal:** banner footer + email footer + footer support contact all say "Email support@kitehub.me hoặc Zalo (sắp ra mắt Phase 1.5)." **Wave 99 full:** create Zalo OA Business account (vendor process, can start now in parallel), wire OA to webhook → forward DM to support inbox, document setup in operations runbook.

### B-NEW-3: Welcome email persona-tone split (formal vs informal)
- **Industry reference:** MISA meInvoice formal pattern ([helpv4.meinvoice.vn](https://helpv4.meinvoice.vn/)) + Vietnamese culture references ([talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese](https://talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese/))
- **Severity:** P0 (block Wave 98 invite — sending wrong-tone welcome email to a 45-year-old Center Owner is a trust-break that's hard to recover from)
- **Fix outline:** During GAP-543 content rewrite, ship 2 variants of welcome + approve-tenant email per persona segment from B-NEW-1: `_formal.html` ("Kính gửi anh/chị {fullName}") for P2_CENTER_OWNER + P3_CENTER_MANAGER; `_informal.html` ("Chào bạn {firstName}") for P1_SOLO_TEACHER + ANONYMOUS_PROSPECT. Template selection by `tenant.persona_segment` at send time. If B-NEW-1 not shipped Wave 98, default ALL emails to formal — formal-to-young is mildly awkward but recoverable; informal-to-authority is trust-burning.

---

## Recommendations (prioritized for Wave 98 7-day window)

### Must-fix before invite (P0)
1. **GAP-540: Verify support@kitehub.me forwarding** — send test email, confirm reception. If broken, invite cannot ship.
2. **GAP-543: Confirm beta-invite template exists** — if 3 missing templates include `beta-invite`, MUST ship Wave 98. Verify which 3 are missing.
3. **GAP-543: Manual 2-client render check (15 min)** — send each of 5 templates to test gmail.com + outlook.com accounts, screenshot, verify HTML + plain-text render. Catches 80% of breakage at 30-min cost.
4. **GAP-543 + B-NEW-3: Default all emails to formal Vietnamese tone** for Wave 98 (defer persona-split logic to Wave 99 if B-NEW-1 not shipped). "Kính gửi anh/chị" + "Trân trọng" — safer default than informal.
5. **GAP-539: Add PDPL consent language to banner** ("Bằng cách tiếp tục, bạn đồng ý xử lý dữ liệu cá nhân theo PDPL [link]") — compliance + trust.

### Should-fix Wave 98 if scope allows (P1)
6. **GAP-539: Version chip in banner** (`v0.9.0-beta` top-right corner, ~2h FE work).
7. **GAP-539: Mount banner on dashboard layout** (gap notes deferred to "Bucket A FE polish wave" — this IS that wave). ~2h.
8. **GAP-540: Zalo OA "coming soon" copy** in footer + banner. ~30 min copywriting.
9. **GAP-540: mailto: subject prefill** with `tenant_slug`. ~30 min.

### Defer to Wave 99 with explicit rationale (P2)
10. B-NEW-1 persona survey at signup.
11. B-NEW-2 full Zalo OA integration (start vendor onboarding now, ship Wave 99).
12. GAP-538 persona-mapped sample data seed.
13. GAP-540 cmd-K palette.
14. GAP-542 day-3 + day-30 surveys (extend 2-touch to 4-touch lifecycle).
15. GAP-542 screenshot capture in feedback widget.

### Open question for user
- **GAP-540: floating chat widget vendor decision** still says "vendor TBD Wave 79+." If genuinely deferred, the banner / footer copy must reflect "support qua email only Phase 1; chat Phase 1.5" — silence reads as broken UX. Pick now: Crisp (free tier) / Tawk.to (free) / mailto-only (no widget) — and ship copy matching the choice.

---

## Cross-cluster references

- Inside-out scope (Cluster B 6 gaps GAP-538..543) ships RETAIN-layer foundation per Wave 78 Bucket B + E + F precedents.
- This audit covers ONLY external SaaS benchmark dimension. Pair with: (a) persona simulation agent (P1/P2/P3 walkthroughs of beta first-touch flow), (b) failure-mode matrix agent (what breaks when email forwarding down / Resend DKIM fail / widget vendor outage).
- Wave 98 plan PR should reference this audit in §1 Brainstorm Q3 (external benchmark findings) per `outside-in-coverage-trigger.md` Bước 5 synthesis pattern.

---

## Sources

- [Linear changelog](https://linear.app/changelog) — beta banner + version chip + changelog patterns
- [Linear releases doc](https://linear.app/docs/releases) — releases page convention
- [Linear changelog 2026-04-30 releases](https://linear.app/changelog/2026-04-30-releases) — banner for shared issue visibility
- [Notion onboarding (goodux.appcues.com)](https://goodux.appcues.com/blog/notions-lightweight-onboarding) — contextual checklist pattern
- [Notion SaaS onboarding template](https://www.notion.com/templates/saas-onboarding-checklist) — onboarding step tracking
- [Notion personalized onboarding (candu.ai)](https://www.candu.ai/blog/how-notion-crafts-a-personalized-onboarding-experience-6-lessons-to-guide-new-users) — persona survey at signup
- [Intercom NPS surveys lifecycle](https://www.intercom.com/blog/how-intercom-does-nps-surveys/) — multi-touch survey timing
- [Intercom mobile surveys](https://www.intercom.com/help/en/articles/6068874-send-a-survey-on-your-mobile-app) — audience rule trigger patterns
- [Intercom NPS userpilot review](https://userpilot.com/blog/intercom-nps-survey/) — survey lifecycle stages
- [Zonka NPS tools SaaS 2026](https://www.zonkafeedback.com/blog/nps-tools-for-saas) — industry NPS standard
- [Vena Beta Release Disclaimer](https://www.venasolutions.com/beta-release-disclaimer) — formal beta disclaimer template
- [Google Android Beta program](https://www.google.com/android/beta) — beta program opt-in/out + data-may-reset language
- [MISA meInvoice support](https://www.meinvoice.vn/en/support/) — VN B2B formal email tone benchmark
- [MISA meInvoice help](https://helpv4.meinvoice.vn/) — formal VN customer comms pattern
- [MISA Joint Stock](https://www.misa.vn/en/) — VN SaaS enterprise standard
- [DigiStorms SaaS welcome email](https://www.digistorms.ai/blog/saas-welcome-email) — welcome email tone sets entire onboarding
- [KiotViet Zalo OA integration](https://www.kiotviet.vn/tich-hop-lien-ket-zalo-oa-de-gui-tin-nhan-zns-tren-phan-mem-quan-ly-ban-hang/) — VN edu/SaaS Zalo OA pattern
- [KiotViet Zalo OA messaging](https://www.kiotviet.vn/huong-dan-su-dung-kiotviet/retail-cskh/tin-nhan-zalo-oa/) — Zalo OA as primary channel
- [Haravan Zalo OA features](https://support.haravan.com/support/solutions/articles/42000088086-c%C3%A1c-t%C3%ADnh-n%C4%83ng-h%E1%BB%97-tr%E1%BB%A3-tr%C3%AAn-zalo-oa) — VN support Zalo OA first-class pattern
- [Talkpal Vietnamese formal email](https://talkpal.ai/culture/how-do-you-write-a-formal-email-in-vietnamese/) — "Kính gửi" vs "Chào bạn" tone register
- [Travel With Languages VN email guide](https://travelwithlanguages.com/blog/write-email-or-letter-in-vietnamese.html) — formal/informal register critical in VN business
- [NativeX VN email structure](https://nativex.edu.vn/tu-hoc/cau-truc-viet-email-tieng-anh/) — email as personal-brand signal in VN culture
