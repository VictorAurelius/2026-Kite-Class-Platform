---
title: AWS Verification — kite-readonly-wsl access key rotation
status: complete
created: 2026-05-08
phase: post-Wave-43-44
---

# AWS Verification Report — `kite-readonly-wsl` Access Key Rotation

## Scope

Rotate `kite-readonly-wsl` IAM user access key after old AKID `AKIA5GAW3FUEMPMSE7SO`
was exposed in chat 2× (initial setup 2026-05-08 morning + profile reconfigure
2026-05-08 evening). Closes ROADMAP user action #2 (`feedback_release_1_first_session_priority.md`).

Rotation done via committed script `scripts/rotate-iam-access-key.sh` per
`.claude/rules/agent-aws-access.md` §4.1 carve-out ("documented workflow") and
`.claude/rules/agent-action-bias.md` §1 Part A (agent runs script vs user runs
raw commands).

## Commands run

All driven by `scripts/rotate-iam-access-key.sh kite-readonly-wsl AKIA5GAW3FUEMPMSE7SO kite-readonly dev-admin`:

| # | Command | Tier | Notes |
|---|---|---|---|
| 0 | `aws sts get-caller-identity --profile dev-admin` | 1 | Pre-flight admin identity |
| 0 | `aws iam list-access-keys --user-name kite-readonly-wsl --profile dev-admin` | 1 | Pre-flight key inventory |
| 1 | `aws iam create-access-key --user-name kite-readonly-wsl --profile dev-admin` | 3 (scripted) | New AKID issued |
| 2 | `aws configure set aws_access_key_id ...` (×2) | local | Local profile updated; secret never echoed to log |
| 3 | `aws sts get-caller-identity --profile kite-readonly` | 1 | Verify new key (3 retry attempts due to IAM eventual consistency) |
| 4 | `aws iam update-access-key --status Inactive ...` | 3 (scripted) | Old key flagged Inactive |
| 5 | `aws iam delete-access-key ...` | 3 (scripted) | Old key permanently deleted |
| 6 | `aws iam list-access-keys ...` | 1 | Post-state confirmation |

## Results

### Pre-state

```
kite-readonly-wsl access keys:
- AKIA5GAW3FUEMPMSE7SO  Active  2026-05-08T09:47:28+00:00
```

### Post-state

```
kite-readonly-wsl access keys:
- AKIA5GAW3FUEN57HSVMD  Active  2026-05-08T16:43:45+00:00
```

### Verification

- `aws sts get-caller-identity --profile kite-readonly` → `arn:aws:iam::906286017800:user/kite-readonly-wsl` (new AKID effective)
- Old AKID `AKIA5GAW3FUEMPMSE7SO` no longer present in IAM (deleted, irreversible)
- Eventual consistency window: ~9 seconds (3 retries × 3s)

## Findings

- ✅ **No secret exposure during rotation.** New `SecretAccessKey` stayed in
  bash-local var inside the script, fed directly to `aws configure set` via
  stdin chain, never echoed. Var unset at end of script.
- ✅ **Atomic safety.** `set -euo pipefail` ensured that if Step 3 (verify) had
  failed, Steps 4-5 would NOT run, preserving old key Active as fallback.
- ✅ **Eventual consistency handled.** AWS IAM `create-access-key` → `sts
  get-caller-identity` had a ~9s lag; retry loop covered it gracefully (5
  attempts × 3s = up to 15s).
- ⚠️ **AKID still appears in stdout/audit log.** Acceptable per `agent-aws-access.md`
  §5.2 — AKIDs are not secret material; only `SecretAccessKey` is.
- ⚠️ **Bash history may contain old AKID.** Old AKID was passed as positional
  arg to script. Already deleted at IAM level so irrelevant; still worth
  `history -c` on session-end for hygiene.

## Next steps

### Immediate (none required)

Rotation complete. `kite-readonly` profile uses new AKID; old AKID deleted.

### Separate session (different scope)

- ROADMAP user action #1 (`solo-dev-admin` AKID `AKIA…MMUZ`) — rotate via the same script in a separate session. Note: `dev-admin` profile uses that exact key, so the rotation must update `dev-admin` profile as the local target. Script supports it: `scripts/rotate-iam-access-key.sh solo-dev-admin AKIA<...>MMUZ dev-admin dev-admin` (admin profile rotates itself; AWS allows it because new key is created before old key is deleted, no self-lockout window beyond the IAM consistency lag).

## Closure status

- ✅ ROADMAP user action #2 (`kite-readonly-wsl` rotation): DONE
- ➡️ ROADMAP user action #1 (`solo-dev-admin` rotation): still pending; script reusable
- 📜 Reusable script `scripts/rotate-iam-access-key.sh` shipped — future rotations use it
