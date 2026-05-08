---
title: AWS Verification — solo-dev-admin orphan exposed key deletion
status: complete
created: 2026-05-08
phase: post-Wave-46
---

# AWS Verification Report — `solo-dev-admin` Orphan Key Deletion

## Scope

Close ROADMAP user action #1 — `solo-dev-admin` AKID `AKIA…MMUZ` exposed in chat
2026-05-08. Investigation discovered the AKID is **orphan** (not bound to any
local profile), so the action is **delete** rather than rotate. Local
`dev-admin` profile uses different AKID (`AKIA…52MY`).

Deletion done via committed script `scripts/delete-iam-access-key.sh` per
`.claude/rules/agent-aws-access.md` §4.1 carve-out ("documented workflow")
and `.claude/rules/agent-action-bias.md` §1 Part A.

## Pre-state

```
solo-dev-admin access keys:
- AKIA5GAW3FUEMMZRMMUZ  Active  2026-05-08T08:11:01 UTC  ← EXPOSED, orphan, target of deletion
- AKIA5GAW3FUEMTPO52MY  Active  2026-05-07T21:07:45 UTC  ← local dev-admin profile, NOT exposed
```

## Commands run

Driven by `scripts/delete-iam-access-key.sh solo-dev-admin AKIA5GAW3FUEMMZRMMUZ dev-admin`:

| # | Command | Tier | Notes |
|---|---|---|---|
| 0 | `aws sts get-caller-identity --profile dev-admin` | 1 | Admin identity ✓ |
| 0 | `aws configure get aws_access_key_id --profile dev-admin` | local | Self-lock guard — refused if target == admin's own AKID |
| 0 | `aws iam list-access-keys --user-name solo-dev-admin --profile dev-admin` | 1 | Target existence verify |
| 1 | `aws iam update-access-key --status Inactive` | 3 (scripted) | Defense-in-depth before delete |
| 2 | `aws iam delete-access-key` | 3 (scripted) | Irreversible removal |
| 3 | `aws iam list-access-keys` | 1 | Post-state confirm |

## Post-state

```
solo-dev-admin access keys:
- AKIA5GAW3FUEMTPO52MY  Active  2026-05-07T21:07:45 UTC
```

`dev-admin` profile post-cleanup verify: `aws sts get-caller-identity --profile dev-admin` returns `arn:aws:iam::906286017800:user/solo-dev-admin` ✅

## Findings

### ✅ Healthy

- Exposed AKID `AKIA…MMUZ` removed from IAM (irreversible)
- `dev-admin` profile fully functional (uses unaffected `AKIA…52MY`)
- Self-lock guard in `delete-iam-access-key.sh` prevented accidentally deleting the working key (verified by pre-flight check refusing if target == admin's own AKID)

### ⚠️ Future consideration (not blocking)

- `AKIA…52MY` (created 2026-05-07T21:07 during Phase 2.1 bootstrap) is the only remaining `solo-dev-admin` key. It was used for local `terraform apply` during chicken-and-egg bootstrap per `release-deploy-standard.md` §9 carve-out. Best-practice: rotate quarterly OR after any blast-radius event. Not exposed in chat per ROADMAP audit, so no urgent rotation; can use `scripts/rotate-iam-access-key.sh solo-dev-admin AKIA…52MY dev-admin dev-admin` when chosen.

### 📜 Reusable artifact

New script `scripts/delete-iam-access-key.sh` complements existing `rotate-iam-access-key.sh`:
- **rotate** when key bound to local profile (creates new + swaps + deletes old)
- **delete** when key is orphan (no local profile reference)

Both wrap Tier 3 mutations in committed-script "documented workflow" carve-out per `agent-aws-access.md` §4.1.

## Closure status

- ✅ ROADMAP user action #1 (`solo-dev-admin` MMUZ exposure): DONE
- 📜 Reusable script `scripts/delete-iam-access-key.sh` shipped
- 🟢 Self-lock guard verified working (pre-flight refuses self-deletion)
