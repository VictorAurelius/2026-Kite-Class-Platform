---
title: Wave rst-cascade-1 — 13 cascade gap live walkthrough (LOCAL first → AWS verify)
status: draft
created: 2026-05-26
updated: 2026-05-26
audience: dev
tag_primary: rst-cascade
tags_secondary: [phase-1-beta, aws-restore-followup, cost-optimized, local-first]
counter: 1
date_launch: TBD (post Wave aws-restore-1 closure ship)
waves: [rst-cascade-1]
gaps: [GAP-657, GAP-658, GAP-659, GAP-543, GAP-530, GAP-370, GAP-608, GAP-684, GAP-508, GAP-514, GAP-534, GAP-538, GAP-599, GAP-502, GAP-516, GAP-531, GAP-610, GAP-611, GAP-656, GAP-724]
---

# Wave rst-cascade-1 — 13 cascade gap live walkthrough (LOCAL first → AWS verify)

## 1. Brainstorm

### Q1 — Inside-out scope

- **Source:** Wave aws-restore-1 closure session-handoff identified 13 PARTIAL gaps eligible cascade verification post-GAP-612 unblock
- **Trigger user direction 2026-05-26:** "Wave rst-cascade-1 là rst local hay rst aws, rõ ràng phải đảm bảo rst local thành công 100% trước mới rst aws để tránh lãng phí tài nguyên"
- **Rule:** Per `local-self-test-before-aws-deploy.md` v1.0.0 (Wave 102.8 Bucket C) — mandate local-pass-first → AWS-trigger

### Q2 — Outside-in (per `outside-in-coverage-trigger.md`)

**SKIPPED** per §4 exception "Wave 100% internal scope — ops/refactor/tech debt + recent outside-in audit ≤30 days". Cascade verification = walkthrough-only, no architecture rethink. Wave 100 outside-in audit covered cascade topology.

### Q3 — Risks + tradeoffs

| Risk | Severity | Mitigation |
|---|---|---|
| Local stack health regression (Docker, RDS, services) | 🟠 Medium | Phase 0 preflight per `local-self-test-before-aws-deploy.md` §3 Step 1 |
| 13 walkthroughs sequential = ~4-6h | 🟡 Low | Parallelize Phase α: 3-4 cluster sub-buckets (email + auth + onboarding + infra) via Opus agents |
| AWS resource burn during walkthrough = unnecessary cost if local FAIL | 🟠 Medium | Phase β AWS chỉ start sau Phase α 100% PASS; `bash scripts/aws/start-stack.sh` minutes-before + `stop-stack.sh --force` minutes-after |
| 5-7 day GAP-533 Resend warm-up dependency | 🟡 Low | Email cluster walkthroughs use mock-send first; final live send Day 5+ user-action |
| Some cascade gaps may STAY PARTIAL after walkthrough (true PARTIAL discoveries) | 🟡 Acceptable | Honest per `gap-done-discipline.md` §3 PARTIAL exit ramp; file follow-up gaps |

### Q4 — Authorization required

- Phase α LOCAL: Tier 1 (no AWS) + coordinator local walkthrough — no special authorization
- Phase β AWS Trigger 1 (start stack): Tier 2 `bash scripts/aws/start-stack.sh` confirm
- Phase β walkthrough (live API calls): Tier 1 read-only (curl/browser) — no special authorization
- Phase β Trigger 2 (stop stack post-verify): Tier 2 `bash scripts/aws/stop-stack.sh --force` confirm

### Q5 — Cost optimization (per user AskUserQuestion 2026-05-26 Option C Hybrid)

**Hybrid stop strategy:**

```
Wave aws-restore-1 closure ship (current PR #1857)
   ↓ stop stack immediately
bash scripts/aws/stop-stack.sh --force  ← EC2 + RDS stop, $0 burn
   ↓ Wave rst-cascade-1 Phase α LOCAL (no AWS needed)
   ↓ after Phase α 100% PASS
bash scripts/aws/start-stack.sh  ← brief production window ~30min
   ↓ Phase β AWS walkthrough subset
bash scripts/aws/stop-stack.sh --force  ← stop again
```

