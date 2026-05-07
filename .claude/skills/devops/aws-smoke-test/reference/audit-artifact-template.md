# Audit Artifact Template

Copy this template to `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>.md` and fill in.

Required by `.claude/rules/agent-aws-access.md` §5 logging requirement.

---

```markdown
---
title: AWS Verification — <topic>
status: complete
created: YYYY-MM-DD
phase: <e.g. 2.1 / 2.2 / 2.3 / 4>
related:
  - .claude/rules/agent-aws-access.md
  - .claude/skills/devops/aws-smoke-test/SKILL.md
  - <link to relevant deploy plan / runbook / parent gap>
---

# AWS Verification Report — <topic>

**Date:** YYYY-MM-DD
**Phase:** <phase>
**Account:** 906286017800 (`ap-southeast-1`)
**Trigger:** <what prompted this verification — user question / post-apply / quarterly drift>
**Saved per:** `.claude/rules/agent-aws-access.md` §5 logging requirement

---

## Scope

<1-3 paragraphs: what was checked + why + which resources are in scope>

---

## Commands run (Tier 1 read-only per agent-aws-access.md §2)

| Command | Purpose | Result |
|---|---|---|
| `aws sts get-caller-identity` | Account verify | account 906286017800 ✅ |
| `aws ec2 describe-instances --filters ...` | EC2 inventory | <count> running |
| `<more commands>` | <purpose> | <result> |

All commands Tier 1 allowed per `agent-aws-access.md` §2.1 — `describe-*` / `list-*` / allowed `get-*` / `head-*` / `curl -sI`.

---

## Results

### Resource inventory

<Tabular breakdown by category — VPC / EC2 / RDS / ECR / Secrets / etc.>

### Endpoint accessibility

<HEAD probe results for relevant endpoints>

---

## Findings

### F1. <severity icon> <short title>

<detailed description; if anomaly, root cause + recommended fix>

### F2. ...

---

## Next steps (recommended)

| Priority | Action | Owner | Effort |
|---|---|---|---|
| P0 / P1 / P2 / P3 | <action> | User / Agent | <est> |

---

## Compliance / Audit notes

- All commands Tier 1 per `.claude/rules/agent-aws-access.md` §2 — read-only, no AWS state mutation
- No secret values logged (Tier 2 `get-secret-value` not run)
- Verification artifact saved per §5 requirement

---

## Related

- Parent gap / wave: <link>
- Previous verification: <link to prior artifact in `aws-verification/`>
- Follow-up gaps filed: <list, if any>
```

---

## Filename convention

`YYYY-MM-DD-<phase>-<topic>.md`

Examples:
- `2026-05-08-phase-2-3-post-apply.md`
- `2026-05-15-phase-2-3-quarterly-drift.md`
- `2026-06-01-phase-4-staging-bootstrap.md`
- `2026-07-01-cloudtrail-baseline-verify.md`

Multiple verifications per day → suffix with topic detail (e.g. `-post-apply` vs `-pre-deploy`).

---

## Reminders

- ❌ Never log secret values
- ❌ Never log full credit card / PII
- ✅ AWS account ID 906286017800 OK to log (already public per repo)
- ✅ Resource IDs, IP addresses, DNS names OK
- ✅ Save even when everything PASSES — happy-path audit trail matters
