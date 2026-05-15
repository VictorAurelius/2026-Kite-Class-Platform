---
title: Credential Rotation — admin-password (Wave 77 Bucket C automation)
status: pending
created: 2026-05-15
trigger: leak
related-gaps: [GAP-525]
parent-incident: documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
---

# Credential Rotation Audit — admin-password

## Scope

Rotation cho credential class `admin-password` leak trong session 2026-05-13. Wave 77 Bucket C wrapper script
`scripts/rotate-leaked-credentials.sh` đã guide user-execution. User điền outcome vào template này
sau khi hoàn tất rotation.

## Pre-rotation state (verified via Tier 1 read-only)

```
[FILL] aws secretsmanager describe-secret output (ARN + LastChangedDate trước rotation)
```

## Rotation steps performed

| Step | Description | Status | Timestamp (UTC) |
|---|---|---|---|
| 1 | Generate new credential | pending | — |
| 2 | Store in AWS Secrets Manager (put-secret-value) | pending | — |
| 3 | Redeploy / re-seed consumer | pending | — |
| 4 | Smoke test new credential | pending | — |
| 5 | Revoke old credential at vendor portal | pending | — |
| 6 | Verify old credential rejected | pending | — |

## Verification

```
[FILL] verify command outputs (per credential-rotation-runbook.md §4)
```

## Notes

- Wrapper script: `scripts/rotate-leaked-credentials.sh --cred=admin-password`
- Runbook: `documents/05-guides/operations/credential-rotation-2026-05-13.md`
- General runbook: `documents/05-guides/operations/credential-rotation-runbook.md`

## Closure

When all 6 rotation steps verified, set `status: complete` in frontmatter + update parent incident
artifact `2026-05-14-wave-72a-3-credentials.md` rotation-status table row to `verified`.

Flip GAP-525 `PARTIAL` → `DONE` only after all 3 credentials reach `verified` per
`.claude/rules/gap-done-discipline.md` §2.
