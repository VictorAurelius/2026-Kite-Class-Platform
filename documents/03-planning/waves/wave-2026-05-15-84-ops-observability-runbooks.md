---
title: Wave 84 — Ops Observability + Account-prep Runbooks + Secrets Rotation
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 84
waves: [84]
risk_profile: LOW-MEDIUM (ops infra, no user-facing change)
trigger: Wave 83 launch-blockers CLOSED; ops audit per `post-wave-audit-mandate.md` outstanding items
estimated_wall_clock: 10-14h
---

# Wave 84 — Ops Observability + Account-prep Runbooks

## 1. Brainstorm

**Q1 (goal):** Close ops readiness audit blockers — AWS observability baseline (CloudTrail multi-region + CloudWatch dashboard), automated secrets rotation, 4 missing account-prep runbooks, Vietnamese runbook overlays. Push Ops Readiness audit từ ~60/100 → 80/100 threshold cho Phase 1 BETA launch gate.

**Q2 (decision context):** Backend production-ready post-Wave-82/83 nhưng observability vẫn manual. Per `aws-observability-first.md` v1.0.0 — CloudTrail MUST be in place trước khi apply non-trivial infra; verify Wave 81 deploy đã có. Secrets Manager rotation 90-day policy chưa wired (GAP-379 50%). 4 account-prep runbooks (GAP-394) cho dev mới onboarding chưa hoàn tất. Vietnamese overlay GAP-423 (AWS SES) + GAP-424 (Statuspage) cho dev VN unfamiliar English.

**Q3 (risks):**
- Production incident without CloudTrail → can't reconstruct attacker actions (PDPL audit fail)
- Secrets stale → rotation gap → leak window unbounded
- Account-prep gaps → onboard dev mới tốn thời gian + có thể miss steps
- VN narrative miss per `dev-readable-doc-language.md` mandate

## 2. Task Breakdown

| Bucket | Item | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A** | GAP-437 CloudTrail multi-region + CloudWatch dashboard (per `aws-observability-first.md` Phase 2-3) | user-action AWS Console + coordinator verify | 2-3h | First (audit baseline) |
| **B** | GAP-379 Secrets Manager rotation 90-day automation (lambda + EventBridge schedule) | coordinator | 2-3h | Parallel A |
| **C** | GAP-394 4 missing account-prep runbooks (AWS account / Cloudflare / Resend / Vercel onboarding) | coordinator | 3-4h | Parallel |
| **D** | GAP-423 Vietnamese overlay cho AWS SES runbook | coordinator | 1h | Parallel C |
| **E** | GAP-424 Vietnamese overlay cho Statuspage/Instatus runbook | coordinator | 1h | Parallel C,D |
| **F** | GAP-431 Helm `startupProbe` templates (defer K8s migration Phase 2; minimal for Helm lint pass) | coordinator | 1h | Parallel |
| **G** | GAP-414 EC2 monthly right-sizing automation (CloudWatch alarm + cost report) | coordinator | 1h | Parallel |
| **H** | Post-wave Ops Readiness /100 audit refresh — target ≥80 | auditor coordinator | 1h | After A-G |

## 3. Scope — Bucket detail

### Bucket A — GAP-437 CloudTrail + CloudWatch dashboard

- Verify CloudTrail `kitehub-main` `IsLogging=true` (was set Wave 81 per `aws-observability-first.md` Phase 1)
- Add CloudWatch metric filters: failed IAM auth, root account use, security group changes, secrets access
- Dashboard `KiteHub-Production` với widgets: ALB request rate + 5xx rate, EC2 CPU/memory, RDS connections/IOPS, SES delivery rate, CloudTrail alerts
- Alarm thresholds + SNS topic → email/Slack notification

### Bucket B — GAP-379 Secrets rotation 90-day

- Lambda function `rotate-secret-handler` per AWS-managed rotation template
- EventBridge schedule: 90 days per secret
- Per-secret rotation strategy:
  - DB password: AWS Secrets Manager native RDS rotation
  - JWT/TOTP/STAFF secrets: in-place new value + dual-active 24h window
  - Cloudflare/Resend API keys: manual rotation runbook (`secrets-rotation-runbook.md` Wave 71)
- Test: trigger manual rotation 1 secret → verify services reload secret value

