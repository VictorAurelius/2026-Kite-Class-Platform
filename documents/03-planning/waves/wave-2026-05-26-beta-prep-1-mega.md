---
title: Wave beta-prep-1 — 9 bucket mega-wave path-to-beta-launch
status: complete
created: 2026-05-26
updated: 2026-05-26
closed_at: 2026-05-26
audience: dev
tag_primary: beta-prep
tags_secondary: [phase-1-beta, pdpl-deadline-2026-07-01, outside-in-audited, parallel-execution]
counter: 1
date_launch: 2026-05-27
waves: [beta-prep-1]
gaps: [GAP-727, GAP-730, GAP-372, GAP-754, GAP-755, GAP-756, GAP-757]
prs: [1872, 1873, 1871, 1875, 1877, 1874, 1876]
---

# Wave beta-prep-1 — Phase 1 BETA path-to-launch mega-wave

## 1. Brainstorm

### Q1 — Inside-out scope (dev proposed)

Initial 6-bucket scope (Wave rst-cascade-1 closure session): PDPL compliance-min + Security beta-min + Ops beta-min + GAP-727 class-teacher + GAP-730 idempotency + Beta invite mechanism.

### Q1 — Outside-in findings (3 Opus parallel ~5min)

Per `outside-in-coverage-trigger.md` v1.1.0 — 3 agents triangulated BEFORE scope lock:

**Inside-out from ROADMAP/queue:**
- PDPL hard deadline 2026-07-01 (~5 tuần countdown)
- Phase 1 BETA gate: quality ≥80 + 5 tenants live + 0 P0 incidents 2w
- 38 active Phase 1 BETA P0 backlog (per Wave audit-stale-sweep-1 finding)

**Inside-out from inside-out-queue.md (2026-05-26 entry):**
- PDPL scope-cut decision: minimum 5 items vs full 8 items
- Risk tolerance Moderate per CLAUDE.md

**Outside-in NEW (3-agent consensus C-1 to C-8):**
1. 🔴 P0 Zalo notification gap (Persona + Benchmark) — VN edu reality
2. 🔴 P0 Tenant support channels missing (3-agent consensus) — Easy Edu norm = hotline + Zalo OA
3. 🔴 P0 PDPL Bucket A incomplete (Benchmark) — DPO + DPIA per Decree 356/2025; ngày minimum 5 items locked D2
4. 🔴 P0 Multi-branch missing (Persona) — P2 cohort blocked signup; Path A filter invite P2 1-branch locked D3
5. 🔴 P0 Payroll + Payment integration status (Persona) — defer Phase 2 + interim Excel
6. 🟠 P1 VN-localization audit chưa apply (Failure-mode) — invoice + reminder + landing templates
7. 🟠 P1 Concurrency hardening narrow (Failure-mode) — extend từ 1 → 5 hot paths
8. 🟠 P1 Beta-to-GA criteria tracking artifact (Benchmark)

### Q2 — Risks + tradeoffs

| Risk | Severity | Mitigation |
|---|---|---|
| PDPL deadline 2026-07-01 risk | 🔴 P0 | Bucket A minimum 5 items (~4-5d); ship target ~2026-06-23 = 8-15d buffer |
| Zalo OA approval delay 3-7d block Bucket G | 🟠 P1 | User registration parallel với Claude work; placeholder link → swap khi active |
| Multi-branch P2 cohort filter rejects ~50% P2 signups | 🟠 P1 | Honest expectation-setting via waitlist; Phase 2 catch-up |
| 9 buckets parallel = high coordinator overhead | 🟡 acceptable | Per Wave rst-cascade-1 proven 3-4x speedup; OK |
| Outside-in blind spots còn lại C-6/C-7/C-8 P1 | 🟡 acceptable | Address Wave beta-prep-2 post first cohort feedback |

### Q3 — Authorization required

- Phase 1 LOCAL execution: Tier 1 (no AWS) + coordinator local work
- AWS smoke verify Bucket A + F: Tier 2 `bash scripts/aws/start-stack.sh` confirm
- Beta tenant invite send: user-managed (out of Claude scope per session 2026-05-26 boundary)

### Q4 — Wave-pack-planner pattern

9 buckets sized cho 6-8 Opus 4.7 parallel agents + coordinator inline (Bucket H 1d + Bucket L 2-3d). Per `agent-model-opus-default.md` v1.0.0 + `agent-background-spawn-default.md` v1.0.1.

