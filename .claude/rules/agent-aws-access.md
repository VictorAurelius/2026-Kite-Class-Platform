---
paths:
  - "infrastructure/**/*.tf"
  - "scripts/aws/**"
  - "documents/04-quality/audits/aws-verification/**"
  - ".github/workflows/*aws*.yml"
---

# Agent AWS Access — read-only allowlist + mandatory logging

**Priority:** 🟠 MANDATORY — bounds blast radius for agent AWS interactions
**Version:** 1.0.2
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (allowlist + log requirement + self-test) per §6.5; new constraint, no constraint loosening; paired same-PR with first audit artifact `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md`)
**Applies to:** Every Claude session that issues `aws` CLI commands, `curl` against AWS-hosted endpoints, or terraform actions affecting AWS account 906286017800. Scope includes Bash tool invocations + sub-agents.

---

## 1. The Rule

> **Agent AWS interactions follow 3 tiers: (1) Allowed read-only commands run freely + logged; (2) Always-confirm commands need explicit user approval per call; (3) Banned mutation commands are never run by agent — user executes manually.**
>
> **Every read-only verification session MUST save output to `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>.md` for audit trail.**

This rule operationalizes `release-deploy-standard.md` §9 ("Post-deploy verification = ✅ ADOPT — agent runs smoke test scripts, parses logs"). §9 states agent role at high level; this rule provides the command-level boundary.

---

## 2. Tier 1 — Allowed read-only commands (run freely + log)

Agent may run without per-command confirmation. Output MUST be logged per §5.

### 2.1 Allowed AWS CLI verbs (prefix-matched)

| Verb prefix | Examples | Notes |
|---|---|---|
| `describe-*` | `describe-instances`, `describe-db-instances`, `describe-trails` | Always read-only |
| `list-*` | `list-secrets`, `list-buckets`, `list-clusters`, `list-roles` | Read-only metadata |
| `get-*` (with exceptions) | `get-caller-identity`, `get-trail-status` | See §2.2 banned `get-*` exceptions |
| `head-*` | `head-bucket`, `head-object` | Existence checks |

### 2.2 Banned `get-*` despite prefix (data exfil risk)

Even though `get-*` is mostly read-only, these reveal secret material → must be Tier 2 (always-confirm):

- `aws secretsmanager get-secret-value` (returns secret string)
- `aws ssm get-parameter --with-decryption` (returns decrypted secret)
- `aws kms decrypt` (decrypts ciphertext)
- `aws sts get-session-token` (issues credentials)
- `aws iam get-access-key-last-used` (surface for credential mining)
- `aws s3 cp s3://...` (downloads object — could be secret)
- `aws s3 sync s3://...` (downloads tree)

### 2.3 Allowed network probes

Read-only HTTP/HTTPS GET against AWS-hosted endpoints:
- `curl -sI` (HEAD only) — preferred for endpoint presence checks
- `curl -s -o /dev/null -w "%{http_code}"` — status code only
- `dig +short` — DNS resolution

Banned:
- `curl -X POST/PUT/DELETE` against AWS endpoints (write operation)
- Full body download of unknown resources (data exfil)

---

## 3. Tier 2 — Always-confirm commands (explicit user approval per call)

Agent MUST request user confirmation via `AskUserQuestion` before each call. Examples:

- Anything in §2.2 (secret-revealing read)
- `aws cloudtrail get-trail-status` on production trails (low-risk but worth confirming first time per session)
- `aws ec2 describe-instances` with `--filters` returning > 50 instances (cost of API call)
- Data-events on CloudTrail (cost: $0.10/100k)

Confirmation pattern:
```
Run `aws secretsmanager get-secret-value --secret-id kite/prod/jwt-secret`?
This returns the secret value to terminal/logs. Confirm?
```

---

## 4. Tier 3 — Banned mutation commands (never run by agent)

Agent does NOT run these. User runs manually OR via documented terraform workflow.

### 4.1 Banned by verb prefix

| Verb prefix | Why banned |
|---|---|
| `create-*` | Creates resources, incurs cost |
| `delete-*` | Removes resources, may be irreversible |
| `put-*` | Writes data (S3, secrets, parameter store) |
| `update-*` | Modifies config |
| `modify-*` | Same |
| `terminate-*` | Stops/destroys instances |
| `start-*`, `stop-*`, `reboot-*` | Lifecycle actions affecting availability |
| `restore-*` | Cross-account / cross-region restore (data movement) |
| `attach-*`, `detach-*` | Resource topology changes |
| `assume-role` (via CLI direct) | Credential acquisition (terraform handles correctly via env) |

### 4.2 Specific high-risk commands

