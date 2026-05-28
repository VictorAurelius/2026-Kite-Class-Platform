---
audience: dev
date: 2026-05-28
session-theme: Beta launch Plan D refined v2 — 3 outside-in agents synthesis
scope: Wave meta-6 Bucket A walk shutdown follow-up → realistic beta timeline
agents-input:
  - persona-review/2026-05-28-beta-failure-mode-matrix.md (465 lines / 32KB)
  - persona-review/2026-05-28-beta-launch-persona-simulation.md (530 lines / 35KB)
  - outside-in-benchmark/2026-05-28-beta-launch-saas-patterns-benchmark.md (430 lines / 31KB)
plan-d-v1: ~3 weeks (1-3 friends close-loop)
plan-d-v2-refined: 4-8 calendar weeks (1 friend start → expand to 2+3 at Week 6 gate)
fastest-credible: 4 weeks
recommended: 6-8 weeks
---

# Beta launch Plan D refined v2 — 3 outside-in agents synthesis

## Context

Wave meta-6 Bucket A RST walk shutdown 2026-05-28 surfaced 17 bugs in shipped-DONE feature including 2 P0 paths COMPLETELY MISSING. Audit retro found 50% of 46 Wave 80+ features lack runtime walk evidence. User mất niềm tin về beta full-flow pass; need realistic plan.

Per `outside-in-coverage-trigger.md` v1.1.0 MANDATORY, 3 Opus agents spawned in parallel (per `agent-model-opus-default.md`):
- Agent 1: Failure-mode matrix (13 bug classes × 15 flows)
- Agent 2: Persona simulation (5 personas × 3 critical flows)
- Agent 3: External BETA-launch benchmark (Stripe/Linear/Notion/Vercel/VN edu)

This document synthesizes all 3 into final refined Plan D v2.

## CONVERGENCE — 3 agents independently agree

| Aspect | Agent 1 Failure-mode | Agent 2 Persona | Agent 3 Benchmark |
|---|---|---|---|
| Plan D 3-week realistic? | ❌ NO (need 27-42 eng-days) | ❌ PARTIAL (5 ngày prep insufficient) | ❌ NO (4-8 weeks industry norm) |
| **Realistic timeline** | 5-8 weeks compound | 5 weeks (10d prep + 3w beta) | **6-8 weeks** matching norms |
| Fastest credible | 3-4 weeks (refined scope) | 5 weeks honest | **4 weeks** with sustained <72h turnaround |
| Friend cohort start | 1-3 friends | 1-3 friends | **1 friend first → expand 2+3 at Week 6 gate** |
| Scope reduction | 9 flows (defer 6) | P2 Owner-only (defer P3/P4/P5) | **5 critical paths MVP slice** |
| Critical Bug Week 1 | #14 + #17 paired | #14 + #17 + GAP-704 + CRUD | #14 + #17 |

→ **All 3 agents independently arrive at SAME core conclusions.** High signal — Plan D v2 refinement is well-grounded.

## Insights mới từ Agent 3 (BETA-launch benchmark — không có trong Agent 1+2)

### 3 patterns Kite NÊN adopt

1. **Concierge installation pattern** (Stripe Collison + Superhuman 30-min 1-on-1) — founder TỰ schedule walkthrough call 60min qua Zalo/Google Meet + install bằng tay tài khoản cho từng friend.
   - Counter "send link + chờ feedback" anti-pattern (solo-dev default fall)
   - Phù hợp solo-dev + 1-3 friends scale
   - Bug feedback signal quality 10x cao hơn email/Slack thread

2. **MVP slice radical reduction** (Notion 2015 reset + Stripe manual-backend + ELSA single-use-case)
   - Slice xuống **5 critical paths** (signup → invite staff → class CRUD → invoice → attendance)
   - Thay vì cố verify 46 Wave 80+ features (per audit retro)
   - Counter Quibi-class scale-before-learning risk

3. **Long-duration tiny cohort > short-duration big cohort** (Stripe 6 tháng × 10-30; Superhuman 100/tuần throttled)
   - Stretch Plan D từ 3 tuần → 4-8 tuần
   - **1 friend first**, then expand 2+3 at Week 6 gate
   - Industry baseline 4-8 weeks cho major capability beta

### 3 anti-patterns AVOID