**Cost projection:**
- Standby (stack stopped between Phase α and Phase β): ~$13/mo RDS storage + $0 EC2 stopped (EBS storage only ~$2/mo) = **~$15/mo standby**
- Phase β active window: ~$1-2/h × ~30min = **<$1 one-time burn**
- Total Wave rst-cascade-1 marginal cost: **~$15-16** (vs ~$30-50/day if keep stack running)

## 2. Task Breakdown

### Phase 0 — Preflight (15min, all local)

| # | Task | Est | Owner |
|---|---|---|---|
| T1 | Wave plan ship + draft PR | 10min | Coordinator |
| T2 | Post Wave aws-restore-1 closure merge → `bash scripts/aws/stop-stack.sh --force` (Phase β-1 cost optimization) | 5min | User-execute (Tier 2 confirm) |

### Phase α — LOCAL walkthrough 13 gaps (3-4h)

| Cluster | Gaps | Est | Approach |
|---|---|---|---|
| **Email** | 657 + 658 + 659 + 543 + 530 + 370 | 60-90min | Bucket B0/B1 — Opus agent 1 |
| **Auth + admin** | 684 + 514 + 534 + 599 + 508 | 60-90min | Bucket B2/B3 — Opus agent 2 |
| **Onboarding + signup** | 538 + 516 + 531 + 610 + 611 + 724 | 60-90min | Bucket B4/B5 — Opus agent 3 |
| **Infra + UI** | 502 + 656 | 30-45min | Coordinator inline |

**Each cluster:**
1. `bash kitehub/scripts/up.sh --profile full` baseline + verify 4 infra services healthy
2. Per gap: walk §2.4 (a)→(g) per `pre-handoff-self-test-completeness.md`
3. Document outcome per gap (DONE / STAY PARTIAL with delta / OPEN if regression)

### Phase β — AWS verify (≤45min wall-clock + brief burn)

| # | Task | Est | Cost |
|---|---|---|---|
| T1 | `bash scripts/aws/start-stack.sh` + wait 3 EC2 running + RDS available | ~10min | Tier 2 |
| T2 | Per Phase-α-PASSED gap, brief AWS smoke (curl + browser) | ~20min | Tier 1 read-only |
| T3 | DONE flips per gap | ~5min | Coordinator |
| T4 | `bash scripts/aws/stop-stack.sh --force` | ~5min | Tier 2 |

### Phase E — Wave closure (15min)

- 5-target sync per `post-merge-sync-completeness.md` §2
- Scope-completeness reconciliation per `wave-closure-scope-completeness.md` §3
- File next session follow-ups (Wave aws-rebuild-sop-1 GAP-693, hard-blocker waves)

**Total estimate:** ~4-6h coordinator-inline OR ~2-3h with 3 parallel Opus agents Phase α.

## 3. Scope

### Phase α LOCAL — 13 gaps walkthrough scope

#### Email cluster (6 gaps)

| Gap | % current | Walkthrough scope |
|---|---|---|
| GAP-657 | 95 | EmailHardeningTest re-enable + send test email locally (mock SMTP); verify headers (Reply-To + List-Unsubscribe + multipart/alternative) |
| GAP-658 | 80 | VN sample seed worker integration với OnboardingChecklistService — locally trigger seed-data.sh + verify VN samples populated DB |
| GAP-659 | 95 | Per-tone variant render — locally render welcome.formal/informal + invite-staff.formal/informal HTML; visual diff |
| GAP-543 | 95 | 5 email types Vietnamese tone audit — locally render + native VN reader review (user-action) |
| GAP-530 | 10 | 5-email-type flow E2E locally — signup → email-verification → invite → password-reset → batch-invoice send |
| GAP-370 | 95 | Resend dashboard verify + terraform-cloudflare apply DKIM (deferred Day 5+ user-action) |

