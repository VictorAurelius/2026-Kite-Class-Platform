# GAP-693: AWS rebuild SOP playbook (13 bước + 5 gates + 8 failure-mode prevention)

**Status:** 🔵 OPEN (BLOCKED — depends on GAP-612 resolve + GAP-694 DONE + GAP-692 Phase 1 DONE)
**Priority:** 🔴 P0 (META — single SOP eliminates 8 prior failure-mode classes)
**Domain:** DevOps + Meta
**Detected:** 2026-05-21
**Related PRs:** TBD
**Related Docs:** Outside-in audit synthesis 2026-05-21; GAP-612; GAP-694; GAP-692

## Current State (verified 2026-05-21 via 3 parallel outside-in agents)

> Per `audit-to-gap-pipeline.md` §2.5 state-check. Outside-in agents F-1 (failure-mode matrix) + E-1 (external benchmark) + P-1 (persona simulation) completed 2026-05-21. Multiple rules + scripts đã ship cover INDIVIDUAL failure modes — chưa có CONSOLIDATED playbook integrate all.

### Existing pieces (DO NOT duplicate)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Release deploy standard | `.claude/rules/release-deploy-standard.md` v1.2.0 | ✅ shipped — §4.4 rollback, §9 agent role matrix |
| Concurrent mutation guard | `.claude/rules/concurrent-production-mutation-ops.md` | ✅ shipped Wave 65 |
| Pre-mutation state-check | `.claude/rules/pre-mutation-state-check.md` v1.2.0 | ✅ shipped — §3.5 plan-vs-predicted reconciliation |
| Release fix retry budget | `.claude/rules/release-fix-retry-budget.md` v1.1.0 | ✅ shipped |
| Terraform apply retry | `.claude/rules/terraform-apply-retry-reconfirm.md` | ✅ shipped |
| Pre-handoff self-test completeness | `.claude/rules/pre-handoff-self-test-completeness.md` v1.1.1 | ✅ shipped — §2.4 admin-flow checklist |
| AWS observability first | `.claude/rules/aws-observability-first.md` | ✅ shipped (CloudTrail BEFORE Phase 2.3) |
| Terraform targeted apply phased | `.claude/rules/terraform-targeted-apply-phases.md` (memory) | ✅ shipped — 6-phase pattern |
| Release deploy SOP runbook | `documents/05-guides/deploy/release-1-deploy-plan.md` + `release-1-deploy-runbook.md` | ⚠️ partial — Phase 1 BETA specific, not generic rebuild |
| Post-deploy smoke script | `scripts/smoke-test.sh` | ✅ shipped GAP-377 |
| Rollback workflow | `.github/workflows/rollback.yml` + `scripts/smoke-rollback-cycle.sh` | ✅ shipped Wave 63 GAP-477 |
| Bootstrap state backend runbook | `documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md` | ✅ shipped Wave 43 |
| **Consolidated rebuild playbook** | `documents/05-guides/operations/aws-rebuild-runbook.md` | ❌ missing — this gap delta |
| **Single bootstrap script** | `scripts/aws/bootstrap-new-account.sh` (idempotent 6-phase) | ❌ missing |
| **Plan-vs-predicted auto-classifier** | `scripts/aws/terraform-plan-classify.sh` | ❌ missing |
| **Pre-invite production smoke gate** | `scripts/aws/preflight-invite.sh` (SES + DKIM + smoke + role-guard parity) | ❌ missing |

**Grep commands run:**
```bash
find documents/05-guides -iname "*rebuild*"  # 0 results — generic rebuild runbook missing
ls scripts/aws/ 2>/dev/null  # existing: start-stack.sh, stop-stack.sh per CLAUDE.md
find scripts -name "bootstrap*"  # existing: bootstrap-and-verify.sh (account-specific, not rebuild SOP)
```

## Problem

Rebuild AWS từ scratch (Item 3 user-flagged 2026-05-21) cần consolidated playbook integrate:
- 13 sequenced steps (~2h cold-start target)
- 5 mandatory gates (pre-bootstrap dry run, local stack health, local smoke E2E, pre-apply reconciliation, pre-invite production smoke)
- 8 failure-mode prevention checks (from outside-in synthesis)

Hiện tại knowledge phân tán across 12+ rule files + memory entries + audit artifacts. Solo dev mệt mỏi sau 8h session (per persona simulation Step 1 drop-off) sẽ skip cross-reference 12 rules → miss Class 8 CloudTrail-baseline OR Class 4 config-drift sweep.

Per outside-in failure-mode matrix synthesis 2026-05-21 top-3 ranked classes:
1. **Class 4 — Config-shaped value code drift** (eliminated by GAP-692 prerequisite)
2. **Class 8 — Audit baseline before infra apply** (`aws-observability-first.md` rule)
3. **Class 2 — Postgres-specific bug invisible to CI** (smoke admin-login §3.1)

## Context