1. **Ship trên scope chưa walked + tin audit pass** — CHÍNH LÀ Wave meta-6 pattern recurrence ≥7. Mitigation: `feature-ship-runtime-walk-mandate.md` v1.0.0 + Phase 2 retro-walk batch BEFORE invite.

2. **Beta cohort quá lớn quá sớm** — industry recommend 20-50 nhưng đó POST-PMF; Kite hiện pre-PMF + solo-dev → 1-3 friends đúng range. Risk: 10 paying users trước close-loop close = Quibi pattern.

3. **Beta link send + chờ feedback** (Paul Graham anti-pattern Stripe tránh được) — solo-dev mặc định fit pattern này; counter explicitly bằng scheduled call + concierge install (pattern #1).

## REFINED PLAN D v2 — Final timeline

### Phase prep: Week 1-4 (engineer work)

| Week | Phase | Activity | Effort | Owner |
|---|---|---|---|---|
| **1** | P0 Wave A | Fix Bug #14 email send (outbox + RabbitMQ + email template + binding kitehub-email) | 5-7 eng-days | Solo dev |
| **1-2** | P0 Wave B | Fix Bug #17 user provision on accept (Option A/B/C decision then implement) | 3-5 eng-days | Solo dev |
| **2** | P0 GAP-704 JWT | JWT tenantId claim post-signup verify + fix nếu broken | 1-2 eng-days | Solo dev |
| **2** | Course/Class CRUD verify | Walk verify foundational entity CRUD (base for attendance/grade/billing) | 2 eng-days | Solo dev |
| **3** | RST walk 5 paths | Manual walk: signup → invite staff → class CRUD → invoice → attendance | 3 eng-days | Solo dev |
| **3** | AWS deploy stage | Deploy + smoke test on AWS staging | 2 eng-days | Solo dev |
| **4** | Production-surface bug fix | Fix 5-10 production-only bugs (Class I/J/K/L/M) surfaced AWS deploy | 5-7 eng-days | Solo dev |

**Total prep: ~22-30 eng-days = 3-4 calendar weeks** with sustained focus.

### Phase close-loop: Week 5-7 (1 friend)

| Week | Phase | Activity | Owner |
|---|---|---|---|
| **5** | Friend onboarding | Concierge install 60-min Zalo call + walkthrough 5 critical paths | Founder + 1 friend |
| **5-6** | Daily fix turnaround | <72h target per bug surfaced. Daily standup OR weekly call to collect feedback | Founder |
| **6** | Mid-cohort review | Friend rates feature-pass rate per path (1-10) + qualitative feedback | Founder + friend |
| **7** | **GATE decision** | If feature-pass rate ≥80% AND friend recommend-NPS ≥7 → expand cohort | Founder |

### Phase expanded: Week 8-10 (2+3 friends)

| Week | Activity |
|---|---|
| **8** | Recruit + onboard friends 2+3 (same concierge pattern) |
| **8-10** | Iterate based on multi-friend feedback variance |
| **10** | Phase 1.5 PAID prep decision: open beta wider OR graduate paid |

### Summary timelines

| Variant | Total | Risk |
|---|---|---|
| Fastest credible | **4 weeks to start 1-friend beta** | Higher — assumes <72h turnaround + clean AWS deploy |
| Recommended | **6 weeks to start + 8-10 weeks to expand** | Realistic — matches industry benchmark |
| Conservative | 8-10 weeks to start | Lower risk, longer market wait |

## Concrete bare-minimum prep checklist (Week 1-4)

Per persona MUST-HAVE matrix + failure-mode HARD blockers:

### Must-fix Week 1-2 (HARD blockers per Agent 1)

- [ ] **Bug #14 Email send path** (GAP-787) — outbox + RabbitMQ binding + email template + kitehub-email consumer
- [ ] **Bug #17 User provision on accept** (GAP-786) — architecture decision Option A/B/C then implement
- [ ] **Bug #13 UserContext UUID refactor** OR null-allowed workaround (already shipped, may need more sites)
- [ ] **Bug #8 @PreAuthorize sweep** (GAP-788 Wave A) — clear ghost-guards across all kiteclass-core controllers
- [ ] **GAP-704 JWT tenantId claim** — verify post-signup JWT contains tenantId

### Must-walk Week 3 (5 critical paths)

- [ ] Signup → email verify → first login
- [ ] Owner invite staff → email arrives → staff accept → staff login (FULL E2E)
- [ ] Course/Class CRUD (foundational)
- [ ] Invoice generate + payment
- [ ] Attendance per class

### Must-fix Week 4 (AWS deploy bugs — Agent 1 projection 15-25 bugs)

- [ ] Class I CSP / CORS / DNS (3-5 bugs)
- [ ] Class J Secrets / env coverage (4-7 bugs) — Highest risk per GAP-717 Wave 81+104.5 recurrence
- [ ] Class K SSL / TLS cert (2-4 bugs)
- [ ] Class L SES email DKIM (3-5 bugs)
- [ ] Class M Reactor production-load (3-4 bugs)

## Friend-beta recruitment recommendation

Per Agent 2 + Agent 3 convergence:

**Recruit profile:**
- Persona: **P2 Center Owner** (NOT mix with P1/P3/P4/P5 in close-loop)
- Friends: 1 first → 2+3 at Week 6 gate
- Profile variance: different organization size (small / medium center) + different VN region (Hà Nội / TP.HCM / regional)
- Trust signal: existing trusted relationship (friend / colleague / acquaintance comfortable giving honest feedback)
- Patience signal: explicitly tell them "this is alpha, expect bugs, daily fixes will happen"

**Avoid:**
- Strangers from waitlist (no patience for early bugs)
- 10+ users (solo-dev can't sustain daily fix turnaround at scale)
- Mixed persona (signal too varied at this stage)

## Acceptance criteria (Plan D v2)

- [ ] Week 4 ends with: GAP-786 + GAP-787 + GAP-704 + Bug #8 sweep all shipped + AWS deploy clean + 5 critical paths walked
- [ ] Week 5 starts with: 1 friend recruited + concierge call scheduled + KiteHub account provisioned
- [ ] Week 6 mid-review: friend NPS ≥7 + feature-pass rate ≥80%
- [ ] Week 7 GATE: decision documented (expand OR extend prep)
- [ ] Week 10 ends with: ≤3 friends used product 4+ weeks + Phase 1.5 PAID readiness decision logged

## Risks + mitigations

| Risk | Probability | Mitigation |
|---|---|---|
| Bug #14+#17 architecture decision takes >5 days | Medium | Per `release-fix-retry-budget.md` §3.5 investigation-first; lock decision Week 1 day 1-2 |
| AWS deploy surfaces 25+ bugs (worse than projected 15-25) | Medium | Week 4 buffer can stretch to Week 5; close-loop pushes to Week 6 |
| Friend NPS <7 at Week 6 | Medium | Refine + extend close-loop; may need Bug #8/12 sweep before expand |
| Solo-dev burnout (4-10 weeks intensive) | High | Daily checkpoint + weekly retro; avoid weekend work post-Week 3 |
| User pivots scope mid-cohort | Low | Lock 5 paths + communicate to friend upfront "Phase 1 only" |

## References

- Walk shutdown findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` (17-bug catalog)
- Audit retro: `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md` (46-feat enumeration)
- Failure-mode matrix: `documents/04-quality/audits/persona-review/2026-05-28-beta-failure-mode-matrix.md` (Agent 1)
- Persona simulation: `documents/04-quality/audits/persona-review/2026-05-28-beta-launch-persona-simulation.md` (Agent 2)
- BETA-launch benchmark: `documents/04-quality/audits/outside-in-benchmark/2026-05-28-beta-launch-saas-patterns-benchmark.md` (Agent 3)
- META rule landed same session: `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0
- META rule landed same session: `.claude/rules/docs-only-pr-no-block-wait.md` v1.0.0
- Gaps filed Wave meta-6 walk: GAP-786 (Bug #17), GAP-787 (Bug #14), GAP-788 (META Wave 80+ retro-walk batch)

## Next session pickup

1. Plan D v2 locked → user approve via this session decision
2. Wave A first-wave planning agent spawn next (Bug #14 + #17 + GAP-704 + Course/Class CRUD verify scope)
3. After Wave A wave plan PR ships → execute Week 1 fix
4. Daily checkpoint via `documents/03-planning/session-handoffs/*.md` for Plan D v2 progress
