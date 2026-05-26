---
title: Wave beta-prep-1 — 9 bucket mega-wave path-to-beta-launch
status: draft
created: 2026-05-26
updated: 2026-05-26
audience: dev
tag_primary: beta-prep
tags_secondary: [phase-1-beta, pdpl-deadline-2026-07-01, outside-in-audited, parallel-execution]
counter: 1
date_launch: 2026-05-27
waves: [beta-prep-1]
gaps: [GAP-727, GAP-730, GAP-372]
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

- **2026-05-26 (status: draft):** Wave plan created. Source = 3-Opus outside-in audit consensus (persona + benchmark + failure-mode) + user decisions D1-D5 locked. 9 buckets ~3-4 tuần wall-clock (parallel) targeting PDPL hard deadline 2026-07-01 with 8-15d buffer. Per `wave-tag-numbering-convention.md` v1.0.0 tag-based schema: `tag_primary: beta-prep`, `counter: 1`. Waiting user explicit "claude proceed Wave beta-prep-1" before Phase α agent spawn (next session per session-end-context-check.md heads-up zone 76%).