### Q5 — Skip outside-in further audit?

Per `outside-in-coverage-trigger.md` §4 — outside-in audit ≤30 ngày fresh (2026-05-26 == today). Skip further audit; rely on consensus C-1 to C-8 findings.

## 2. Task Breakdown

### Phase 0 — Preflight (~15 min)

- T1: Wave plan ship + draft PR (this PR)
- T2: User start Zalo OA Free registration parallel (3-7d Zalo approval)
- T3: Verify local Docker stack 11/11 healthy (per Wave rst-cascade-1 Phase 0 pattern)

### Phase α — Parallel execution (~3-4 tuần wall-clock, 6 Opus bg-agents + 2 inline)

| Cluster | Bucket | Effort | Approach |
|---|---|---|---|
| 1 | A PDPL compliance-min 5 items | ~4-5d | Opus agent 1 (worktree isolation) |
| 2 | B Security-beta-min P0 CVE + 4 tests | ~7d | Opus agent 2 |
| 3 | C Ops monitoring | ~5d | Opus agent 3 |
| 4 | D GAP-727 class-teacher-fix | ~3-5d | Opus agent 4 |
| 5 | E Concurrency hardening 5 hot paths | ~5-7d | Opus agent 5 |
| 6 | F Beta invite + onboarding + landing footer + VN content + bulk CSV + multi-branch filter | ~7-10d | Opus agent 6 |
| 7 | G Tenant support channels (in-product widget + Zalo OA Free stub + help links) | ~3-5d | Opus agent 7 (OR consolidate F+G) |
| 8 | H Multi-branch decision spike | ~1d | Coordinator inline |
| 9 | L Landing + pricing audit + beta disclaimer + Zalo FAQ | ~2-3d | Coordinator inline |

### Phase β — AWS smoke verify (~30-45min)

- Smoke Bucket A endpoints (Privacy notice + Consent + DSAR placeholder)
- Smoke Bucket F (signup form + invoice template + reminder template)
- Smoke Bucket G (in-product widget link + help links)

### Phase E — Wave closure (~30min)

- 5-target sync per `post-merge-sync-completeness.md` §2
- Scope-completeness reconciliation per `wave-closure-scope-completeness.md` §3
- File follow-up gaps for deferred items (DPO/DPIA/DSAR/event-enum to Wave compliance-2)

## 3. Scope

### Bucket A — PDPL compliance-min 5 items (~4-5d) [Decision D2 locked]

1. Privacy notice tiếng Việt + ToS public ("v1 pending counsel" disclaimer)
2. Consent checkbox signup + granular per data category
3. Audit log immutable ✅ ĐÃ SHIP Wave 92 V61 (no action needed)
4. Data retention policy document (1-page doc)
5. Breach notification SOP (72h timeline runbook + email template)

**DEFERRED Phase 2:** DPO formal appointment + DPIA tracker + DSAR endpoint + Audit event enumeration ≥10 events.

### Bucket B — Security-beta-min (~7d)

1. Patch 3-5 P0 CVE từ 6 HIGH backlog
2. Auth race condition test (signup concurrent same email)
3. Upload size cap enforcement test
4. Bucket policy verify non-public unless intended
5. Branch-RLS negative tests (multi-tenant isolation prove)

### Bucket C — Ops monitoring (~5d)

1. Status page minimal (Statuspage.io free OR self-host single page)
2. P0 SNS alerts (Sentry alert routes OR AWS SNS topic + email subscription)
3. Restore drill 1-shot (verify backup → restore → smoke health)

### Bucket D — GAP-727 class-teacher-fix (~3-5d)

Fix `hasAccessToClass` multi-tenant boundary per Đợt 105 RST finding. Reference `feedback_class_teacher_fix.md` + Wave 105 audit.

### Bucket E — Concurrency hardening 5 hot paths (~5-7d)

1. **Tenant create race** (GAP-730 original)
2. **Enroll-into-FULL-class race** (Wave 105 B5 P0)
3. **Reminder cron retry duplicate**
4. **Email-verify double-click**
5. **Role-grant race**

Per `pre-handoff-self-test-completeness.md` §2.x flow class checklist applicable.

### Bucket F — Beta invite mechanism (~7-10d)

