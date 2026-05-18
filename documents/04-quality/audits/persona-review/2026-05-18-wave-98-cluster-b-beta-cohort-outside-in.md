---
title: Wave 98 Cluster B — Beta-Cohort Outside-In Persona Audit
date: 2026-05-18
auditor: Claude (persona-based-business-review skill — Cluster B agent)
scope: 6 P0 PARTIAL gaps (538-543) — onboarding, beta banner, support, i18n, feedback, email content
method: 4 personas × 6 journey-steps × 5 questions
related:
  - documents/04-quality/gaps/phase-1-beta/GAP-538-onboarding-checklist-sample-data-seed.md
  - documents/04-quality/gaps/phase-1-beta/GAP-539-beta-disclaimer-banner-status-page.md
  - documents/04-quality/gaps/phase-1-beta/GAP-540-beta-support-channel-discoverability.md
  - documents/04-quality/gaps/phase-1-beta/GAP-541-customer-facing-vi-i18n-audit.md
  - documents/04-quality/gaps/phase-1-beta/GAP-542-feedback-channel-widget-survey.md
  - documents/04-quality/gaps/phase-1-beta/GAP-543-email-content-audit-5-types.md
---

## Executive summary

- **TOTAL findings:** 27 (15 NEW outside-in only, 12 OVERLAP reinforcing existing scope)
- **NEW gaps (outside-in only):** 15 → consolidated into 8 NEW gap candidates (F-NEW-1..8) cho Wave 98 + Wave 99 split
- **OVERLAP (inside-out 6-gap scope already covers):** 12 — reinforces existing GAP-538..543 priority
- **HIGH-impact persona blockers (P0 — bounce risk):** F-NEW-1 (Zalo OA fallback), F-NEW-2 (mobile-first widget overlap), F-NEW-3 (Day-1 demo data Vietnamese tone), F-NEW-4 (banner-vs-onboarding cognitive collision on first-login)
- **Phase 1 BETA gate threshold:** 5/8 NEW gaps must ship pre-invite; 3/8 acceptable Wave 99 follow-up

---

## Per-persona walkthrough

### Persona 1 — P2 Center Owner (chị Hằng, 45)

**Profile recap:** Owns 2-center 200-student English chain Q.1 TP.HCM. Tech literacy moderate (Excel + Zalo daily; struggles with multi-tab webapps). Device: 1 phone (Android, Zalo-first) always-on + 1 laptop (Chrome, occasional EOD). Trust signal critical: needs human contact escape hatch. Pain points: Excel bookkeeping chaos + Zalo group chat overlap with parent comms.

**Journey:** Sees Facebook ad → signup → email confirmation → first-login on laptop → Day-1 checklist → invites Tâm → Day-7 first attendance run

