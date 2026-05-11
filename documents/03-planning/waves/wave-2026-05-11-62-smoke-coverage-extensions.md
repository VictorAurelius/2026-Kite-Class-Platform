---
title: Wave 62 — Smoke test coverage extensions (GAP-475 P1+P2 cluster)
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [62]
gaps: [GAP-475]
---

# Wave 62 — Smoke test coverage extensions (GAP-475 6 sub-items)

**Goal:** Production-grade smoke test coverage gated by first beta tenant invite confidence per `release-deploy-standard.md` §3.4 PROD MAJOR subset.
**Trigger:** Post-Wave 61 smoke-test review (this session) identified 6 coverage gaps blocking beta launch; GAP-475 filed P1 audit-driven.
**Estimated wall-clock:** ~3h serial → 3 buckets parallel ~1h longest bucket.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA cutover gates (ROADMAP §🚀 step 4-7). All 5 personas affected (login + MFA + email all customer-facing). Smoke is gatekeeper for cutover step 7 final smoke.

**Q2 (trade-offs):**
- Considered: ship sub-items serially per gap order (rejected — 3 disjoint clusters possible)
- Considered: defer P2 (latency/migration/rollback) to Wave 63 (rejected — same files touched, breaks atomic shipping)
- Considered: combine all into 1 bucket (rejected — `smoke-test.sh` + `smoke-ses.sh` + new `smoke-rollback-cycle.sh` are disjoint files, parallel-safe)

**Q3 (risks):**
- Test mailbox setup (Sub-2) requires user-action (IMAP creds or Mailgun route) → ship script + env-gated, document setup in runbook
- Seed admin credentials (Sub-1) — env-gated `SMOKE_AUTH_USER/PASS`, reference `production-seed-runbook.md`
- Sub-3 MFA depends on Sub-2 email loop → same bucket (serial within Bucket B)
- Rollback cycle (Sub-6) — dry-run only locally, real exec deferred to real cutover dry-run

---

## 2. Task Breakdown

| Bucket | Gap(s) sub | Owner | Effort | Disjoint? |
|--------|-----------|-------|--------|-----------|
| A | GAP-475 Sub-1 + Sub-4 + Sub-5 | bg-agent | ~1h | ✅ `scripts/smoke-test.sh` (serial within bucket) |
| B | GAP-475 Sub-2 + Sub-3 | bg-agent | ~1h | ✅ `scripts/smoke-ses.sh` (sequential — MFA depends on email loop) |
| C | GAP-475 Sub-6 | bg-agent | ~45min | ✅ new file `scripts/smoke-rollback-cycle.sh` |

Disjoint check: A touches smoke-test.sh, B touches smoke-ses.sh, C creates new file. No file overlap.

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM (production confidence gate, scripts only — not customer-facing code path) → model: **Opus medium** per `feedback_sonnet_baseline_context_thrash.md`
**Cross-layer?:** NO (all DevOps/scripts, no FE+BE consumer pair) → skip Bucket 0 Foundation

| # | Bucket | Gap sub | Priority | Files (glob) | Spawn order |
|:-:|--------|---------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-475 Sub-1 + Sub-4 + Sub-5 | 🟠 P1 + 🟡 P2 + 🟡 P2 | `scripts/smoke-test.sh` | parallel |
| 2 | **B** | GAP-475 Sub-2 + Sub-3 | 🟠 P1 + 🟠 P1 | `scripts/smoke-ses.sh` + `documents/05-guides/deploy/email-ses-setup-runbook.md` §6 | parallel |
| 3 | **C** | GAP-475 Sub-6 | 🟡 P2 | `scripts/smoke-rollback-cycle.sh` (new) + `documents/05-guides/operations/incident-response-runbook.md` cross-link | parallel |

### Bucket A — smoke-test.sh extensions (auth + latency + migration)

- Files: `scripts/smoke-test.sh` (RELATIVE path)
- Acceptance:
  - Sub-1: `check_auth_happy_path` function added, env-gated `SMOKE_AUTH_E2E=1` + `SMOKE_AUTH_USER` + `SMOKE_AUTH_PASS`; POST seeded admin → JWT → protected GET 200; dry-run exit 0 (unset env → skip)
  - Sub-4: `check_latency_thresholds` function OR refactor existing checks to capture `time_total`; per-endpoint threshold map (actuator/health <500ms, public courses <1500ms, FE landing <2000ms); JSON output `smoke-latency-{ts}.json`
  - Sub-5: `check_migration_head` function added, env-gated `SMOKE_MIGRATION_VERIFY=1` + admin auth; query Flyway via gateway-proxied admin endpoint; assert latest version matches max V-prefix in `kitehub/kiteclass-core/src/main/resources/db/migration/`