User-chosen sequencing 2026-05-21: Phase 0 local self-test (GAP-694) → Item 2 refactor (GAP-692) → rebuild. This gap = step 3 execution playbook. **CANNOT execute until:**
- GAP-612 resolved (account #1 fate determined OR account #2 LEGIT path chosen)
- GAP-694 DONE (local smoke working — required by Gate 3 in this SOP)
- GAP-692 Phase 1 DONE (env-reference.yaml shipped — required by Gate 4 reconciliation)

External benchmark (Agent E-1 2026-05-21): industry standard SaaS rebuild SOP from OneUptime + AWS Cutover Runbook Guide + HashiCorp Terraform DR + Spacelift + 12factor.net = `~13 steps, ~2h cold-start, blue-green optional`.

## Evidence

- Outside-in audit synthesis 2026-05-21 — 3 agent reports (failure-mode matrix + external benchmark + persona simulation)
- 8 failure-mode classes documented với prior incident citations (Wave 60-91 audit artifacts)
- 5-gate design from persona simulation (Dev rebuild flow drop-offs eliminated)
- 13-step SOP template from external benchmark (Heroku/Render/Fly.io/Vercel rebuild patterns)

## Proposed Fix

### Phase 1 — Ship rebuild playbook doc (~1 day)

`documents/05-guides/operations/aws-rebuild-runbook.md` v1.0 với:

**§1 — TL;DR + applicability** (when to use this runbook; out-of-scope)

**§2 — Pre-flight checklist** (account ready + state backend + GAP-612 resolved + GAP-694 + GAP-692 Phase 1 done)

**§3 — 13-step SOP với time-box mỗi bước:**

```
0.  Bootstrap script (scripts/aws/bootstrap-new-account.sh, single idempotent)
1.  (5m)  Verify TF S3 backend + DynamoDB lock alive
2.  (10m) terraform plan + §3.5 reconciliation table per pre-mutation-state-check.md
3.  (15m) Targeted apply Phase 1: CloudTrail + S3 audit bucket (BEFORE prod) — Class 8 prevention
4.  (20m) Targeted apply Phase 2: VPC + SG + IAM + RDS
5.  (15m) Targeted apply Phase 3: EC2 + ALB + Secrets + Route53
6.  (10m) DNS propagate verify (dig from 1.1.1.1 + 8.8.8.8 — 2 resolvers)
7.  (5m)  ACM cert ISSUED + ALB :443 listener attached — Class 6 prevention
8.  (15m) Local smoke first: ./scripts/up.sh + scripts/local/smoke-e2e.sh PASS — Class 5 prevention
9.  (10m) Deploy image: deploy-production.yml (concurrent-mutation-ops.md serialization)
10. (5m)  Health probes sequencing /actuator/health → /api/status → / — Class 7 prevention
11. (5m)  Smoke admin-login per release-deploy-standard.md §3.1 — Class 2 prevention
12. (5m)  Synthetic 5 user flows: signup/login/dashboard/API/logout
13. (T+24h ongoing) Monitor CloudWatch error rate + P95 latency
```

**§4 — 5 mandatory gates (BLOCK execution if fail):**

- Gate 1 (before Step 0): `scripts/aws/bootstrap-dry-run.sh` — verify admin key permissions + Free Tier quota + CF API token
- Gate 2 (before Step 8): `scripts/local/preflight-stack.sh` PASS — local stack healthy (per GAP-694)
- Gate 3 (before Step 9): `scripts/local/smoke-e2e.sh` PASS — Playwright headless P2+P3+admin flows green (per GAP-694)
- Gate 4 (before Step 9 `dry_run=false`): `scripts/aws/terraform-plan-classify.sh` + audit artifact under `documents/04-quality/audits/aws-verification/YYYY-MM-DD-plan-reconciliation.md` per pre-mutation-state-check.md §3.5
- Gate 5 (before sending beta invite): `scripts/aws/preflight-invite.sh` PASS (SES out-of-sandbox + DKIM resolve + test-send delivered + role-guard parity grep clean) per pre-handoff-self-test-completeness.md §2.4

**§5 — 8 failure-mode prevention checklist** (per F-1 synthesis — each class with specific verifiable command)

**§6 — Smoke test minimum (7 items per E-1 benchmark)**

**§7 — Rollback procedure** (reference release-deploy-standard.md §4.4 + rollback.yml workflow)

**§8 — Troubleshooting matrix** (cross-link prior incidents Wave 60-91)

**§9 — Self-test artifact** (worked example — apply runbook retroactively to Wave 88 cutover incident; verify gates would have caught Wave 71b role-guard mismatch + Wave 64 IAM cascade + Wave 65 SSM-terraform race)

### Phase 2 — Ship 4 missing automation scripts (~2 days)

- `scripts/aws/bootstrap-new-account.sh` — single idempotent 6-phase orchestrator (state backend → OIDC role → CloudTrail → core infra → secrets → smoke)
- `scripts/aws/bootstrap-dry-run.sh` — Gate 1 pre-flight (admin key + Free Tier quota + CF API token + DNS pre-check)
- `scripts/aws/terraform-plan-classify.sh` — Gate 4 auto-classifier (parse `terraform plan -json` output; classify each resource Real/Phantom/Backlog; require explicit acknowledge before `dry_run=false`)
- `scripts/aws/preflight-invite.sh` — Gate 5 pre-invite (SES quota + DKIM + smoke admin-login + role-guard parity grep)

### Phase 3 — Codify into rule (~30 min)

`.claude/rules/aws-rebuild-sop-mandate.md` v1.0.0:
- Mandate rebuild runbook §3 execution
- 5 gates mandatory pass before next step
- Override trailer `AWS_REBUILD_GATE_SKIP: <gate-N> — <reason + follow-up gap>`
- Pattern frequency >1/month skip triggers meta-review

### Phase 4 — Self-test (~1 hour)

Apply runbook retroactively to Wave 88 cutover incident:
- Gate 3 local smoke would have caught FE rebuild `NEXT_PUBLIC_API_URL=localhost:9000` instead of `https://api.kitehub.me` (Hotfix #1465 root cause)
- Gate 4 plan reconciliation would have caught EC2 force-replace from Wave 37 backlog 9 ngày un-applied
- Gate 5 pre-invite would have caught CORS `https://app.kitehub.me` 403 pre-deploy

Document worked example trong runbook §9.

## Acceptance Criteria

- [ ] Phase 1 — `documents/05-guides/operations/aws-rebuild-runbook.md` v1.0 shipped (§1-§9 sections)
- [ ] Phase 2 — `scripts/aws/bootstrap-new-account.sh` idempotent + tested cold start ≤45 min
- [ ] Phase 2 — `scripts/aws/bootstrap-dry-run.sh` Gate 1 implemented
- [ ] Phase 2 — `scripts/aws/terraform-plan-classify.sh` Gate 4 implemented + auto-fills reconciliation table
- [ ] Phase 2 — `scripts/aws/preflight-invite.sh` Gate 5 implemented (SES + DKIM + smoke + parity)
- [ ] Phase 3 — `.claude/rules/aws-rebuild-sop-mandate.md` v1.0.0 codified với override mechanism
- [ ] Phase 4 — Self-test: runbook applied retroactively to Wave 88 incident; documents 3 specific gates would have caught real prior bugs
- [ ] Integration verified — all 12 existing rules cross-referenced trong runbook §5 failure-mode prevention table
- [ ] Total rebuild time-box verified ≤2h cold-start (excluding T+24h monitoring) qua real execution OR fixture run
- [ ] Update `release-deploy-standard.md` §9 with reference to this runbook (link from agent role matrix)

## Related

- **GAP-612** AWS account suspension (trigger event; rebuild gated on resolve)
- **GAP-694** local self-test investigation (prerequisite — Gate 2/3 require working local stack)
- **GAP-695** self-test readiness comprehensive plan (parent catalog cho local self-test execution; Gate 2 local stack health + Gate 3 local smoke E2E reference GAP-695 4-tier plan)
- **GAP-692** env-reference.yaml refactor (prerequisite — Gate 4 plan classifier reads env vars)
- `.claude/rules/release-deploy-standard.md` v1.2.0 (existing deploy standard; this gap = specialization for rebuild scope)
- `.claude/rules/concurrent-production-mutation-ops.md` (Class 1 prevention — Gate 4 + Step 9)
- `.claude/rules/pre-mutation-state-check.md` v1.2.0 §3.5 (Class 6 prevention — Gate 4 reconciliation)
- `.claude/rules/release-fix-retry-budget.md` v1.1.0 (apply during rebuild if fail surfaces)
- `.claude/rules/terraform-apply-retry-reconfirm.md` (apply if retry needed)
- `.claude/rules/aws-observability-first.md` (Class 8 prevention — Step 3 CloudTrail FIRST)
- `.claude/rules/pre-handoff-self-test-completeness.md` v1.1.1 §2.4 (Class 5 + Gate 5 admin-flow checklist)
- `meta-gap-priority.md` §3 — META P0 force-multiplier
- Outside-in audit synthesis 2026-05-21 (3 agent reports — F-1 + E-1 + P-1)
- External benchmarks: AWS Cutover Runbook Guide, OneUptime Migration Runbook 2026, HashiCorp Terraform DR, 12factor.net §X dev/prod parity

## Log

- **2026-05-21** — Gap filed from outside-in audit synthesis. BLOCKED state explicit: cannot execute until GAP-612 resolved + GAP-694 DONE + GAP-692 Phase 1 DONE. Synthesis consolidated 8 failure-mode classes (from F-1 agent) + 13-step SOP (from E-1 external benchmark) + 5 gates (from P-1 persona simulation) into single playbook design. Cross-references 12 existing rules — runbook integrates rather than duplicates. Self-test deferred to Phase 4 post-implementation.