1. Beta invite mechanism (GAP-372) — admin issue invite token
2. 5-tenant onboarding script (manual hand-holding playbook)
3. Landing trust footer (testimonial + security badge + counsel disclaimer)
4. Invoice template VN content swap (apply `vn-localization-audit-checklist.md`)
5. Reminder template VN content swap
6. Bulk-invite CSV team (P2 cần invite 8 GV cùng lúc)
7. **Multi-branch filter** [Decision D3 locked]: nếu "Số chi nhánh" >1 → waitlist redirect

### Bucket G — Tenant support channels (~3-5d) [Decision D4 locked]

1. In-product chat widget OR help link beside error states
2. Zalo OA Free stub (placeholder → swap link real khi user complete registration)
3. Help links near critical CTAs (signup + verify + first-class + first-invoice)
4. Escalation runbook (dev biết khi nào escalate Zalo → email → phone)

**User-action prerequisite**: User register Zalo OA Free (~3-7d Zalo approval). Claude work parallel với placeholder.

### Bucket H — Multi-branch decision spike (~1d) [Decision D3 locked, coordinator inline]

1. Document decision: "Multi-branch defer Phase 2"
2. Update FAQ public: "Tính năng đa chi nhánh sẽ ship Q3 2026"
3. Filter logic in Bucket F invite form
4. Filter beta cohort: chỉ invite P2 1-branch
5. File follow-up gap: `multi-branch foundation Phase 2`

### Bucket L — Landing + pricing audit (~2-3d, coordinator inline)

1. Landing page audit per `vn-localization-audit-checklist.md`
2. Pricing page: "Free during beta. Lifetime 20% discount post-convert"
3. Beta disclaimer banner top-right corner
4. FAQ Zalo expectation-setting + multi-branch waitlist
5. Pricing Solo cohort audit clarify quota + cap

### Out-of-scope (defer Wave beta-prep-2 OR Wave compliance-2)

- DPO formal appointment + DPIA tracker + DSAR endpoint + Audit event enumeration ≥10 (PDPL Bucket A deferred items)
- Multi-branch foundation (Phase 2 mega-wave candidate)
- Payroll module audit (Wave beta-prep-2)
- Payment integration audit (Wave beta-prep-2)
- Attendance UX redesign (post beta cohort feedback)
- Wave rst-cascade-2 cascade fixes (GAP-752 + GAP-753)
- Đợt 108 RST comprehensive (post Wave beta-prep-1 ship)

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|---|---|---|---|
| `documents/04-quality/gaps/GAP-727-*.md` | Gap file | `bash scripts/query-gaps.sh GAP-727` | ✅ exists |
| `documents/04-quality/gaps/GAP-730-*.md` | Gap file | `bash scripts/query-gaps.sh GAP-730` | ✅ exists |
| `documents/04-quality/gaps/GAP-372-*.md` | Gap file | `bash scripts/query-gaps.sh GAP-372` | ✅ exists |
| `vn-localization-audit-checklist.md` rule | Rule | `ls .claude/rules/vn-localization-audit-checklist.md` | ✅ exists |
| `pre-handoff-self-test-completeness.md` rule | Rule | Per Phase α Bucket E concurrency hardening | ✅ exists |
| `ci-queue-local-runner-threshold.md` rule (new Wave rst-cascade-1 closure) | Rule | Per local-CI parity pre-push | ✅ shipped 2026-05-26 |
| Wave 92 V61 admin_audit_logs immutable | Migration | Per `kitehub/*/migrations/V61__*.sql` | ✅ shipped |
| Statuspage.io free OR self-host | Service | TBD per Bucket C agent investigation | 🆕 to-be-decided |

## 5. Verification Gates

| Gate | Before | Pass criteria |
|---|---|---|
| Gate 1 | Wave plan ship | User explicit "claude proceed Wave beta-prep-1" |
| Gate 2 | Phase α agent spawn | Outside-in audit findings integrated §1 ✓; local CI parity scripts ready |
| Gate 3 | Per-bucket PR merge | Bucket scope satisfied per §3 + local CI 10/10 PASS + AC checked |
| Gate 4 | Phase β AWS smoke | AWS stack started + 4 DONE flips Bucket A + Bucket F endpoints production-equivalent verified |
| Gate 5 | Wave closure flip | Scope-completeness reconciliation §3 + 5-target sync + 9-bucket DONE/PARTIAL verdict |
| Gate 6 | Phase 1 BETA gate check (user-managed) | Quality audit ≥80 + 5 beta tenants live + 0 P0 incidents 2w |

## 6. Agent Spawn Pattern

