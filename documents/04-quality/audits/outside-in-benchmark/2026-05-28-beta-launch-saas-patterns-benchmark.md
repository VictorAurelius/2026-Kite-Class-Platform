---
audience: dev
date: 2026-05-28
audit-type: outside-in-benchmark
session-theme: SaaS BETA-launch patterns benchmark cho Kite Platform Plan D refinement
companies-benchmarked: 7 (3 Western + 2 VN + 2 vertical-similar)
trigger-context: Wave meta-6 Bucket A walk surfaced 17 bugs trong shipped-DONE feature + retro audit 50% Wave 80+ features lack runtime walk evidence + user mất niềm tin về beta full-flow pass
related-rule: .claude/rules/feature-ship-runtime-walk-mandate.md v1.0.0
related-audits:
  - documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md
  - documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md
plan-d-baseline: "1-3 friend beta ~3 weeks close-loop"
output-format: outside-in-benchmark report
priority: P0 (Phase 2 BETA scope-shaping input)
---

# External Benchmark — SaaS BETA-launch patterns cho Kite Platform Plan D

## Tóm lược điều hành (TL;DR)

Benchmark 7 công ty (Stripe / Linear / Notion / Superhuman / MISA / ELSA Speak / KiotViet) qua 5 trục: close-loop cohort size, duration, scope at launch, feedback mechanism, graduation criteria. **3 patterns chi phối:**

1. **Concierge installation pattern** (Stripe "Collison Installation", Superhuman 30-min 1-on-1 onboarding) — founder TỰ install/onboard từng user, không gửi link rồi chờ. **Phù hợp cao với Kite Platform Plan D solo-dev mode** — 1-3 friends = installable bằng tay từng người.
2. **Tiny cohort + extreme high-touch + long duration** (Stripe 6 tháng iterate với 10-30 friends; Superhuman 30-min calls + 100 users/tuần cap). Cohort 1-3 friends THẤP hơn industry "20-50 beta users" recommendation NHƯNG vẫn hợp lý cho solo-dev pre-PMF stage — định nghĩa "nascent PMF" của First Round = 5-7 người sẵn sàng dùng + 1 sẵn sàng trả tiền.
3. **MVP slice không phải feature-complete** (Notion 2015 reset bỏ 4 nhân viên + scope; Stripe manual-merchant-setup backend ban đầu). Kite hiện carry 46 Wave 80+ DONE features nhưng 50% chưa walk → Plan D NÊN slice xuống MVP path (Owner signup → invite 1 staff → 1 class CRUD → 1 invoice = critical path) thay vì cố verify all 46.

**Top 3 anti-patterns cần tránh:**
1. **Premature scaling khi PMF chưa có** — Quibi $1.75B ship trước khi validate; Kite không có vốn-burn rủi ro nhưng có thời-gian-burn nếu scale public beta trước close-loop close.
2. **Beta link gửi rồi chờ feedback** ("Will you try our beta? Great, we'll send link") — Paul Graham anti-pattern Stripe tránh được.
3. **Ship trên scope chưa walked + audit pass nhầm** — đây CHÍNH LÀ pattern Wave meta-6 đã hit (trust-pass recurrence ≥7), Kite cần đặt RST walk làm gate-keeper bắt buộc.

**Fastest credible timeline từ trạng thái hiện tại đến close-loop beta:**
- (a) **2-3 tuần** đến close-loop 1-3 friends beta — nhưng PHẢI shrink scope MVP slice + walk 100% critical path TRƯỚC khi invite (eliminate trust-pass class)
- (b) **+6-10 tuần** thêm để expand 10 paying users (full audit suite + bugs from close-loop fixed)
- (c) **+12-20 tuần** thêm để GA — phụ thuộc PDPL deadline 2026-07-01 + counsel engagement Phase 2

---

## §1. Companies benchmarked — playbook details

### 1.1 Stripe (Western — fintech B2B)

**Cohort identification:** Y Combinator alumni network — first 10-30 customers từ YC friends ecosystem.

**Cohort size:** ~10-30 friends trong vài tháng đầu; iterate aggressively trước khi public launch.

