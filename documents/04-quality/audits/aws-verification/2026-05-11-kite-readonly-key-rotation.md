---
title: AWS Verification — kite-readonly-wsl key rotation (orphan cleanup + create + verify)
status: complete
created: 2026-05-11
phase: cleanup
---

# AWS Verification — kite-readonly-wsl key rotation

## Scope

User-flagged miss tại `/start-session`: AWS section báo "no-auth" dù credentials có sẵn. State-check phát hiện local `~/.aws/credentials` `kite-readonly` profile giữ AKID `AKIA…E7SO` đã bị deleted khỏi AWS từ 2026-05-08 rotation. AWS hiện chỉ có 1 active key `AKIA…SVMD` cho IAM user `kite-readonly-wsl`, secret không có ở local.

User pre-authorized Tier 3 override (Option B per session retro) để create new key. RTK output filter intercepted secret on first attempt → 1 orphan key landed without local secret → cleanup + retry với proper RTK bypass.

## Commands run

### Pre-flight verification

```bash
AWS_PROFILE=kite-readonly aws sts get-caller-identity
# Result: InvalidClientTokenId — AWS doesn't recognize the E7SO key

AWS_PROFILE=dev-admin aws sts get-caller-identity
# Result: 906286017800 user/solo-dev-admin (verified dev-admin profile works)

AWS_PROFILE=dev-admin aws iam list-access-keys --user-name kite-readonly-wsl
# Result: 1 active key AKIA…SVMD (2026-05-08), secret not local
```

### Create attempt #1 (failed — RTK output filter)

```bash
AWS_PROFILE=dev-admin aws iam create-access-key --user-name kite-readonly-wsl \
  --output json 2>&1 > /tmp/new-key.json
# Result: AWS created key AKIA…NFQPYTGI; stdout intercepted by RTK schema-rewrite;
# /tmp/new-key.json = 0 bytes → secret LOST
```

### Cleanup orphan + retry

```bash
rtk proxy aws iam delete-access-key --user-name kite-readonly-wsl \
  --access-key-id AKIA5GAW3FUENFQPYTGI --profile dev-admin --region ap-southeast-1
# Result: orphan NFQPYTGI deleted

rtk proxy aws iam create-access-key --user-name kite-readonly-wsl \
  --profile dev-admin --region ap-southeast-1 --output json \
  > /tmp/new-key.json 2>/tmp/new-key.err
# Result: 266 bytes captured to file; exit 0
# RTK proxy bypass preserves stdout for shell redirect

NEW_AKID=$(rtk proxy jq -r '.AccessKey.AccessKeyId' /tmp/new-key.json)
NEW_SECRET=$(rtk proxy jq -r '.AccessKey.SecretAccessKey' /tmp/new-key.json)
# Captured: AKID=AKIA5GAW3FUEHWLM2ATE, secret length=40

# Update credentials file (heredoc to avoid echoing secret)
cat > ~/.aws/credentials << EOF
[kite-readonly]
aws_access_key_id = $NEW_AKID
aws_secret_access_key = $NEW_SECRET
region = ap-southeast-1
...
EOF
chmod 600 ~/.aws/credentials
```

### Verify + cleanup secrets from disk

```bash
AWS_PROFILE=kite-readonly aws sts get-caller-identity
# Result: 906286017800 user/kite-readonly-wsl ✅

shred -u /tmp/new-key.json /tmp/new-key.err
# Result: files securely deleted
```

### Test collect-state.sh fix

```bash
unset AWS_PROFILE && bash .claude/skills/workflow/start-session/scripts/collect-state.sh --refresh-aws
# Result: AWS section now reports:
# · Account/Region: 906286017800 / ap-southeast-1
# · EC2: 0 running, 2 stopped, 2 total
# · RDS: 1 stopped (kitehub-postgres)
# · ALB: 1 active (kitehub-alb)
# · CloudTrail: kitehub-main=IsLogging:True
```

## Results

| Action | Outcome |
|---|---|
| Pre-flight diagnosis | E7SO deleted; SVMD active (secret lost); credentials stale |
| Delete orphan NFQPYTGI | ✅ |
| Create new key (HWLM2ATE) | ✅ secret captured |
| Update ~/.aws/credentials | ✅ profile kite-readonly + dev-admin both present |
| Auth verify kite-readonly | ✅ user/kite-readonly-wsl |
| Shred temp secret files | ✅ |
| collect-state.sh — explicit AWS_PROFILE | ✅ fix shipped (this PR) |

## Findings + script fix

`collect-state.sh` was calling `aws` commands without setting `AWS_PROFILE` → default profile resolution → none configured → silent `no-auth` fallback. Fixed by adding `--profile kite-readonly` to all Tier 1 calls (override-able via `AWS_PROFILE` env). Per `agent-aws-access.md` §2.1 — read-only profile is correct architectural choice cho session-start state collection.

## Secret values handling

- AKID `AKIA…HWLM2ATE` saved to `~/.aws/credentials` (file mode 600)
- Secret captured via shell var (NEW_SECRET, length 40) → heredoc to credentials file → unset after
- Temp files `/tmp/new-key.json` + `/tmp/new-key.err` shredded via `shred -u`
- No secret value in this audit artifact; no secret in transcript via `echo` (length check only)

## Active keys post-rotation

| AKID | IAM User | Created | Local secret? |
|---|---|---|---|
| `AKIA…SVMD` | kite-readonly-wsl | 2026-05-08 | ❌ NO (orphaned — should be deleted in follow-up) |
| `AKIA…HWLM2ATE` | kite-readonly-wsl | 2026-05-11 (this session) | ✅ YES |
| `AKIA…CTVPN647` | solo-dev-admin | (pre-session) | ✅ YES |

## Follow-up actions recommended

1. **Delete orphan SVMD** — Tier 3 mutation, requires separate user authorization. Reduces risk surface (key active in AWS without local secret = anyone with leaked secret could use it).
2. **Rotate `solo-dev-admin` key** — `AKIA…CTVPN647` was pasted in chat 2026-05-11 (Anthropic transcript = lost trust). Standard rotation hygiene.
3. **Wait for kite-readonly key audit artifact lifecycle** — quarterly key rotation per `secrets-rotation-runbook.md` §5 (180-day cadence).

## Compliance with rules

- ✅ `agent-aws-access.md` §2.1 — Tier 1 reads (sts, list-access-keys) used
- ✅ `agent-aws-access.md` §4.1 — Tier 3 (create-access-key, delete-access-key) used WITH explicit user pre-authorization Option B + override trailer in commit
- ✅ `agent-aws-access.md` §2.2 — secret captured via internal shell var, never echoed to transcript
- ✅ `agent-aws-access.md` §5 — this audit artifact saved
- ✅ Temp files containing secret material shredded via `shred -u`
