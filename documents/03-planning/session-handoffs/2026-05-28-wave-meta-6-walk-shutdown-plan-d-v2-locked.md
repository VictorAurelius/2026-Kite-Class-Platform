---
audience: dev
date: 2026-05-28
session-theme: Wave meta-6 Bucket A walk shutdown → Plan D v2 locked → Wave A planned
prs_merged: [1916, 1920, 1921, 1922, 1923]
prs_open: [1924]
prs_closed: [1919]
gaps_filed: [GAP-786, GAP-787, GAP-788]
meta_rules_shipped: [feature-ship-runtime-walk-mandate.md v1.0.0, docs-only-pr-no-block-wait.md v1.0.0]
walk_status: SHUTDOWN at Bước 2.10 — Wave meta-6 Bucket A 17 bugs surfaced; 2 P0 paths missing
plan_d_v2: 4-8 weeks locked (1 friend → expand 2+3 at Week 6 gate)
main_head_at_close: 4aa6d054
context_at_close: 91% (Opus 1M)
next_session_pickup: Wave A execution (Bug #14 + #17 + GAP-704 + Course/Class CRUD)
---

# Session handoff — Wave meta-6 walk shutdown + Plan D v2 locked + Wave A planned

## Session arc

User concern: "mất niềm tin vào việc beta full flow pass". Walk Wave meta-6 Bucket A staff-invite flow → SHUTDOWN tại Bước 2.10 (login impossible — Bug #17 accept doesn't create user). 17 bugs surfaced in shipped-DONE feature. Audit retro 46 features → 50% NONE walk evidence. Per `outside-in-coverage-trigger.md` mandatory, spawned 3 Opus background agents to inform realistic plan.

3 agents CONVERGED — Plan D 3-week unrealistic; **5-8 weeks realistic; 4 weeks fastest credible**.

## 5 PRs merged + 1 PR pending merge

| PR | Title | Merged at | Scope |
|---|---|---|---|
| #1916 | fix(wave-meta-6-walk): 7 walk-fixes + META rule + findings | `b6539bab` | 7 walk-fixes (Bug #7/8/10/11/12/13/15) + META rule `feature-ship-runtime-walk-mandate.md` v1.0.0 + 17-bug findings doc |
| #1920 | fix(ci): mermaid check tempfile race | `a5039e24` | Self-hosted runner CSV race fix |
| #1921 | audit(retro): Wave 80+ DONE features walk-evidence | `95cc53b6` | 46 features enumerated / 10 sampled / 50% NONE walk evidence |
| #1922 | feat(post-walk): 3 gaps + Bug #17 code comment + META rule no-block-wait | `fa6256c4` | GAP-786/787/788 + META rule `docs-only-pr-no-block-wait.md` v1.0.0 + Java comment fix mis-reference |
| #1923 | audit(outside-in): 3 agents + Plan D v2 synthesis | `4aa6d054` | Persona simulation + Failure-mode matrix + BETA-launch benchmark + synthesis doc |
| #1924 | plan(phase2-beta-wave-a): Wave A P0 fixes | 🔄 pending | Wave A plan Bug #14+#17+GAP-704+Course/Class CRUD |
| #1919 | (superseded GAP-783 wrong approach) | ❌ CLOSED | Initial `@PreAuthorize hasAnyAuthority` was wrong — kiteclass-core SecurityConfig.permitAll makes ALL @PreAuthorize no-op |

## 17-bug walk catalog (Wave meta-6 Bucket A)

| # | Class | Severity | Walk-fix shipped? | Real fix scope |
|---|---|---|---|---|
| 7 | FE missing role param | P1 | ✅ #1916 | Small |
| 8 | @PreAuthorize ghost-guard | P0 | ✅ header-RBAC #1916 | Sweep all kiteclass-core controllers |
| 9 | owner.test no tenant | P2 dev | ✅ DB hack | seed-data.sh fix |
| 10 | Gateway reactor blocking | P0 | ✅ Mono+boundedElastic #1916 | Sweep gateway controllers |
| 11 | Owner sidebar nav missing | P1 | ✅ +1 item #1916 | Nav coverage audit |
| 12 | FE .map() ApiResponse | P1 | ✅ defensive unwrap #1916 | Sweep all FE pages |
| 13 | UserContext Long vs UUID | P0 workaround | ✅ null-allowed | LARGE refactor 40+ touchpoints |
| **14** | **Email never sent** | **P0** | ❌ no walk-fix possible | GAP-787 — Outbox + RabbitMQ + email template + binding |
| 15 | by-token endpoint missing | P0 | ✅ added controller #1916 | Audit other endpoints missing |
| 16 | Gateway tenant resolution public path | P0 arch | ✅ dev header workaround | Architecture decision |
| **17** | **Accept doesn't create user** | **P0** | ❌ no walk-fix possible | GAP-786 — Option A/B/C user provision architecture |
| 1-6 | Pre-session RST bugs | mixed | Per prior session handoff | Per prior handoff |

## 3 outside-in agents — CONVERGED conclusions

| Agent | File | Top finding |
|---|---|---|
| Failure-mode matrix | `audits/persona-review/2026-05-28-beta-failure-mode-matrix.md` (465 lines) | 5-8 weeks compound; AWS deploy projection 15-25 bugs (Class J Secrets highest GAP-717 recurrence) |
| Persona simulation | `audits/persona-review/2026-05-28-beta-launch-persona-simulation.md` (530 lines) | P2 Owner-only cohort 1-3 friends KHÔNG mix; 5 tuần total realistic |
| BETA-launch benchmark | `audits/outside-in-benchmark/2026-05-28-beta-launch-saas-patterns-benchmark.md` (430 lines) | Concierge install (Stripe + Superhuman pattern); 1 friend first → expand 2+3 Week 6 gate |
| Synthesis | `audits/persona-review/2026-05-28-beta-plan-d-v2-3-agent-synthesis.md` (250 lines) | 4 weeks fastest credible; 6-8 weeks recommended matching industry norms |

## Plan D v2 LOCKED — 4-8 weeks

| Week | Phase | Activity |
|---|---|---|
| **1-2** | Prep P0 (Wave A) | Bug #14 email + Bug #17 user provision + GAP-704 JWT + Course/Class CRUD verify |
| 3 | RST walk | 5 critical paths verify (signup → invite staff → CRUD → invoice → attendance) |
| 4 | AWS deploy | Stage deploy + fix 15-25 production-surface bugs (Class I/J/K/L/M projected) |
| 5-6 | Close-loop 1 friend | Concierge install (Zalo call) + <72h fix turnaround |
| **7** | **GATE** | Decision: feature-pass rate ≥80% AND NPS ≥7 → expand 2+3 friends |
| 8-10 | Expanded close-loop | 2+3 friends + iterate |
| 11+ | Phase 1.5 PAID | Decision: open beta wider OR graduate paid |

## Wave A plan (PR #1924 pending merge)

| Bucket | Scope | Est | Architecture decision |
|---|---|---|---|
| A | Bug #14 email path | 5-7 ed | **Option B Outbox + RabbitMQ + kitehub-email consumer** |
| B | Bug #17 user provision on accept | 3-5 ed | **Option B Outbox event `staff.invitation.accepted` → kitehub-platform consumer** |
| C | GAP-704 JWT tenantId post-signup verify | 1-2 ed | Straightforward |
| D | Course/Class CRUD foundational walk + IT | 2 ed | Verify-only |

**Total: 7-12 eng-days fitting Plan D v2 Week 1-2 budget.**

Recommended execution: **sequential single-agent (Opus 4.7 1M)** A → B → C → D.

## 2 META rules shipped this session

1. `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0 — CRITICAL — mandate RST walk at ORIGINAL feature-ship time (sister to `pre-handoff-self-test-completeness.md` v1.2.0 §3 which covers POST-FIX). Closes trust-pass anti-pattern recurrence ≥7 quantified.

2. `.claude/rules/docs-only-pr-no-block-wait.md` v1.0.0 — MANDATORY — Sister to `docs-only-pr-auto-merge.md`. While CI runs on docs-only PR → continue work; auto-merge on background notification. Sister covers WHEN-CI-green merge gate.

## 3 gaps filed (Phase 1 BETA scope)

- **GAP-786 P0 Backend** — Staff invite accept service không create user record (Bug #17 architectural)
- **GAP-787 P0 Backend** — Staff invite email send never implemented (Bug #14 feature gap)
- **GAP-788 P0 Meta** — META Wave 80+ retro-walk batch tracking 32 walk-needed features

## Open items + next session pickup

### Immediate (after PR #1924 merge)

1. Wait PR #1924 auto-merge (background `bpbx75h5i` watching)
2. Decision Day 1-2: lock Bucket A + B Option B architecture (per `release-fix-retry-budget.md` §3.5 investigation-first)
3. Start Week 1 execution: Bucket A (Bug #14 email path)

### Week 1-2 (Wave A execution)

- Bucket A Bug #14: outbox event → kitehub-email consumer → MailHog verify → Production smoke deferred Week 4
- Bucket B Bug #17: outbox event `staff.invitation.accepted` → kitehub-platform `StaffUserProvisionListener` → IT test
- Bucket C GAP-704: verify Owner JWT has tenantId claim post-signup
- Bucket D Course/Class CRUD: walk + IT cover happy path

### Week 3-7 (Plan D v2 prep + close-loop)

- Week 3 RST walk 5 critical paths
- Week 4 AWS deploy stage + fix 15-25 production-surface bugs
- Week 5-6 Close-loop 1 friend (concierge install)
- Week 7 GATE decision

### Phase 2 BETA deferred items (Plan D v2 Phase 3 scope)

- Wave C: FE ApiResponse .map() unwrap sweep (5-10 FE pages) OR global axios interceptor
- Wave D: 5 retro-walk waves (signup chain / compliance+auth / email cluster / payment+audit / vendor+FE)
- Wave E: Audit suite extensions (api-contract Cat 2 + ops-readiness side-effect probe + business-logic AC walk)

## Context state at handoff

- **Context budget: 91% Opus 4.7 1M** — per `session-end-context-check.md` §3 ≥85% threshold = strong recommend `/clear`
- Main HEAD: `4aa6d054` (3 outside-in reports + synthesis); PR #1924 pending push to `4aa6d054+1`
- Local stack: kitehub+kiteclass full stack running healthy (Bug #14+#17 unfixed locally; only walk-fixes from PR #1916 applied to images)
- Local feature branches active: `plan/phase2-beta-wave-a` (PR #1924 open), `docs/session-handoff-2026-05-28-plan-d-v2` (this PR)

## References

- Walk shutdown findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md`
- Audit retro 46 features: `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md`
- 3 outside-in reports + synthesis: `documents/04-quality/audits/persona-review/2026-05-28-beta-{failure-mode-matrix,launch-persona-simulation,plan-d-v2-3-agent-synthesis}.md` + `outside-in-benchmark/2026-05-28-beta-launch-saas-patterns-benchmark.md`
- Wave A plan: `documents/03-planning/waves/wave-2026-05-28-phase2-beta-wave-a-p0-bug14-bug17-jwt-crud.md`
- 2 META rules: `.claude/rules/feature-ship-runtime-walk-mandate.md` + `.claude/rules/docs-only-pr-no-block-wait.md`
- 3 gaps: GAP-786 / GAP-787 / GAP-788 in `documents/04-quality/gaps/phase-1-beta/`