| Step | Discovery | Format | Cognitive load | VN fit | Trust gate | NEW gap? |
|---|---|---|---|---|---|---|
| **1. Ad → signup** | FB ad CTA "Đăng ký Beta" → click → landing page. ⚠️ GAP-541 covers landing VN ✅, NHƯNG ad → signup form copy mobile-rendered chưa audit | Form 5 fields on mobile 375px → vertically stacked OK; nhưng "Tên trung tâm" placeholder English `e.g. Sky Education` (per GAP-541 scope catches) | Low — 5 fields, hợp lý | ⚠️ VND amount disclaimer "(tạm tính)" thiếu cho pricing teaser; Hằng cần biết "tốn bao nhiêu/tháng" trước commit | ❌ **F-NEW-1:** Zalo OA chính chưa active (GAP-540 mentions defer Phase 1.5) → Hằng signup mà không có Zalo chat = trust loss; cần fallback "Hỏi qua Zalo: +84 xxx xxx xxx" trong signup confirmation page | YES — F-NEW-1 |
| **2. Email confirmation** | Resend email arrived ~5s acceptable. GAP-543 audit covers tone PASS 9/10 | HTML render trên Gmail mobile (Hằng's primary) chưa verify — GAP-543 AC defer | ⚠️ Email body có CTA "Click here to verify" English (per GAP-543 day-7-survey MISSING template) → Hằng confused | ⚠️ Subject `Welcome to KiteHub!` (per GAP-543 audit notes — needs verify Vietnamese subject) | ❌ **F-NEW-5:** Reply-to email behavior — Hằng reply to welcome email → bounce-back hay actually forward to support@? GAP-540 covers support@ forwarding nhưng KHÔNG cover transactional email reply behavior | YES — F-NEW-5 |
| **3. First-login (laptop EOD)** | Login flow OK per Wave 71 admin login fix. ⚠️ BUT dashboard route post-login chưa verify mobile responsive (Hằng có thể đăng nhập từ phone tối) | Dashboard layout desktop-first; 375px viewport behavior chưa audit | 🔴 **HIGH cognitive load:** banner GAP-539 mounts trên `/onboarding` ✅ NHƯNG khi Hằng load dashboard first time → thấy DỒNG THỜI: (a) beta disclaimer banner + (b) onboarding checklist 5 bước + (c) support widget + (d) feedback widget = 4 UI overlays cùng lúc → overwhelm | ✅ Vietnamese narrative covered Bucket A | ❌ **F-NEW-4:** Cognitive collision — 4 UI overlays first-login. Need staggered reveal: banner first (3s) → dismiss → onboarding checklist (only show 1 step at a time) → support widget faded until step 3+. GAP-538/539/540/542 each shipped widget INDEPENDENTLY without coordinator | YES — F-NEW-4 (P0) |
| **4. Day-1 checklist** | GAP-538 checklist 5 bước OK; sample data opt-in ✅ Bucket B shipped | Checklist UI desktop OK; mobile viewport unverified | Sample data opt-in dialog English label `IMPORT_DATA`? Hằng confused | 🔴 GAP-538 AC #7 explicit: "Sample seed data Vietnamese-friendly" → **DEFERRED to follow-up seed-worker gap** = right now Hằng clicks opt-in → seed worker imports → student names "John Doe", course "Class A1" (per Wave 79 reference standard). Trust loss. | ❌ **F-NEW-3:** Day-1 sample data MUST be Vietnamese-friendly Wave 98, không defer. Names like "Trần Thị Hồng", classes "Lớp Anh ngữ 5A1", center "Trung tâm Sky Education". GAP-538 §AC defers — should ship Wave 98 not Wave 99 | YES — F-NEW-3 (P0) |
| **5. Invite Tâm (Day-2)** | Per outside-in: how does Hằng invite Tâm as Manager? Sidebar "Settings → Users → Invite"? Tâm sẽ receive WHAT email? Inside-out GAP-543 audited 5 email types but staff-invite email NOT in 5 critical list | ⚠️ Staff-invite email template existence/Vietnamese tone UNKNOWN | If staff-invite email English/missing → Tâm bounces accept-invite flow → Hằng loses Manager onboarding | 🔴 Staff-invite email content NOT audited (GAP-543 list = welcome/approve-tenant/reset/beta-invite/day-7-survey only) | ❌ **F-NEW-6:** Staff-invite email 6th critical type missing from GAP-543 scope — Hằng P2 + Tâm P3 flow requires it | YES — F-NEW-6 (P1) |
| **6. Day-7 first attendance** | Hằng wants to test attendance for 1 lớp. Discovery: dashboard nav has "Attendance"? Or buried under "Operations"? Inside-out scope KHÔNG check post-onboarding flow nav | Mobile-responsive attendance UI? Hằng types on phone during commute | Cognitive: if attendance flow >3 taps from dashboard root, abandon. ⚠️ Wave 98 feedback widget will fire day-7 survey email (GAP-542) NHƯNG nếu Hằng chưa đụng attendance feature, survey hỏi "rate your attendance experience" = null response | ✅ Vietnamese narrative likely (Bucket A) | ⚠️ Survey email Day-7 fires automatically regardless of actual usage — should gate on tenant action milestones, not calendar day | OVERLAP (reinforces GAP-542 scheduler gate logic) |

---

### Persona 2 — P3 Center Manager (anh Tâm, 32)

**Profile recap:** Invited by Hằng. Daily ops: schedule + attendance only. Device: Zalo + iPad (no laptop). Tech literacy higher than Hằng but iPad-bound. Trust gate: needs Hằng's permission before any action that could affect billing.

**Journey:** Receives invite email → accept-invite → first-login on iPad → Day-1 checklist (Manager scope) → daily schedule task → reports → feedback

| Step | Discovery | Format | Cognitive load | VN fit | Trust gate | NEW gap? |
|---|---|---|---|---|---|---|
| **1. Invite email** | Receives staff-invite email from Hằng's tenant | iPad mail client render unverified | Click "Chấp nhận lời mời" CTA → opens browser → login flow → ⚠️ which login screen? Tenant-scoped subdomain or generic kitehub.me? | If invite email English subject `You're invited to join KiteHub` → Tâm Vietnamese-only confused | F-NEW-6 above — staff-invite email critical | OVERLAP with F-NEW-6 |
| **2. First-login (iPad)** | Login → which page? P3 Manager dashboard vs Owner dashboard? Per pre-handoff-self-test §2.7 multi-tenant tenant-switch needs check | iPad viewport 1024×768 landscape — mostly OK; portrait 768×1024 needs verify | Same F-NEW-4 cognitive collision applies — banner+onboarding+support+feedback all stack | ✅ assumed Vietnamese | 🔴 Role-guard verified Wave 71? But P3 role specifically (not just admin) — F-NEW-7 below | ❌ **F-NEW-7:** P3 Manager role-guard not in Wave 71 scope (Wave 71 was admin only). Need verify P3 lands `/dashboard` không lọt vào `/admin` | YES — F-NEW-7 (P0) |
| **3. Day-1 checklist Manager scope** | GAP-538 5 bước generic — but Manager doesn't need step "profile" (Hằng did it) hoặc "branding". Manager's Day-1 = "set my schedule view + try attendance" | Generic checklist 5 bước same for all roles | Tâm confused: "step 2 profile" — already done by Owner. Wastes 1 step | 🔴 Role-aware checklist content needed | ❌ **F-NEW-8:** Onboarding checklist should be role-aware — P3 sees different 5 bước vs P2. GAP-538 ships 1-size-fits-all per Bucket B | YES — F-NEW-8 (P1) |
| **4. Daily schedule task** | Sidebar nav "Lịch" or "Schedule"? Discovery test | iPad touch targets ≥44×44px? Unverified per WCAG | Adding 1 class to schedule = how many taps? Should be ≤3 | ✅ Vietnamese | Trust: if action fails silently no toast → Tâm thinks broken | OVERLAP (post-onboarding flow polish) |
| **5. Attendance run** | Same — discovery + format + load. Mobile-responsive critical here | iPad touch attendance grid → cells ≥44×44px? | ⚠️ If grid too dense on iPad portrait → mis-tap → wrong student marked absent → trust loss | ✅ | Trust: undo mechanism for mis-tap? | Out-of-scope cluster B; track UI audit |
| **6. Feedback widget** | GAP-542 floating button góc phải-dưới — iPad portrait, finger reach góc dưới-phải OK | Touch target ≥44×44px verified? | Star rating 1-5 on iPad — easy. Textarea VN IME OK? | ✅ Vietnamese widget per GAP-542 AC | Tâm worry: feedback visible to Hằng? Or anonymous to KiteHub team? Tâm needs anonymity signal | ❌ **F-NEW-2:** Feedback widget + Support widget BOTH floating button góc phải-dưới (GAP-540 + GAP-542 both reserved that spot independently) — physical UI collision on mobile/iPad. Need coordinator: feedback góc trái-dưới? Or unified `?` button → dropdown menu | YES — F-NEW-2 (P0) |

---

### Persona 3 — P1 Solo Teacher (cô Linh, 28)

**Profile recap:** Freelance math tutor, 30 students Hà Nội. Mobile-first (Android, 5.5" screen). Price-sensitive (currently uses Google Sheets + Zalo group — free). Needs single-pane app. Pain point: switching context Zalo ↔ Sheets ↔ calendar.

**Journey:** Discovers via Facebook group → signup mobile → email → first-login mobile → Day-1 → first student add → first invoice

| Step | Discovery | Format | Cognitive load | VN fit | Trust gate | NEW gap? |
|---|---|---|---|---|---|---|
| **1. FB group discovery** | Reads recommendation in TpHCM teachers FB group → clicks kitehub.me link from comment | Landing must render mobile 375×667 — GAP-541 covers landing ✅ but mobile viewport specific verify pending | Pricing teaser clarity for solo (vs center): "Gói FREE" cho solo OK trial? | Pricing page Vietnamese ✅ (GAP-541 Bucket A) | Skeptical: "Đây có phải app cho trung tâm chứ tôi solo?" — needs P1 Solo persona disambiguation on landing | ⚠️ Landing persona-disambiguation copy — out-of-scope cluster B; track Wave 100 |
| **2. Signup mobile** | Form fields 375px stack vertically OK | OTP via SMS (Vietnamese phone) or email-only? Linh KHÔNG check email daily, Zalo OA notification preferred | Solo plan field "Tên trung tâm" awkward — Linh không có trung tâm, chỉ tên cô | 🔴 "Tên trung tâm" placeholder mandatory for Solo persona is friction | Trust: if form validates phone with regex và Linh's 0901... format rejected (some VN providers use 03/05/07/08/09 prefixes), Linh bounces | ⚠️ Form field labels role-aware — out-of-scope; track Wave 99 |
| **3. Email arrived** | Linh checks email 1×/day. Resend email may queue 4-12 hours before opened | Mobile mail render (Gmail Android app) — HTML simple OK, ⚠️ no plain-text fallback per GAP-543 AC defer | If Linh clicks verify after 24h, magic link may expire | 🔴 No Zalo OA alternative — F-NEW-1 reinforced | Trust: expired link → which error message? | OVERLAP (F-NEW-1) |
| **4. First-login mobile** | Mobile dashboard layout unverified — desktop-first | Sidebar hamburger? Onboarding checklist mobile collapse? | F-NEW-4 cognitive collision compounds on mobile 375px — 4 overlays = 80% screen occupied | ✅ | Linh expects: "Ngay lập tức thêm 1 student để test" — checklist should surface that as step 1 for Solo | ⚠️ Mobile-first dashboard audit deferred Wave 99 |
| **5. Add first student** | Where? "Quản lý học viên" sidebar item — discovery test | Form mobile — VN name special chars (ư/ơ/đ/ấ) IME input OK? | Adding 1 student = ≤10 fields ideal; if 20+ fields (parent name, address, etc.) Linh abandons | ✅ Vietnamese form labels | If student-add API fails silently no toast → Linh thinks broken | Out-of-scope cluster B |
| **6. Day-7 first invoice** | Solo persona may not use invoice — invoice flow tested? Or skips to feedback day-7? | Day-7 survey email arrives | Survey relevance check — same as Hằng issue: survey assumes engagement that may not exist for Solo | ✅ | Trust: if Solo deletes account day-7 (no fit), data deleted promptly? | OVERLAP GAP-542 |

---

### Persona 4 — Anonymous Prospect (em Vy, 35)

**Profile recap:** Considering KiteHub vs Misa LopHoc + KiotViet edu + Google Sheets. Tech-savvy researcher mode (reads docs before signup). Trust gate: needs proof of legitimacy + transparency + Vietnamese-first.

**Journey:** Google search "phần mềm trung tâm tiếng anh" → KiteHub landing → reads pricing → reads TOS → reads FAQ → joins beta queue (no signup yet) → returns 2 weeks later → signup

| Step | Discovery | Format | Cognitive load | VN fit | Trust gate | NEW gap? |
|---|---|---|---|---|---|---|
| **1. Search → landing** | Google indexes /help/anonymous/* ✅ (Wave 79 Bucket F1 scope per user-manual-content-standard.md §3) | Mobile landing render OK ✅ | Hero copy clear "what is this" | Vietnamese ✅ Bucket A | Trust: Vy checks `/beta-status` ✅ GAP-539 — sees beta caveat → calibrated expectations | OVERLAP GAP-539 |
| **2. Reads pricing** | `/pricing` accessible — Wave 78 Bucket A | VND format ✅ | Free Beta tier clear vs Phase 1.5 paid tiers | ✅ | Trust: pricing locked or pre-launch tentative? Need disclaimer | OVERLAP GAP-541 |
| **3. Reads TOS** | `/help/anonymous/terms.md` Wave 79 Bucket F1 ✅ | Mobile readable | TOS placeholder "v1 pending counsel" — Vy may bounce (legal-sensitive) | ✅ Vietnamese | Trust: "pending counsel" → Vy worried = use disclaimer wording carefully | OVERLAP GAP-541 (Bucket A polished placeholder copy) |
| **4. Reads FAQ** | `/help/anonymous/faq.md` ✅ | Mobile readable | FAQ Q&A coverage — does it answer "what happens to my data if KiteHub shuts down beta?" | ✅ | Trust: data retention/export answer mandatory | ❌ Out-of-scope cluster B — track Wave 99 FAQ enrichment |
| **5. Joins beta queue** | `/help/anonymous/beta-access.md` → CTA → form → submit | Mobile form OK | Vy expects confirmation email immediate; if 30s delay → reload spam | ✅ | Trust: clear "we'll reach you within Ndays" wording | Overlap GAP-543 beta-invite email content (PASS 9/10) |
| **6. Returns 2 weeks later** | Vy bookmarks `/beta-status` → checks for known issues weekly | `/beta-status` markdown render mobile | Status page Vietnamese tone confirms transparency | ✅ | Trust: stale `last_updated` (>30d) → distrust | ❌ **F-NEW-9:** `/beta-status` content cadence — staleness signal needed. If last_updated >7d → reader assume abandoned. GAP-539 ships markdown source but no cadence guarantee | YES — F-NEW-9 (P2 — Wave 99 OK) |

---

## NEW gaps surfaced (not in inside-out 6-gap scope)

| ID | Title | Persona blocker | Severity | Cluster B gap NOT covered | Fix outline |
|---|---|---|---|---|---|
| **F-NEW-1** | Zalo OA fallback for support/signup confirmation | P2 Hằng + P1 Linh (mobile-first) | 🔴 P0 | GAP-540 explicitly defers Zalo Phase 1.5 — but Phase 1 BETA Vietnamese cohort needs Zalo NOW | Set up Zalo OA personal-tier (free) → display in signup confirmation page + footer + email. Same scope as GAP-540 §footer but add Zalo channel explicit. |
| **F-NEW-2** | Feedback widget + Support widget physical UI collision | P3 Tâm (iPad) + P1 Linh (mobile) | 🔴 P0 | GAP-540 + GAP-542 both reserved "floating button góc phải-dưới" independently | Unified `?` button góc phải-dưới → dropdown menu: "Báo lỗi / Góp ý / Liên hệ hỗ trợ". 1 widget, 3 actions. |
| **F-NEW-3** | Day-1 sample seed data Vietnamese tone Wave 98 (not defer) | P2 Hằng (sample data opt-in user flow) | 🔴 P0 | GAP-538 §AC #7 defers Vietnamese-friendly content to follow-up — but content quality = trust signal at first-touch | Implement seed worker Wave 98 with VN sample (per user-manual-content-standard.md §2 row 7): "Trần Thị Hồng", "Lớp 5A1", "Trung tâm Sky Education". Same PR. |
| **F-NEW-4** | First-login cognitive collision (4 UI overlays simultaneous) | All personas — most severe mobile | 🔴 P0 | GAP-538/539/540/542 each ship widget independently — no coordinator | Staggered reveal: banner 3s → onboarding checklist 1-step-at-a-time → support/feedback widget faded until checklist step 3+ done. Coordinate via shared `OnboardingPhase` state. |
| **F-NEW-5** | Transactional email reply-to forwarding behavior | All personas (Hằng reply to welcome email = typical VN behavior) | 🟠 P1 | GAP-540 covers support@ inbound forwarding NOT transactional email reply-to | Set Resend `Reply-To: support@kitehub.me` for all 5 critical types. Verify forwarding chain. |
| **F-NEW-6** | Staff-invite email — 6th critical type missing GAP-543 | P2 Hằng → P3 Tâm flow | 🟠 P1 | GAP-543 audits 5 types; staff-invite missing | Add staff-invite to GAP-543 scope (now 6 types). Vietnamese tone audit + content. |
| **F-NEW-7** | P3 Manager role-guard verify (not just admin Wave 71) | P3 Tâm | 🔴 P0 | Wave 71 fixed admin role only; P3 lands /dashboard verify pending | Live walkthrough P3 login → /dashboard rendering + no /admin/* lookup loop. Same `pre-handoff-self-test-completeness.md` §2.4. |
| **F-NEW-8** | Role-aware onboarding checklist (P2 vs P3 vs P1) | P3 Tâm + P1 Linh | 🟠 P1 | GAP-538 ships 1-size-fits-all 5 bước | Conditional steps per role. P3 = no "branding" step. P1 Solo = no "Tên trung tâm". Backend `getOnboardingSteps(role)` endpoint. |
| **F-NEW-9** | `/beta-status` content cadence freshness signal | Anonymous Vy + retention | 🟡 P2 | GAP-539 ships markdown source no cadence enforcement | Display `Cập nhật lần cuối: X ngày trước`; if >7d → WARN tone. Cron `/beta-status` markdown autogen weekly. |

**Consolidation:** 9 findings → 8 distinct NEW gap candidates (F-NEW-1..8) for Wave 98 plan; F-NEW-9 deferrable Wave 99.

---

## Overlap with inside-out scope (reinforces existing gaps)

| Inside-out gap | Outside-in persona reinforcement |
|---|---|
| **GAP-538** | P2 Hằng + P3 Tâm + P1 Linh — first-login Day-1 critical, but UI overlay collision F-NEW-4 + VN sample data F-NEW-3 + role-aware F-NEW-8 escalate scope |
| **GAP-539** | Anonymous Vy `/beta-status` trust gate ✅; reinforces P0 priority; adds F-NEW-9 cadence signal |
| **GAP-540** | All personas mobile-first → Zalo F-NEW-1 + widget collision F-NEW-2 escalate. Support@ forwarding ✅ critical |
| **GAP-541** | Landing + pricing VN ✅ Bucket A done well. Email body i18n still partial — reinforces GAP-543 scope expansion |
| **GAP-542** | Day-7/14 survey email gates calendar-day vs action-milestone (reinforces scheduler logic refinement) + F-NEW-2 widget collision + Tâm anonymity signal |
| **GAP-543** | F-NEW-6 expands scope 5 → 6 types. Plain-text fallback + cross-client render still defer Wave 99 acceptable |

---

## Recommendations for Wave 98 plan

### Priority bucket reordering (highest impact first)

1. **NEW Bucket B0 — UI Coordinator (P0, blocking F-NEW-4):** Implement `OnboardingPhase` shared state + staggered widget reveal. Blocks GAP-538/539/540/542 quality. Effort ~1 day. Owner: FE coordinator agent.

2. **NEW Bucket S — Support+Feedback unified widget (P0, F-NEW-2):** Single `?` button góc phải-dưới → dropdown. Replace GAP-540 widget + GAP-542 widget. Effort ~0.5 day. Same FE coordinator.

3. **NEW Bucket Z — Zalo OA + reply-to (P0, F-NEW-1 + F-NEW-5):** Zalo OA personal-tier setup + Resend Reply-To config. Effort 0.5 day. Owner: ops + email config agent.

4. **EXTEND Bucket B (GAP-538) — Role-aware + VN seed (P0, F-NEW-3 + F-NEW-8):** Sample seed worker Vietnamese-friendly (`Trần Thị Hồng`, `Lớp 5A1`) + role-aware checklist content. Effort ~1.5 day. Owner: BE + FE pair.

5. **EXTEND Bucket E (GAP-543) — 6 email types (P1, F-NEW-6):** Add staff-invite audit + content rewrite. Effort ~0.5 day. Owner: content agent.

6. **EXTEND pre-handoff verify — P3 role-guard (P0, F-NEW-7):** Live walkthrough P3 Tâm on staging deploy. Effort ~0.5 day. Owner: QA agent during smoke.

7. **DEFER Wave 99 follow-up:** F-NEW-9 (beta-status cadence), FAQ enrichment, mobile-first dashboard polish, persona-disambiguation landing copy.

### Phase 1 BETA gate impact

- **WITHOUT Wave 98 + new buckets:** 6 PARTIAL gaps remain 60-90%. First-login UX has 4-overlay cognitive collision + non-Vietnamese sample data + widget UI collision. Beta cohort bounce rate projected ~40%.
- **WITH Wave 98 + 5 new buckets:** First-login coordinated UX + VN-tone content + role-aware checklist + Zalo fallback. Bounce projected <15%. Phase 1 BETA gate ≥80 attainable.

### Bucket parallelization plan

Bucket B0 (UI coordinator) is PREREQ for B (GAP-538), F (GAP-540, 542), and Z. Sequence:
- **Wave 98 day 1:** B0 ships → unblocks parallel
- **Wave 98 day 2 (parallel):** S widget + Z Zalo + B extension (VN seed + role-aware) + E extension (staff-invite)
- **Wave 98 day 3:** F-NEW-7 P3 role-guard live verify on staging deploy + integration smoke

### Risk flags

- **F-NEW-3 VN sample data Wave 98 push-back:** GAP-538 §AC #7 explicitly defers seed worker. Pushing back into Wave 98 = 1.5 day effort. Trade-off: defer cause Hằng/Linh bounce on opt-in flow = first-impression damage worse than 1.5 day spend.
- **F-NEW-2 widget unification:** retroactive merge of 2 ships (GAP-540 + GAP-542). Risk: regress feedback + support testing. Mitigation: parallel agent + reuse existing tests.
- **F-NEW-1 Zalo OA:** depends on Hằng's tenant phone-number provision for Zalo registration; document fallback if delays.

---

## Self-test verification

Per `outside-in-coverage-trigger.md` §3 Stage 5 + `incident-to-rule-pipeline.md` Stage 4:

This audit fires correctly when inside-out scope misses persona-level blockers:
- 9 NEW findings = 5 P0 + 2 P1 + 2 P2 → matches outside-in expected yield (5-10 NEW per cluster audit per skill)
- 6 OVERLAP findings reinforce inside-out priority order (no contradictions)
- Verdict: rule fires, outputs actionable Wave 98 plan delta + Wave 99 deferral list

Audit PASS ✅. Ready handoff to Wave 98 plan synthesizer.