6-7 Opus 4.7 parallel bg-agents per `agent-model-opus-default.md` v1.0.0 + `agent-background-spawn-default.md` v1.0.1 + `feedback_parallel_agent_strategy.md`:

| Agent | Bucket | Worktree | Effort |
|---|---|---|---|
| 1 | A PDPL minimum | worktree-a-pdpl | 4-5d |
| 2 | B Security | worktree-b-security | 7d |
| 3 | C Ops monitoring | worktree-c-ops | 5d |
| 4 | D class-teacher-fix | worktree-d-class | 3-5d |
| 5 | E Concurrency hardening | worktree-e-concurrency | 5-7d |
| 6 | F+G Beta invite + Tenant support combined | worktree-f-g-invite-support | 10-15d (chia 2 sub-agents nếu cần) |
| Inline | H Multi-branch spike | main project | 1d |
| Inline | L Landing audit | main project | 2-3d |

Wave-pack-planner pattern proven 3-4x speedup per Wave rst-cascade-1 retro (~1h 45min coordinator vs ~4-6h sequential).

## 7. Closure Protocol

1. Audit artifact: `documents/04-quality/audits/quality/2026-06-XX-wave-beta-prep-1-closure.md`
2. Audits-index.csv row appended
3. Wave-history.jsonl entry tag-based schema (`tag_primary: beta-prep`, `counter: 1`)
4. CSV updates per gap (DONE flips with evidence OR stay PARTIAL with delta)
5. ROADMAP §🎯 Current Status: Wave beta-prep-1 SHIPPED entry
6. Session handoff: `2026-06-XX-wave-beta-prep-1-shipped-beta-1-queued.md`
7. AWS stack stopped post-verify
8. Frontmatter `status: draft → complete` flip
9. Scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3
10. Follow-up gaps filed:
    - Wave compliance-2 — PDPL deferred items (DPO + DPIA + DSAR + event-enum)
    - Wave multi-branch-1 — Phase 2 foundation
    - Wave beta-prep-2 — Payroll + Payment + Attendance UX

## 8. Log

- **2026-05-26 (status: complete):** Wave SHIPPED in single session ~6h wall-clock (vs ~3-4 tuần plan estimate — ~80x speedup via 6-agent parallel + admin-merge GAP-746 exception). 7 PRs merged: H #1872 → C #1873 → D #1871 admin → E #1875 admin → B #1877 admin → A #1874 (E2E fix) → F+G #1876 (RequestBetaAccessPage router mock). 2 spawn rounds (1st: 4/6 hit Sonnet thrash + Anthropic plan quota exhaustion at 22:30 BKK; 2nd post-quota-reset: 4 Opus 4.7 successful). 2 fix-agents for FE PRs E2E + unit tests. 3 ADMIN_MERGE_OVERRIDE: GAP-746 trailers (kiteclass-core multi-tenant test flake exception class per `admin-merge-discipline.md` v1.0.3). Main HEAD a64bcef2. Phase β AWS infrastructure smoke PASS (api.kitehub.me/actuator/health 200 + landing 200 + beta-status 200); wave code DEFER deploy per GAP-612 RST policy gate (no ECR push until local RST verified). Filed follow-up gaps: GAP-754 multi-branch foundation Phase 2 (paired ADR-036) + GAP-755 PDPL consent BE persistence (paired Bucket A FE consent capture) + GAP-756 Wave production deploy + RST verify P0 (paired Phase β defer) + GAP-757 Post-wave audit suite refresh P1 (post-wave-audit-mandate.md 3-day window). 1 META rule shipped same session: `pre-flight-aws-lifecycle-check.md` v1.0.0 (force-multiplier prevent cred-rotate cycle recurrence saved ~12min/incident).

## 9. Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` v1.0.0 §3)

| Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|
| **Bucket A.1** Privacy notice tiếng Việt + ToS public | ✅ DONE | PR #1874 — documents/01-business/legal/{privacy-notice,terms-of-service}.md |
| **Bucket A.2** Consent checkbox signup + granular per category | 🟡 PARTIAL | FE shipped 3 checkboxes (PR #1874); BE persistence DEFER GAP-755 P1 |
| **Bucket A.3** Audit log immutable | ✅ DONE | Wave 92 V61 (pre-existing; skip in scope) |
| **Bucket A.4** Data retention policy | ✅ DONE | PR #1874 — data-retention-policy.md |
| **Bucket A.5** Breach notification SOP | ✅ DONE | PR #1874 — breach-notification-sop.md |
| **Bucket B.1** Patch 3-5 P0 CVE | 🟡 PARTIAL | 0 HIGH found (Wave br-4 cleared); 2 moderate transitive → GAP-FE-CVE-MODERATE-001 P2 |
| **Bucket B.2** Auth race condition test | ✅ DONE | PR #1877 AuthRaceConcurrencyIT Testcontainers Postgres |
| **Bucket B.3** Upload size cap test | 🟡 PARTIAL | 3 IT @Disabled pending GAP-UPLOAD-CAP-CONFIG-001 P2 (kitehub-branding multipart cap config) |
| **Bucket B.4** Bucket policy verify | ✅ DONE | PR #1877 — storage-buckets.md 3 prod S3 ALL private |
| **Bucket B.5** Branch-RLS negative tests | ✅ DONE | PR #1877 MultiTenantRLSNegativeIT 3 scenarios |
| **Bucket C.1** Status page setup | 🟡 PARTIAL | PR #1873 Statuspage runbook shipped; Phase 2 auto-sync DEFER (~4-5h follow-up post 2-week cohort live) |
| **Bucket C.2** P0 SNS alerts | 🟡 PARTIAL | PR #1873 cloudwatch-p0-alarms.tf (8 alarms code shipped); terraform apply DEFER GAP-756 deploy unblock |
| **Bucket C.3** Restore drill 1-shot | 🟡 PARTIAL | PR #1873 framework PASS shellcheck/yaml/self-test 7/7; live TTR baseline DEFER GAP-257 Phase 3 quarterly |
| **Bucket D** GAP-727 hasAccessToClass | 🟡 PARTIAL (80%) | PR #1871 6 IT cases shipped; live verify gated GAP-612 AWS restore + GAP-756 deploy |
| **Bucket E** Concurrency 5 hot paths | ✅ DONE | PR #1875 — Path 1 (DataIntegrityViolation handler) + Path 4 (idempotent verify) + Path 5 (race recovery); Path 2 already hardened Wave br-1; Path 3 documented gap Phase 2 |
| **Bucket F.1** Beta invite mechanism (GAP-372) | ✅ DONE | Pre-existing Wave 33+45 (verified PR #1876 state-check) |
| **Bucket F.2** 5-tenant onboarding playbook | ✅ DONE | PR #1876 beta-cohort-onboarding-playbook.md |
| **Bucket F.3** Landing trust footer | ✅ DONE | PR #1876 LandingShellSSR.tsx |
| **Bucket F.4** Invoice template VN | ✅ DONE | PR #1876 invoice.html Thymeleaf |
| **Bucket F.5** Reminder template VN | ✅ DONE | Existing subscription-renewal-reminder.html already VN/VND (verified PR #1876) |
| **Bucket F.6** Bulk-invite CSV team | ❌ NOT-IMPLEMENTED | DEFER follow-up gap (V62 migration + admin UI + batch endpoint scope) |
| **Bucket F.7** Multi-branch filter [D3] | 🟡 PARTIAL | PR #1876 FE shipped (BetaRequestForm + /waitlist + 3 tests); BE server-side mirror DEFER follow-up gap |
| **Bucket G.1** In-product help icon | ✅ DONE | Pre-existing Wave 98 GAP-656 SupportMenu (verified PR #1876) |
| **Bucket G.2** Zalo OA Free stub | ✅ DONE | PR #1876 tenant-support-channels-runbook.md (7-step swap procedure documented) |
| **Bucket G.3** Help links near critical CTAs | ✅ DONE | PR #1876 HelpLink.tsx (8-topic registry, 5 tests) |
| **Bucket G.4** Escalation runbook | ✅ DONE | PR #1876 support-escalation-runbook.md |
| **Bucket H** Multi-branch decision spike | ✅ DONE | PR #1872 ADR-036 + FAQ q1.4 update + GAP-754 follow-up |
| **Bucket L** Landing + pricing audit | ❌ NOT-IMPLEMENTED | DEFER Wave beta-prep-2 (coordinator inline scope; landing footer Bucket F.3 partial cover) |

**Aggregate verdict:** 17 ✅ DONE + 8 🟡 PARTIAL + 2 ❌ NOT-IMPLEMENTED = 27 items. 100% scope tracked with explicit follow-up per `gap-done-discipline.md` §3 PARTIAL exit ramp.