- Verify: `bash scripts/smoke-test.sh https://example.invalid https://example.invalid` (URL won't resolve; expect graceful failure with exit 1, no crash); dry-run all 3 new functions exit 0 unset

### Bucket B — smoke-ses.sh extensions (email loop + MFA)

- Files: `scripts/smoke-ses.sh` + `documents/05-guides/deploy/email-ses-setup-runbook.md` (new §6.X subsection)
- Acceptance:
  - Sub-2: `send_receive_email_e2e` function: env-gated `SMOKE_EMAIL_E2E=1` + `SMOKE_EMAIL_RECIPIENT` + `SMOKE_EMAIL_IMAP_HOST/USER/PASS` (or `SMOKE_EMAIL_MAILGUN_API_KEY`); `aws ses send-email` template → poll IMAP/Mailgun events for receipt within 5min timeout; assert subject + body content
  - Sub-3: `verify_mfa_otp_e2e` function (depends on Sub-2): trigger signup via `curl POST /api/auth/register` → poll mailbox via Sub-2 helpers for OTP email → regex extract OTP → POST `/api/auth/verify-email` → assert 200
  - Runbook §6.X documents test mailbox setup (Mailgun route OR dedicated `smoke@kitehub.me` IMAP)
- Verify: dry-run exit 0 with unset env (skip both); document required env in script header

### Bucket C — smoke-rollback-cycle.sh (new file)

- Files: `scripts/smoke-rollback-cycle.sh` (CREATE) + `documents/05-guides/operations/incident-response-runbook.md` (add cross-link)
- Acceptance:
  - Sub-6: New script ≥150 LOC; flow = (1) capture current deploy SHA via `gh api`, (2) trigger rollback via `gh workflow run rollback.yml -f target_sha=<previous>`, (3) wait for workflow complete + health-check, (4) run `smoke-test.sh`, (5) restore forward via `gh workflow run` again, (6) re-smoke; report time-to-recovery in JSON `rollback-cycle-{ts}.json`
  - `--dry-run` flag default; only `--execute` actually triggers workflows
  - Env-gated `ROLLBACK_CYCLE_E2E=1` for real exec
  - Runbook cross-link added at incident-response §"Rollback validation cadence"
- Verify: `bash scripts/smoke-rollback-cycle.sh --dry-run` exit 0; documents required env

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `scripts/smoke-test.sh` | Existing script | `ls scripts/smoke-test.sh && wc -l` | 636 LOC | ✅ exists (Bucket A extends) |
| `scripts/smoke-ses.sh` | Existing script | `ls scripts/smoke-ses.sh && wc -l` | 197 LOC | ✅ exists (Bucket B extends) |
| `scripts/smoke-rollback-cycle.sh` | New script | `ls scripts/smoke-rollback-cycle.sh` | not found | 🆕 to-be-created (Bucket C) |
| `documents/05-guides/operations/incident-response-runbook.md` | Existing runbook | `ls documents/05-guides/operations/incident-response-runbook.md` | exists | ✅ exists (Bucket C cross-link) |
| `documents/05-guides/deploy/email-ses-setup-runbook.md` | Existing runbook | `ls documents/05-guides/deploy/email-ses-setup-runbook.md` | 580 LOC | ✅ exists (Bucket B §6.X) |
| `gh workflow run rollback.yml` | Workflow | `ls .github/workflows/rollback.yml` | TBD verify in Bucket C state-check | 🆕 verify-or-create (Bucket C fallback: file follow-up gap if workflow absent) |
| `check_health` / `check_page` / `check_api_json` (existing functions) | smoke-test.sh fn | `grep -n "^check_" scripts/smoke-test.sh` | 10 functions | ✅ exists |
| Flyway migration dir | Path | `ls kitehub/kiteclass-core/src/main/resources/db/migration/V*.sql \| wc -l` | verified in Bucket A | ✅ exists |

Banned shortcuts respected: no `| head`, no aspirational refs without 🆕 flag.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash -n scripts/smoke-test.sh && shellcheck scripts/smoke-test.sh` (syntax + lint) | ShellCheck CI job |
| B | `bash -n scripts/smoke-ses.sh && shellcheck scripts/smoke-ses.sh` | ShellCheck CI job |
| C | `bash -n scripts/smoke-rollback-cycle.sh && shellcheck scripts/smoke-rollback-cycle.sh && bash scripts/smoke-rollback-cycle.sh --dry-run` | ShellCheck CI job |

No backend/frontend code changes → no mvn/pnpm verify needed.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 3 buckets spawned with `run_in_background: true` + `isolation: worktree`
- Model: Opus medium (per stake-tier MEDIUM per `feedback_sonnet_baseline_context_thrash.md`)
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all 3 background completions
- Each agent runs ShellCheck locally before declaring done; coordinator checks CI on PR

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates GAP-475 Log + completion_pct in CSV
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append in closure PR
- Sub-items deferred (e.g. workflow missing → Sub-6 PARTIAL) tracked per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged
- **`## Release Plan Progress` section in closure PR** — Wave 62 contribution to Phase 1 BETA step 7 (final smoke gate); Waves Remaining table

---

## 8. Log

- **2026-05-11** (draft): Plan created. GAP-475 6 sub-items decomposed → 3 parallel buckets (A: smoke-test.sh extensions, B: smoke-ses.sh extensions, C: new rollback-cycle script). Stake MEDIUM → Opus medium. Cross-layer NO → skip Bucket 0.
- **2026-05-11** (complete): Wave SHIPPED. 3 parallel Opus-medium agents → PR #1183 (Bucket A, +294 LOC) + #1184 (Bucket B, +229+97 LOC) + #1185 (Bucket C, +291+15 LOC). All shellcheck clean. 4/6 sub-items functional; Sub-5 (Flyway HTTP endpoint absent) + Sub-6 (rollback.yml absent) PARTIAL with follow-up GAP-476 + GAP-477 filed. GAP-475 → PARTIAL 75%. Wall-clock ~4min agent sum (max ~4min Bucket C). Streak: 96 consecutive 0-clarification. State-check discipline win: Sub-3 MFA adapted to actual link-based verify pattern (`?token=<UUID>` per AuthController) not assumed 6-digit OTP — agent followed BE source-of-truth per `audit-to-gap-pipeline.md` §2.5.