**Duration close-loop:** ~6 tháng iterating prototype TRƯỚC khi mở rộng. "Over the next 6 months, they showed their API to friends, watched people interact with it, and iterated as fast as they could." ([Just Go Grind — The First Few: Stripe](https://www.justgogrind.com/p/the-first-few-stripe))

**Feature scope at launch:** **MVP slice extreme** — backend chạy MANUAL: "When someone signed up to Stripe, Patrick would call up his friend who would then actually manually set up a merchant account for that user." Frontend là API; backend là Patrick gọi điện thoại. Manual = product market fit signal trước infra investment. ([How They Grow — Stripe](https://www.howtheygrow.co/p/how-stripe-grows))

**Bug feedback mechanism:** **Collison Installation** — physically setup Stripe trên laptop của founder bạn bè. Paul Graham described: "Diffident founders ask 'Will you try our beta?' and if the answer is yes, they say 'Great, we'll send you a link.' But the Collison brothers weren't going to wait." ([First Round Review — Stripe](https://review.firstround.com/articles/stripe/))

**Graduation criteria:** Word-of-mouth viral — "developers told their friends, spreading through word of mouth because everything else was so bad and painful to work with that people actually were selling this to their friends." Khi friends-of-friends bắt đầu reach out + onboard self-serve → expanded beta.

---

### 1.2 Superhuman (Western — email B2C/B2B prosumer)

**Cohort identification:** Waitlist + survey screening — only "good fit" customers approved cho onboarding.

**Cohort size:** Waitlist 180K+ nhưng access throttled — **founder Rahul Vohra hard-cap 100 new users/tuần**. ([Waitlister — Superhuman case study](https://waitlister.me/growth-hub/case-studies/superhuman))

**Duration close-loop:** Multi-year — founder Rahul đích thân onboard từng user trong 1-2 năm đầu. ([First Round Review — Superhuman Onboarding Playbook](https://review.firstround.com/superhuman-onboarding-playbook/))

**Feature scope at launch:** Email client — narrow scope (read + compose + shortcuts) NHƯNG quality bar cực cao (sub-100ms response time hard requirement).

**Bug feedback mechanism:** **30-minute 1-on-1 onboarding call** mandatory mọi user. Founder bay tới văn phòng user mang gift + dạy email faster. Feedback loop intimate.

**Graduation criteria:** "Sean Ellis PMF survey" — 40% users would be "very disappointed" if product gone = PMF signal. Khi cross threshold → relax 100/week cap.

---

### 1.3 Notion (Western — productivity B2C → B2B)

**Cohort identification:** Friends + Product Hunt community (post-2018 relaunch).

**Cohort size:** Failed-then-restart pattern. **Original 2013 beta:** 4 employees + ~$2M raised + ~2 năm building "no-code programming tool" → users didn't want it. **2015 reset:** layoff toàn bộ team, founders move sang Kyoto, rebuild from scratch. ([Lenny's Newsletter — Inside Notion: Ivan Zhao](https://www.lennysnewsletter.com/p/inside-notion-ivan-zhao))

**Duration close-loop:** **3+ năm rebuild silently** before relaunch 2018 Notion 1.0 trên Product Hunt → Product of the Day/Week/Month.

**Feature scope at launch:** **Aggressive scope reduction** — original "all-in-one no-code tool" thất bại; relaunch focused "notes + tasks + wikis + databases trong 1 workspace" — clearer value prop.

**Bug feedback mechanism:** Product Hunt comment thread 150+ — founders engaged personally reading mọi feedback. Community Discord later.

**Graduation criteria:** Product Hunt traction signal — Product of the Day/Week/Month → expanded outreach. Then community-led growth (templates ambassadors).

---

### 1.4 MISA (Vietnam — accounting/ERP B2B SME)

**Cohort identification:** Government + SOE relationships first (B2G), expand SME later.

**Cohort size:** Hiện 60,000+ government clients + 120,000+ enterprise clients sau ~30 năm operation; specific beta cohort không public. ([MISA Vietnam](https://www.misa.vn/en/))

**Duration close-loop:** Multi-decade — MISA founded 1994; SaaS pivot (MISA AMIS) ~2017-2019. **15-day free trial** = current SaaS onboarding standard.

**Feature scope at launch:** Single product (accounting) → expand vertical (ERP / e-invoice / digital signature / banking integration ecosystem).

**Bug feedback mechanism:** "Dedicated guidance from consultants" trong free trial period — VN edu/SaaS context value high-touch onboarding rất mạnh; tương tự Superhuman concierge nhưng outsourced sang consultant team.

**Graduation criteria:** Bank partnership với Standard Chartered cho SME financing — graduation = enterprise partner integration, không phải metric tự thân. ([The Investor — Standard Chartered MISA](https://theinvestor.vn/standard-chartered-accounting-software-firm-misa-join-hands-to-finance-smes-d4547.html))

---

### 1.5 ELSA Speak (Vietnam — edtech B2C language learning với AI)

**Cohort identification:** Founder Vu Van leveraged native Vietnam network — bus drivers, boardroom executives, broad non-native English speakers.

**Cohort size:** Early stage 80-90% users từ Vietnam; specific beta cohort size không public; viral inflection point = 30,000 users trong 24 giờ post SXSW 2016. ([Vietcetera — Vu Van ELSA](https://vietcetera.com/en/vu-vans-journey-in-building-elsa-speak))

**Duration close-loop:** ~1-2 năm training AI model trên Vietnamese speakers TRƯỚC khi global expand. Vietnam = training data ground.

**Feature scope at launch:** Single use case — pronunciation feedback. Narrow + clear value prop.

**Bug feedback mechanism:** AI model trained on Vietnamese pronunciation patterns — feedback loop = corrections from native users improve model. Continuous learning.

**Graduation criteria:** **External validation event** — SXSW 2016 startup competition winner → 30K users/24h → product viral. Different model: rely on PR/conference traction signal.

---

### 1.6 KiotViet (Vietnam — POS/retail management B2B vertical)

**Cohort identification:** Local SME retailers — Hanoi-based founder team focused early on local market.

**Cohort size:** Hiện 150,000+ stores trên platform sau ~10 năm (founded 2014). Specific beta không public. ([About KiotViet](https://about.kiotviet.vn/en/about-us/))

**Duration close-loop:** ~3-5 năm pre-Series A; Series A (Jungle Ventures + Traveloka) ~2019, Series B $45M (KKR) 2024.

**Feature scope at launch:** POS first → retail management → multi-store → integrations (delivery, payment).

**Bug feedback mechanism:** Direct in-store training + phone support (high-touch sales motion).

**Graduation criteria:** Revenue traction — $59.3M (2023) → $76M (2024); Series B funding signal mature growth phase.

---

### 1.7 Vuihoc.vn / Edupia (Vietnam — edtech B2C tutoring) [reference benchmark]

**Cohort identification:** K-12 student + parent direct-to-consumer marketing.

**Cohort size:** Specific beta không public; market segment "thousands of English centers HCMC/Hanoi/Da Nang" theo Vietnam EdTech market 2024 $1B → 2033 $3B projected. ([Vietnam EdTech Market](https://www.expertmarketresearch.com/reports/vietnam-education-technology-market))

**Feature scope at launch:** Single subject vertical (math / English) → expand subjects.

**Bug feedback mechanism:** Live class teacher interaction + analytics dashboard.

**Graduation criteria:** Subject vertical PMF → expand horizontally.

---

## §2. Common patterns extracted (matrix)

| Pattern | Stripe | Linear | Notion | Superhuman | MISA | ELSA | KiotViet |
|---|---|---|---|---|---|---|---|
| **Close-loop cohort size** | 10-30 YC friends | Founder + investor network (specifics non-public) | Failed → reset → Product Hunt community | 100/tuần hard cap from waitlist 180K+ | Government + SOE bootstrap | Vietnam-only early | Local SME retailers |
| **MVP slice scope** | API + manual backend | Issue tracking narrow | Notes+tasks+wiki+DB | Email client narrow | Accounting single product | Pronunciation single use case | POS single use case |
| **Bug-fix turnaround target** | "As fast as they could" 6mo iterate | Not public | Reset signal after 2 năm sai direction | 1-on-1 calls → immediate feedback loop | Consultant-led trial | AI continuous learning | In-store training + phone |
| **Founder-friend reliance** | EXTREME (YC network) | High | Moderate (Product Hunt community) | EXTREME (founder onboard every user) | Network B2G first | Native Vietnam network | Local network |
| **Critical metrics tracked** | Devs using API + word-of-mouth | Velocity (not public) | Product Hunt engagement | Sean Ellis 40% disappointed | Client count + bank partnership | Vietnam → global user count | Store count + revenue |
| **Duration close-loop** | ~6 tháng | Multi-year | ~3 năm rebuild | Multi-year throttled | Multi-decade | ~1-2 năm Vietnam-only | ~3-5 năm pre-Series A |
| **Graduation trigger** | Word-of-mouth viral | Product polish + invite signal | Product Hunt traction | Sean Ellis PMF survey | Bank partnership | SXSW competition winner | Revenue traction |

**3 patterns chi phối (extracted):**

1. **Concierge onboarding pattern** (Stripe + Superhuman) — founder TỰ install/onboard. 1-3 friends @ Kite scale = founder tự đi tới user, không gửi link.

2. **MVP slice radical reduction** (Notion + Stripe manual-backend + ELSA single feature) — beta KHÔNG feature-complete; focus 1-2 critical paths. Kite hiện carry 46 Wave 80+ features → slice xuống 5-8 critical paths cho close-loop.

3. **Long-duration iteration + tiny cohort > short-duration + big cohort** (Stripe 6mo with 10-30, Superhuman multi-year throttled) — Kite Plan D 3-tuần với 1-3 friends fit pattern này; có thể cần extend đến 6-8 tuần nếu close-loop reveals deep bugs (likely sau 17-bug surface).

---

## §3. Anti-patterns to avoid (from failures)

### 3.1 Premature scale + reputation damage

**Quibi case study:** $1.75B burned trong 6 tháng pre-shutdown. Cause: launched với full marketing spend trước khi PMF validate. Anti-pattern: scale before learning. ([Why SaaS Products Fail](https://www.codica.com/blog/why-saas-startups-fail/))

**Kite Platform application:** Risk MODERATE — không có $1.75B at stake, nhưng time-burn similar pattern: nếu invite 10 paying users trước khi close-loop 1-3 friends close cleanly → 17 bugs surfaces trong front of 10 paying = reputation hit + churn → relaunch cost.

### 3.2 Beta link gửi rồi chờ ("Will you try our beta?")

Paul Graham documented Stripe avoided this pattern. Anti-pattern: low founder-time investment per user = low feedback density = slow iteration.

**Kite Platform application:** Solo-dev mode mặc định fit pattern này (limited bandwidth) — MUST counter explicitly bằng concierge installation OR scheduled walkthrough call per friend.

### 3.3 Ship trên scope chưa walked + tin audit pass

**Recurring pattern across early-stage SaaS:** "Bugs, glitches, usability issues frustrate users and drive them to seek alternative solutions." Damage from poor beta reviews compounds — users vent in reviews + share negative feedback. ([Codica SaaS Failures](https://www.codica.com/blog/why-saas-startups-fail/))

**Kite Platform application:** ĐÂY CHÍNH LÀ pattern Wave meta-6 đã hit — 17 bugs/feature × 50% Wave 80+ features unverified projected ≥50-150 bugs cross-feature. **Anti-pattern most directly relevant cho Kite right now.** Mitigation = `feature-ship-runtime-walk-mandate.md` v1.0.0 + Phase 2 retro-walk batch.

### 3.4 Feature-complete-then-launch (vs MVP slice)

Notion 2013-2015 thất bại = build "all-in-one no-code tool" trước khi market validate. Reset bỏ 4 nhân viên + scope, focus narrower "notes+tasks+wiki+DB" → success.

**Kite Platform application:** Kite hiện có 46 Wave 80+ features + Track 2 8 ports + Phase 1 BETA scope đầy đủ. Risk: tương tự Notion 2013 — build trước khi validate. Plan D MVP slice = signal "we're learning Notion 2015 reset, not Quibi launch."

### 3.5 Beta cohort quá lớn quá sớm

Industry guidance "20-50 beta users for soft launch" ([LivePlan SaaS Beta Launch](https://www.liveplan.com/blog/starting/saas-beta-launch)) — nhưng pattern này áp dụng cho product có PMF nascent, không phải pre-PMF.

**Kite Platform application:** 1-3 friends THẤP hơn industry advice nhưng phù hợp với pre-PMF + solo-dev. Risk: TỰ đặt cohort lớn ($5/$10 paying) trước close-loop close = scale-before-learning.

---

## §4. Kite Platform application matrix

| Common pattern | Kite adopt? | Adaptation cho VN edu + solo-dev | Implementation cost |
|---|---|---|---|
| **Concierge installation (Stripe/Superhuman)** | ✅ YES | Founder Việt → schedule 30-60 min walkthrough call/session per friend qua Zalo/Google Meet. Install bằng tay từng tài khoản (seed admin via SQL + invite via UI). | 2-4h per friend setup; 1-2h/week per friend follow-up | 
| **MVP slice radical reduction** | ✅ YES — CRITICAL | Identify 5-8 critical paths (Owner signup → invite 1 staff → 1 class CRUD → 1 invoice). Defer Track 2 ports + non-MVP features. | 1 tuần scope-definition + walking (Phase 2 retro-walk batch limited to MVP slice paths) |
| **Long-duration tiny cohort** | ✅ YES — but stretch Plan D 3w → 4-6w | 1-3 friends qua 6 tuần thay vì 3 tuần để cho phép deep iterate cycles | Calendar window extend, không thêm cost |
| **30-min onboarding call mandatory** | ✅ YES — adapted | 60-min call Việt (longer because VN biz culture relationship-build first 15min) + screen-share install + first transaction walkthrough | 1h/friend/session + 1h follow-up |
| **Sean Ellis PMF survey at graduation** | ⚠️ MAYBE | Tiny cohort 1-3 không statistically significant cho Sean Ellis. Use qualitative: "Bạn sẽ giới thiệu Kite cho 2 trung tâm khác không?" Net Promoter qualitative | 30 min/friend survey at week 4-6 |
| **Word-of-mouth viral indicator (Stripe)** | ⚠️ ASPIRATIONAL | Friend-of-friend signup = strong graduation criterion; tiny cohort khó measure | Free (organic) |
| **Founder follows feedback comment thread (Notion)** | ✅ YES | Github Issues / Linear / private channel per friend cho bug reports; founder respond <24h | <30 min/day |
| **VN-specific high-touch consultant onboarding (MISA)** | ✅ YES — solo dev = self | Founder = consultant. VN edu market expects high-touch; Zalo/email response within day. | Already implicit in concierge pattern |
| **VN-only training data ground (ELSA pattern)** | ✅ YES | VN tutoring center pain points (đăng ký học, điểm danh, hóa đơn VND, phụ huynh Zalo communication) = first user research signal | Free — already aligned via persona work Wave 75+ |
| **Public Product Hunt launch (Notion)** | ❌ NO — defer | Premature trước close-loop close; risk reputation damage if bugs surface publicly | Defer to GA milestone |
| **Throttled waitlist (Superhuman 100/week)** | ❌ NO — over-engineered | Solo-dev không cần waitlist software; manual invite list sufficient | Defer to Phase 1.5+ |
| **SXSW conference launch (ELSA)** | ❌ NO — không applicable | VN edu market — không có equivalent edu-conference inflection signal cần thiết Phase 1 BETA | N/A |

---

## §5. Plan D refinement based on benchmark

**Current Plan D:** "1-3 friend beta ~3 weeks close-loop"

### 5.1 Cohort size 1-3 — validation

**Benchmark verdict:** ✅ APPROPRIATE for pre-PMF + solo-dev. Below industry "20-50 beta users" recommendation but matches Stripe's "first 10-30 YC friends" magnitude when scoped to solo-dev capacity.

**Rationale:**
- First Round PMF Levels: "5-7 people willing to use + 1 willing to pay" = nascent PMF signal. 1-3 friends sits in this range.
- Concierge installation per friend costs 4-8h/friend setup + 1-2h/week follow-up. Solo-dev với 12-18h/day budget = max 3 friends actively walkable in parallel without quality drop.
- Stripe 10-30 friends size assumed YC ecosystem feedback density (multiple friends discussing per day). Kite Việt context = fewer parallel friends but deeper per-friend session.

**Recommendation:** **Keep 1-3 friends.** Don't aspire to 5-10 cohort prematurely.

### 5.2 Duration 3 weeks — risk too short

**Benchmark verdict:** ⚠️ POSSIBLY TOO SHORT. Industry baseline 4-8 weeks for general SaaS; major capabilities 4-6 weeks; platform-level changes 8-12 weeks. ([How Long Does a Beta Test Last](https://blog.betatesting.com/2025/10/17/how-long-does-a-beta-test-last/))

**Rationale specific to Kite:**
- 17 bugs surfaced trong Wave meta-6 single feature walk → expect 5-15 bugs/critical-path × 5-8 paths = 25-100 bugs total post-MVP-slice walk
- Fix turnaround 1-3 day each → bugfix queue alone ~6-8 weeks
- VN biz culture relationship-building first sessions slower than US dev culture; expect 2-3 sessions before friend gives substantive critical feedback (not polite hedging)

**Recommendation:** **Stretch Plan D 3 weeks → 4-6 weeks close-loop minimum.** Frame in user message khi propose.

### 5.3 Feature scope — Plan D needs explicit MVP slice definition

**Benchmark verdict:** ❌ CURRENT PLAN D LIKELY OVER-SCOPED. Industry pattern: MVP slice = 1-2 critical paths, not full feature set.

**Rationale:**
- Kite hiện carry 46 Wave 80+ features + Track 2 8 ports + Phase 1 BETA full scope
- Wave 80+ retro audit 50% features lack runtime walk = 23 features potentially carrying bugs
- Brokering "trust full beta" trên 46 features pre-close-loop = Quibi-pattern risk

**Recommendation:** **Define explicit MVP critical path slice before invite:**
- Path 1: Anonymous → beta signup (Owner persona) — already most stable per RST walks
- Path 2: Owner login → admin dashboard → invite 1 staff → staff accepts → staff login (Wave meta-6 Bucket A — KNOWN BROKEN, needs fix)
- Path 3: Owner → create 1 class → enroll 1 student
- Path 4: Owner → generate 1 invoice → email delivery (Wave 105 GAP-702 email firing)
- Path 5: Staff → view class → take attendance 1 session

5 paths × walk evidence required → ~10-15 days walking + bugfixing **PER FRIEND** in concierge mode.

### 5.4 Bug feedback mechanism

**Benchmark verdict:** Plan D needs explicit channel.

**Recommendation:**
- Zalo group per friend (VN cultural fit; per `vn-localization-audit-checklist.md` §2.4)
- GitHub Issues label `beta-feedback-friend-N` cho Claude tracking
- Weekly 60-min Google Meet với friend; founder takes notes inline trong session
- Bug fix turnaround commitment: P0 (blocking critical path) <24h, P1 <72h, P2 <1 week

### 5.5 Graduation criteria — Plan D needs clear close-loop exit

**Benchmark verdict:** Current Plan D doesn't specify graduation criteria.

**Recommendation cho Kite Plan D refined:**
- **Hard gate 1:** All 5 MVP paths walked clean 2 weeks consecutive across all 1-3 friends (no P0 bugs surfaced in walk)
- **Hard gate 2:** Each friend completes ≥1 real-world workflow (1 real invoice issued OR 1 real class taught OR 1 real staff invited)
- **Hard gate 3:** Each friend qualitative answer "Bạn sẽ giới thiệu Kite cho 2 trung tâm khác không?" = YES
- **Soft gate:** Optional Sean Ellis "very disappointed if gone" but n=1-3 không statistically rigorous

When 3/3 hard gates met → graduate to Phase 1.5 expanded beta with 5-10 paying users.

---

## §6. Realistic timeline for credible beta launch

**Current state:** 17 bugs known (Wave meta-6 Bucket A) + 50% of 46 Wave 80+ features lack runtime walk evidence + Phase 1 BETA scope đầy đủ chưa walk.

### 6.1 Stage (a) — Close-loop 1-3 friends beta

**Estimate: 4-8 weeks (range 2-3 friends + concierge mode)**

| Sub-stage | Duration | Critical work |
|---|---|---|
| MVP slice definition + Phase 2 retro-walk on 5 paths only | 1-2 tuần | Walk Path 1-5 critical paths; surface bugs; file as gaps |
| Fix P0 bugs from walks (Wave meta-6 Bucket A + Phase 2 surface) | 1-3 tuần | Bug 14 email + Bug 17 user provisioning + sister Wave 80+ bugs |
| Identify + invite 1-3 friends | <1 tuần | Personal outreach Zalo; concierge installation prep |
| Concierge install + walkthrough sessions per friend | 2-4 tuần (parallel) | 4-8h setup + 1-2h/week × 4-6 weeks per friend |
| Bug fix turnaround during close-loop | Ongoing | P0 <24h, P1 <72h, P2 <1 week |

**Dependencies + risks:**
- Wave meta-6 Bucket A P0 fix (Bug 14 email + Bug 17 user provisioning) BLOCKING — friends cannot use product trước khi staff-invite works
- Phase 2 retro-walk MVP paths may surface 25-100 additional bugs → bugfix queue dominates timeline
- VN biz culture friend availability — Tết / 30/4 / 2/9 holidays may impact weeks

**Fastest credible: 4 tuần if 0 P0 surface trong walk + friends responsive. Realistic: 6-8 tuần.**

### 6.2 Stage (b) — Expanded beta 10 paying users

**Estimate: +6-10 weeks beyond stage (a)**

| Sub-stage | Duration | Critical work |
|---|---|---|
| Process close-loop feedback → fix consolidated bug backlog | 2-3 tuần | Patterns across 1-3 friends |
| Full audit suite refresh (UI/Quality/Security/Performance/Ops/API/Business) | 1-2 tuần | Per `post-wave-audit-mandate.md` §2 |
| Phase 2 retro-walk remaining ~20 Wave 80+ features (beyond MVP slice) | 3-5 tuần | Per `feature-ship-runtime-walk-mandate.md` |
| Production hardening (Phase 1.5 PAID 5 BLOCKING + 4 STRONGLY recommend per `release-1-plan-2026.md` §2) | 4-6 tuần overlap | Payment + refund/AUP/DSAR/RTBF + pen test |
| 10-user cohort identification + invite (waitlist or selective outreach) | 1-2 tuần | Lighter-touch than 1-3 close-loop |

**Total estimate +6-10 tuần from stage (a).**

### 6.3 Stage (c) — GA launch (v1.0.0)

**Estimate: +12-20 weeks beyond stage (b)**

**Hard dependencies từ `release-1-plan-2026.md`:**
- PDPL 2023 effective date 2026-07-01 (~5 tuần countdown today 2026-05-28) — Phase 2 PDPL items must ship before
- Counsel engagement Phase 2 (~6-10 tuần ETA from decision point)
- v1.0.0 quality gate ≥85/100 (`output-review-mandate.md` §3)
- 5 BLOCKING + 4 STRONGLY recommend Phase 1.5 PAID gaps

**Conservative GA timeline: 2026-08 to 2026-10 range** depending on Phase 1.5 PAID close + counsel.

### 6.4 Summary table

| Stage | Duration | Cumulative (from today 2026-05-28) | Date estimate |
|---|---|---|---|
| (a) Close-loop 1-3 friends | 4-8 tuần | 4-8 tuần | 2026-06-25 to 2026-07-23 |
| (b) Expanded beta 10 paying | +6-10 tuần | 10-18 tuần | 2026-08-06 to 2026-10-01 |
| (c) GA v1.0.0 | +12-20 tuần | 22-38 tuần | 2026-10-29 to 2027-02-19 |

**Fastest credible path (a) = 4-6 tuần if:**
1. MVP slice locked tight (5 paths only)
2. Wave meta-6 Bucket A P0 bugs fix shipped 2026-06-04 (within 1 week)
3. 1 close friend availability confirmed (not 3)
4. Bug fix turnaround sustained <72h

**Realistic path (a) = 6-8 tuần** matching benchmark norms for major capability beta duration.

---

## §7. Recommendations cho Kite Plan D refinement

**Proposed Plan D refined (cumulative):**

```
Plan D-refined (hybrid close-loop):

Week 1-2: MVP slice definition + Wave meta-6 P0 fixes (Bug 14 + Bug 17)
Week 2-3: Phase 2 retro-walk MVP 5 critical paths only (not all 46)
Week 3-4: Identify + invite 1 friend (single friend close-loop pilot, NOT 3 parallel)
Week 4-8: Concierge mode 1-on-1 weekly walkthrough sessions với friend 1
         Bug fix turnaround P0<24h, P1<72h
Week 6: Sean Ellis qualitative survey + go/no-go decision
        If GO → invite friends 2+3 parallel for weeks 6-10
        If NO-GO → another iteration cycle with friend 1
Week 8: Graduation gate evaluation:
        - 5 paths walked clean 2 consecutive weeks
        - Each friend ≥1 real workflow completed
        - Qualitative "would recommend to 2 other centers" = YES
        If 3/3 PASS → graduate to Phase 1.5 expanded beta
        If NOT → extend close-loop or pivot
```

**Key adjustments từ original Plan D:**

| Original Plan D | Refined Plan D | Rationale |
|---|---|---|
| 1-3 friends parallel | Start với 1 friend, add 2+3 at week 6 if gate pass | Solo-dev capacity + reduce concurrent bug fix queue overhead |
| 3 weeks | 4-8 weeks | Industry baseline + bug fix realism |
| Full feature set | MVP slice 5 critical paths | Avoid Quibi pattern + match Notion 2015 reset learning |
| Beta link send | Concierge installation per friend | Stripe Collison + Superhuman 30-min anti-anti-pattern |
| No explicit graduation criteria | 3 hard gates + 1 soft gate | Avoid scope-creep into Phase 1.5 prematurely |

---

## §8. Sources

- [Stripe — Just Go Grind: The First Few](https://www.justgogrind.com/p/the-first-few-stripe)
- [Stripe — First Round Review](https://review.firstround.com/articles/stripe/)
- [Stripe — How They Grow](https://www.howtheygrow.co/p/how-stripe-grows)
- [Stripe — Unicorn Growth: Patrick Collison](https://www.unicorngrowth.io/p/patrick-collison-building-stripe)
- [Notion — Lenny's Newsletter: Inside Notion with Ivan Zhao](https://www.lennysnewsletter.com/p/inside-notion-ivan-zhao)
- [Notion — Figma Blog: How Notion pulled itself back](https://www.figma.com/blog/design-on-a-deadline-how-notion-pulled-itself-back-from-the-brink-of-failure/)
- [Notion — Contrary Research Business Breakdown](https://research.contrary.com/company/notion)
- [Superhuman — First Round Review: Onboarding Playbook](https://review.firstround.com/superhuman-onboarding-playbook/)
- [Superhuman — Waitlister case study](https://waitlister.me/growth-hub/case-studies/superhuman)
- [Superhuman — How They Grow](https://www.howtheygrow.co/p/how-superhuman-grows)
- [MISA Vietnam](https://www.misa.vn/en/)
- [MISA — Standard Chartered partnership](https://theinvestor.vn/standard-chartered-accounting-software-firm-misa-join-hands-to-finance-smes-d4547.html)
- [ELSA Speak — Vietcetera: Vu Van Journey](https://vietcetera.com/en/vu-vans-journey-in-building-elsa-speak)
- [ELSA Speak — VnExpress AI English 50M learners](https://e.vnexpress.net/news/tech/how-vietnamese-entrepreneur-utilizes-ai-to-teach-english-to-50-million-learners-worldwide-4849115.html)
- [KiotViet about](https://about.kiotviet.vn/en/about-us/)
- [KiotViet Vietnam B2B SaaS landscape](https://ptrangnguyen.medium.com/b2b-saas-in-vietnam-2019624d431d)
- [Vietnam EdTech Market 2024-2033](https://www.expertmarketresearch.com/reports/vietnam-education-technology-market)
- [LivePlan — 7 Steps SaaS Beta Launch](https://www.liveplan.com/blog/starting/saas-beta-launch)
- [How Long Does a Beta Test Last](https://blog.betatesting.com/2025/10/17/how-long-does-a-beta-test-last/)
- [First Round — Levels of PMF](https://www.firstround.com/levels)
- [Codica — Why SaaS Startups Fail](https://www.codica.com/blog/why-saas-startups-fail/)
- [Lean Startup — Achieving Product-Market Fit](https://leanstartup.co/resources/articles/a-playbook-for-achieving-product-market-fit/)

---

## §9. Cross-references

- `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` — 17-bug walk findings driving urgency cho close-loop discipline
- `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md` — 50% Wave 80+ unverified context driving MVP slice recommendation
- `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0 — runtime walk mandate per close-loop
- `documents/03-planning/roadmap/release-1-plan-2026.md` Phase 1 BETA scope context
- `.claude/rules/outside-in-coverage-trigger.md` v1.1.0 §1 — this benchmark = direct output of outside-in trigger
- `.claude/rules/vn-localization-audit-checklist.md` v1.1.0 §2.4 — Zalo cultural fit pattern reference