### Bucket C — GAP-394 4 missing runbooks

- `documents/05-guides/account-prep/02-aws-account-setup.md` (already DONE per audit)
- `documents/05-guides/account-prep/05-cloudflare-account-setup.md` (NEW) — DNS + Pages + DDoS
- `documents/05-guides/account-prep/06-resend-account-setup.md` (NEW) — DKIM + domain verify + API key
- `documents/05-guides/account-prep/07-vercel-account-setup.md` (NEW or rename existing) — project link + Pro upgrade path
- `documents/05-guides/account-prep/README.md` index update
- Per `docs-folder-structure.md` §3 README template

### Bucket D-E — Vietnamese overlay

- Add tiếng Việt quick-start section đầu mỗi runbook (3-5 paragraphs)
- Keep English technical identifiers per `dev-readable-doc-language.md` §3
- Side-by-side: tiếng Việt narrative + English commands

### Bucket F — GAP-431 startupProbe

- `infrastructure/helm/kitehub/templates/deployment.yaml` add startupProbe
- Failure threshold 30 × 10s = 5 min startup window cho Spring Boot apps
- Helm lint pass

### Bucket G — GAP-414 EC2 right-sizing

- CloudWatch alarm: CPU <20% avg over 7 days → flag candidate downsize
- Monthly cost report Lambda → SNS digest
- Manual review runbook trong `documents/05-guides/operations/ec2-cost-review.md`

### Bucket H — Ops Readiness /100 audit refresh

- Run `quality/ops-readiness-audit` skill — target ≥80 (vs ~60 baseline 2026-05-08)
- Gaps: CloudTrail + dashboard + alarms + rotation + runbooks all close → +20 points expected
- File any new finding gaps per `audit-to-gap-pipeline.md`

## 4. State-Check Evidence

| Symbol | Verification | Verdict |
|---|---|---|
| CloudTrail `kitehub-main` | `aws cloudtrail get-trail-status --name kitehub-main` IsLogging=true | ✅ exists (Wave 81) |
| CloudWatch dashboard `KiteHub-Production` | `aws cloudwatch list-dashboards` | 🆕 to-be-created |
| Secrets rotation Lambda | `aws lambda list-functions --query 'Functions[?starts_with(FunctionName,\`rotate-\`)]'` | 🆕 to-be-created |
| account-prep runbook list | `ls documents/05-guides/account-prep/*.md` | ✅ partial (2/7 exist) |
| Helm startupProbe | `grep startupProbe infrastructure/helm/kitehub/templates/deployment.yaml` | 🆕 to-be-added |

## 5. Acceptance Gate

| Criterion | Met when |
|---|---|
| GAP-437 CloudTrail audit | dashboard active, alarms wired, ≥3 metric filters firing test event |
| GAP-379 rotation automation | 1 secret rotation triggered + verified service reload |
| GAP-394 runbooks complete | 4 NEW runbooks shipped + README index updated |
| GAP-423/424 VN overlays | tiếng Việt section đầu each runbook |
| GAP-431 startupProbe | Helm lint pass with startupProbe present |
| Ops Readiness /100 ≥ 80 | audit report scored |

## 6. Cross-link

- Wave 83 closure: `wave-2026-05-15-83-hotfix-launch-blockers.md`
- `aws-observability-first.md` v1.0.0
- `post-wave-audit-mandate.md` §2.1 ops scope
- `dev-readable-doc-language.md` §2 VN narrative
- `docs-folder-structure.md` §3 README template

## 5. Verification Gates

See §5 Acceptance Gate table above — bucket-level criteria. Post-wave audit per `post-wave-audit-mandate.md` §2.1 (Backend/FE/Security/Performance categories) per bucket scope.

## 6. Agent Spawn Pattern

Sequential coordinator execution where buckets share files (deploy state, gateway config). Parallel background agents for isolated FE work (cookie consent banner, screenshots capture) per `agent-background-spawn-default.md` §1. Outside-in audit agents (per `outside-in-coverage-trigger.md` §3) spawn parallel background when wave triggers (Wave 85/86 mark §1 Q4).

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md`:
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- ROADMAP §🎯 Snapshot prepend
- gap-status.csv sync per bucket DONE flips
- `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- Session handoff `2026-05-XX-post-wave-NN-handoff.md` NEW