#### Auth + admin cluster (5 gaps)

| Gap | % current | Walkthrough scope |
|---|---|---|
| GAP-684 | 0 | Admin login walk per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) — full credential + nav + role-guard + page render + action |
| GAP-514 | 90 | Live 429 smoke gateway rate limit — locally fire 100+ req/s to auth endpoints + verify Retry-After header |
| GAP-534 | 80 | Invite token single-use — generate invite + use twice → expect 2nd 410 Gone |
| GAP-599 | 85 | Multi-tab JWT sessionStorage — open 2 browser tabs với different admin/owner credentials + verify token isolation |
| GAP-508 | 90 | Production env config registry post-restore verify (RESEND_API_KEY + JWT_CHALLENGE_SECRET both fetched via fetch-secrets.sh local equivalent) |

#### Onboarding + signup cluster (6 gaps)

| Gap | % current | Walkthrough scope |
|---|---|---|
| GAP-538 | 96 | Day-1 onboarding checklist + sample-data full flow locally |
| GAP-516 | 95 | Tenant initialization flow — POST /api/v1/admin/beta-requests/{id}/approve sequence |
| GAP-531 | 95 | 6-step beta approval + tenant init flow runbook walkthrough |
| GAP-610 | 75 | beta-signup validate token — generate token + validate locally → expect 200 (not TOKEN_NOT_FOUND) |
| GAP-611 | 70 | POST /api/v1/auth/beta-signup empty-body → expect JSON 404 (Class D fix verify) |
| GAP-724 | 50 | FE auth bug fixes — auth.test.ts 12/12 PASS verify locally + browser auth flow walkthrough |

#### Infra + UI cluster (2 gaps)

| Gap | % current | Walkthrough scope |
|---|---|---|
| GAP-502 | 90 | kh_backend healthy stability — local docker stack 1h soak test (verify zero auth errors + zero OOM) |
| GAP-656 | 80 | UI Coordinator widget — mobile + desktop browser walkthrough Welcome page + Support menu + first-login overlay |

### Phase β AWS — Subset of Phase α PASS gaps verify

Brief production smoke (≤30min) cho gaps đã PASS local:
- `curl https://api.kitehub.me/<endpoint>` per gap scope
- Browser test cho UI-touching gaps (599 multi-tab, 656 widget, 684 admin login, 724 FE)
- Email walkthrough deferred to Day 5+ post Resend warm-up (GAP-370/533/543/530)

### Out-of-scope