- `aws s3 rb` (remove bucket)
- `aws s3 rm` (delete object)
- `aws iam create-access-key` (long-lived credential creation)
- `aws iam attach-role-policy` (privilege escalation)
- `aws ec2 authorize-security-group-ingress` (network exposure)
- `aws rds create-db-snapshot` (cost + may include secrets)
- `aws cloudtrail stop-logging` (audit blind spot)

### 4.3 Banned terraform actions (agent-initiated only)

Per `release-deploy-standard.md` §9 (revised v1.0.1, 2026-05-08), the bans below scope to **AGENT-INITIATED apply only**. **User-triggered `workflow_dispatch` + confirm input "APPLY" verbatim + human-click is permitted** — preserves human cognitive checkpoint via confirm-input gate while delivering ephemeral OIDC security advantages over local admin-key apply (industry standard Atlantis/TF Cloud pattern). See `release-deploy-standard.md` §9 matrix for the 3-case distinction.

Agent-initiated bans (unchanged from v1.0.0):

- `terraform apply` autonomously by agent — BANNED per `feedback_terraform_apply_retry_reconfirm.md`
- `terraform destroy` ALWAYS user-only (mass deletion) — agent never runs
- `terraform import` (state mutation) — banned
- `terraform state rm/mv/push` (state surgery) — banned

User-triggered (allowed, document inline when invoked):

- `workflow_dispatch` apply via `.github/workflows/terraform-apply.yml` — human-click + confirm input + ephemeral OIDC creds (per Wave 44 GAP-449)
- One-time local `terraform apply` for chicken-and-egg bootstrap — admin key required, rotate immediately after

`terraform plan` is Tier 1 (read-only).
`terraform init` Tier 1 (no AWS state mutation, just provider download).

---

## 5. Logging requirement — every verification session

Every Tier 1 multi-command verification session MUST result in a saved artifact:

```
documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>.md
```

Where `<topic>` is the verification scope (e.g. `phase-2-3-post-apply`, `secrets-populate-check`, `pre-deploy-smoke`).

### 5.1 Required artifact sections

```markdown
---
title: AWS Verification — <topic>
status: complete
created: YYYY-MM-DD
phase: <e.g. 2.3, 4>
---

# AWS Verification Report — <topic>

## Scope
<What was checked, why>

## Commands run
<Each command + brief purpose>

## Results
<Per-resource state + endpoint check>

## Findings
<Anomalies, concerns, follow-ups>

## Next steps
<Recommended actions>
```

### 5.2 Folder hygiene

- Folder `documents/04-quality/audits/aws-verification/` created with `README.md` index
- Each artifact's filename: `YYYY-MM-DD-<topic>.md`
- No personally-identifying data: avoid emails, IP addresses of unknown parties; OK to log own AWS account ID 906286017800 in this folder (already public per repo)
- Secret values NEVER logged (rule §2.2 ensures `get-secret-value` not run by agent)

### 5.3 When logging not required