- GAP-693 SOP runbook execution — defer Wave aws-rebuild-sop-1 (~3 days separate scope)
- GAP-727 hasAccessToClass — defer Wave class-teacher-fix-1
- GAP-730 Idempotency port — defer Wave idempotency-finish-1
- GAP-533 Resend warm-up Day 1-7 user-action — parallel background ~5-7 ngày
- 4 hard-blocker waves (security-1 + ops-1 + compliance-1 + perf-1) — defer post-rst-cascade-1
- Đợt 108 RST (B-CRUD + B-vận-hành + C + D3-D4) — defer post-hard-blocker-waves

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|---|---|---|---|
| `kitehub/scripts/up.sh` | Local stack starter | Wave 82 self-host era, used Wave 102.8 Bucket A | ✅ exists |
| `kitehub/scripts/check-docker.sh` | Preflight | Wave 102.8 Bucket A | ✅ exists |
| `documents/04-quality/gaps/phase-1-beta/GAP-{657,658,...}.md` | 13 gap files | All exist phase-1-beta/ | ✅ verified |
| `bash scripts/aws/start-stack.sh` | AWS resume script | Per GAP-492 dynamic tag lookup | ✅ exists |
| `bash scripts/aws/stop-stack.sh` | AWS standby script | Same script family | ✅ exists |
| `documents/04-quality/gaps/phase-1-beta/closed/GAP-612-...` | GAP-612 DONE (prerequisite) | Closure Wave aws-restore-1 closure PR pending CI green | 🆕 to-be-merged (PR #1857) |
| AWS empirical state post-stop-stack | Tier 1 | Pre-Phase-α: expect EC2 3 stopped + RDS stopped | TBD post-stop-stack execute |

## 5. Verification Gates

Per `local-self-test-before-aws-deploy.md` §3 + `concurrent-production-mutation-ops.md`:

| Gate | Before | Pass criteria |
|---|---|---|
| Gate 1 | Wave plan ship | User explicit "claude proceed Wave rst-cascade-1" |
| Gate 2 | Post Wave aws-restore-1 closure | PR #1857 merged + main updated |
| Gate 3 | Phase β start | Phase α 100% PASS — all 13 walkthroughs documented with verdict per gap |
| Gate 4 | AWS stack start | `gh run list --status in_progress` empty (no concurrent ops) |
| Gate 5 | AWS walkthrough start | EC2 3 running + RDS available + Spring containers 7/7 healthy + api.kitehub.me/actuator/health 200 |
| Gate 6 | DONE flips | Per gap AC verified + evidence cited per `gap-done-discipline.md` §2 (NO mass-flip without per-gap evidence) |
| Gate 7 | AWS stack stop | All Phase β walkthroughs complete + audit artifact saved |

## 6. Agent Spawn Pattern

**Hybrid:** 3 parallel Opus 4.7 background agents Phase α (email + auth + onboarding clusters) + coordinator inline (infra + UI cluster + Phase β AWS) per `agent-model-opus-default.md` §1.

Spawn pattern per `agent-background-spawn-default.md` v1.0.1 + `feedback_parallel_agent_strategy.md`.

## 7. Closure Protocol

1. Audit artifact: `documents/04-quality/audits/quality/2026-05-26-wave-rst-cascade-1-closure.md`
2. Audits-index.csv row appended
3. Wave-history.jsonl entry tag-based schema (`tag_primary: rst-cascade`, `counter: 1`)
4. CSV updates per gap (DONE flips with evidence OR stay PARTIAL with delta)
5. ROADMAP §🎯 Current Status: Wave rst-cascade-1 SHIPPED entry prepended
6. Session handoff: `2026-05-26-wave-rst-cascade-1-shipped-<next-wave>-queued.md`
7. AWS stack stopped post-verify (`bash scripts/aws/stop-stack.sh --force`)
8. Frontmatter `status: draft → complete` flip

### Scope-Completeness Reconciliation (template — fill at closure)

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Email cluster (6 gaps) | TBD | — |
| 2 | Auth + admin cluster (5 gaps) | TBD | — |
| 3 | Onboarding + signup cluster (6 gaps) | TBD | — |
| 4 | Infra + UI cluster (2 gaps) | TBD | — |
| 5 | Phase β AWS subset verify | TBD | — |
| 6 | AWS stack stopped post-Phase-β | TBD | — |
| 7 | DONE flips per gap evidence | TBD | per-gap |
| 8 | Cost outcome ~$15-16 marginal | TBD | reconciled at closure |

## 8. Log

- **2026-05-26 (status: draft):** Wave plan created. Source = Wave aws-restore-1 closure session-handoff + user direction 2026-05-26 "đảm bảo rst local thành công 100% trước mới rst aws để tránh lãng phí tài nguyên". Local-first sequencing per `local-self-test-before-aws-deploy.md` v1.0.0. Cost optimization Option C (Hybrid stop strategy) per user AskUserQuestion 2026-05-26 — total marginal cost ~$15-16 (vs ~$30-50/day if keep stack running). 13 cascade gaps clustered 4 buckets (email + auth + onboarding + infra). 3 Opus 4.7 parallel agents Phase α + coordinator inline Phase β. Per `wave-tag-numbering-convention.md` v1.0.0 tag-based schema: `tag_primary: rst-cascade`, `counter: 1`. Awaiting Wave aws-restore-1 closure PR #1857 merge before launch.