- Single ad-hoc command for immediate troubleshooting (1-2 commands, no follow-up)
- Output already in PR body / commit message (don't double-log)
- Conversation pivots away before complete verification

Default to log unless clearly transient.

---

## 6. Override mechanism

Genuine exception (e.g. mid-incident response, no time to file artifact):

```
git commit -m "...
AWS_VERIFICATION_OVERRIDE: <reason — e.g. P0 incident triage>
AWS_VERIFICATION_FOLLOWUP: <link to artifact filed within 48h>"
```

Trailer logged in quarterly retro. Pattern frequency >5% triggers meta-review.

---

## 7. Worked self-test — apply rule to 2026-05-08 session

User-flagged commands from Phase 2.3 retro session:

| Command | Tier | Verdict |
|---|---|---|
| `curl -sI https://kitehub.vercel.app/` | Tier 1 (network probe) | ✅ allowed |
| `aws ec2 describe-instances --query ...` | Tier 1 (`describe-`) | ✅ allowed |
| `aws rds describe-db-instances --query ...` | Tier 1 (`describe-`) | ✅ allowed |
| `aws sts get-caller-identity --profile default` | Tier 1 (allowed `get-*`) | ✅ allowed |
| `aws cloudtrail describe-trails` | Tier 1 (`describe-`) | ✅ allowed |
| `aws cloudtrail get-trail-status` | Tier 2 (always-confirm first time) | ⚠️ confirm |

Verdict: 5/6 commands ✅ allowed at Tier 1; 1 was Tier 2 borderline (no harm, status check). **No banned commands run.** ✓ rule fires correctly on the original session.

Logging gap: report was conversation-only, NOT saved to repo. ❌ rule §5 violated. Remediation: same-PR artifact `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md` saves the verification.

→ Self-test PASS for command tiering, FAIL→FIX for logging requirement (artifact ships same PR).

---

## 8. Enforcement

### 8.1 Pre-merge reviewer checklist

When reviewing a PR that contains AWS-touching agent activity:
- [ ] Any new Bash tool invocations match Tier 1 list?
- [ ] If Tier 2 used, was AskUserQuestion called?
- [ ] Tier 3 commands absent (or executed by user, not agent)?
- [ ] Verification artifact saved if multi-command session?

### 8.2 Memory auto-load

`feedback_agent_aws_readonly_logging.md` (Phase 4 follow-up of GAP-438) — loads each session, reinforces tiering + logging.

### 8.3 Skill paired (Phase 2 follow-up)

`.claude/skills/devops/aws-smoke-test/SKILL.md` + `scripts/smoke-aws-phase-N.sh` — codified verification pattern. Skills auto-trigger on AWS keywords.

### 8.4 Audit gate hook (deferred)

Future: `audit-gate.py` AUDIT_RULES rule scanning Bash invocations for Tier 3 patterns → BLOCK. Defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard.

---

## 9. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Run `aws *` ad-hoc free-form | Match against Tier 1/2/3 list first |
| Skip logging "small check" verification | Log to `aws-verification/` even for 2-command sessions if findings non-trivial |
| Use `aws secretsmanager get-secret-value` to "verify secret populated" | Use `aws secretsmanager describe-secret` (returns metadata, not value) |
| Run `curl -X POST` to test endpoint creation | User runs creation manually; agent verifies via `curl -sI` (HEAD only) |
| Bundle Tier 3 in Bash chain (`aws describe-x && aws delete-x`) | Each Tier 3 command = explicit user approval per call |
| Save audit artifact in non-canonical location (e.g. session log only) | `documents/04-quality/audits/aws-verification/` is the source of truth |

---

## 10. Relationship to other rules

- **`release-deploy-standard.md`** §9 — high-level agent role matrix; THIS rule is the command-level boundary
- **`agent-action-bias.md`** — agent does work itself; THIS rule scopes that "work" boundary for AWS
- **`mcp-first-with-fallback.md`** — tool-flavor selection; AWS CLI = legitimate Bash fallback (no MCP for AWS exists)
- **`output-review-mandate.md`** §3 — will add row "AWS verification reports" same PR as this rule
- **`incident-to-rule-pipeline.md`** — this rule is direct output of 2026-05-08 user-flagged retro applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity — this rule + first audit artifact + matrix row update all ship same PR
- **`feedback_terraform_apply_retry_reconfirm.md`** — terraform apply specific extension of Tier 3 ban
- **GAP-438** — parent gap; Phase 1 (this rule) + Phase 3 (first artifact) shipping; Phase 2 (skill) + Phase 4 (memory) follow-up

---

## 11. Log

- **2026-05-14** (v1.0.2): PATCH — thêm `paths:` frontmatter — Wave 73 miss fix (rule này nằm trong 13 MANDATORY rules wave plan §3 Scope bỏ sót, vẫn auto-load base context dù scope rule có path trigger rõ ràng). PATCH bump per `rule-change-process.md` §5 — additive frontmatter, no constraint change, deferred-load khi no matching file in context. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve). Scope: AWS work scope.
- **2026-05-08 (v1.0.1):** PATCH — §4.3 reframed "Banned terraform actions" → "Banned terraform actions (agent-initiated only)" + lead-in clarifying scope = AGENT-INITIATED apply per `release-deploy-standard.md` §9 (revised v1.0.1 same wave). Existing BAN clauses preserved verbatim; added "User-triggered (allowed)" sub-section listing workflow_dispatch + chicken-and-egg bootstrap as carve-outs. No constraint loosening for agent-initiated cases. Cross-link aligned với Wave 44 Bucket A. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — clarification of existing rule scope, paired with `release-deploy-standard.md` §9 revision in same wave). Closes Wave 44 Bucket A part of GAP-449 Phase 1.
- **2026-05-08 (v1.0.0):** Rule created in response to user-flagged retro after Phase 2.3 apply session ("lệnh check có vẻ là lệnh tự do, cần bổ sung workflow cho agent aws theo chuẩn đã đề cập chưa? báo cáo chi tiết như này có lưu logs tại repo không?"). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no existing rule covers; `release-deploy-standard.md` §9 high-level only) → Rule+Enforce ✓ (this file + first audit artifact `2026-05-08-phase-2-3-post-apply.md` + folder README + `output-review-mandate.md` §3 row update — all paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§7 worked example on the originating session — 5/6 commands Tier 1 OK, logging requirement violated and now remediated) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint, no loosening). Phase 2 (skill + script) + Phase 4 (memory) deferred to follow-up per GAP-438.
