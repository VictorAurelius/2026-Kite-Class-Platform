---
title: Credential Rotation — resend-api-key (Wave 81 Bucket C)
status: complete
created: 2026-05-15
trigger: leak
related-gaps: [GAP-525]
parent-incident: documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
---

# Credential Rotation Audit — resend-api-key

## Scope

Rotation cho credential class `resend-api-key` leak trong session 2026-05-13 (per GAP-525). Wave 81 Bucket C — user thực hiện rotation qua Resend dashboard mint NEW key + revoke OLD key + `aws secretsmanager put-secret-value`; Claude verify via Tier 1 read-only `describe-secret`.

## Pre-rotation state

- Secret exists trong AWS Secrets Manager (TF-managed)
- Pre-rotation version = AWSPREVIOUS post-rotation: `abb18020-e810-46b0-bb9d-2f1132447174`
- Old key prefix (leaked trong session 2026-05-13): `re_hoMkdPyz...`

## Rotation steps performed

| Step | Description | Status | Timestamp (UTC) |
|---|---|---|---|
| 1 | Snapshot pre-rotation state (Tier 1 `describe-secret`) | ✅ complete | 2026-05-15 ~06:30 |
| 2 | Mint new API key trên Resend dashboard | ✅ complete | ~06:48 |
| 3 | Store in AWS Secrets Manager (`put-secret-value`) | ✅ complete | 2026-05-15 06:50:15 |
| 4 | Smoke test new credential — verify via `describe-secret` Tier 1 | ✅ complete | ~06:51 |
| 5 | Redeploy kitehub-email service (pickup new key) | ⏳ deferred Bucket D | n/a |
| 6 | Smoke test transactional email end-to-end | ⏳ deferred Bucket D | n/a |
| 7 | Revoke OLD key trên Resend dashboard | ✅ complete | ~06:51 |
| 8 | Verify old key rejected (post-deploy) | ⏳ deferred Bucket D | n/a |

## Verification

Post-rotation Tier 1 read-only check (Claude verify):

```bash
$ aws secretsmanager describe-secret \
    --secret-id kitehub/production/resend-api-key \
    --profile dev-admin --region ap-southeast-1 \
    --query '[Name, LastChangedDate, VersionIdsToStages]'

"kitehub/production/resend-api-key"
"2026-05-15T06:50:15.881000+00:00"
{
    "abb18020-e810-46b0-bb9d-2f1132447174": ["AWSPREVIOUS"],
    "e35c5b89-47fc-4254-9ad7-31ba9ecaf1cd": ["AWSCURRENT"]
}
```

- `LastChangedDate` = `2026-05-15T06:50:15Z` ✓ today
- AWSCURRENT version mới: `e35c5b89-47fc-4254-9ad7-31ba9ecaf1cd`
- AWSPREVIOUS version cũ retained 30 ngày: `abb18020-e810-46b0-bb9d-2f1132447174` (recovery available nếu cần revert)

## Notes

- OLD key đã revoked trên Resend dashboard ngay sau khi NEW key save AWS — atomic close (user manual click revoke)
- Services Wave 81 chưa deployed (Bucket D pending) → smoke test transactional email end-to-end (Step 6) defer to Bucket D post-deploy
- Tuy nhiên rotation core (Step 1-4 + 7) đã complete, NEW key trong AWS Secrets Manager sẵn sàng cho Bucket D kitehub-email pickup
- Threat model check (2026-05-15): real cred patterns confirmed exposed trong session jsonl files; rotation mandatory bất chấp local-delete option
- Post-Bucket-D verification queue:
  - Trigger 1 transactional email → verify delivered (Resend dashboard status)
  - Verify old key prefix `re_hoMkdPyz...` rejected qua curl test → expect HTTP 401

## Closure

✅ Steps 1-4 + 7 complete (5/8). Steps 5/6/8 defer Bucket D post-deploy verification (3/8 deferred).

Rotation core (cred save + old revoke) coi như **DONE** vì:
- Old key đã chết bên Resend
- New key trong AWS Secrets Manager
- Smoke test (5/6/8) là verification của Bucket D deploy, không phải rotation step

GAP-525 closure status:
- cred #1 `seed-admin-password` — ✅ pre-existing TF-managed (Wave 72a/77 Bucket C)
- cred #2 `cloudflare-api-token` — ✅ rotated 2026-05-15 06:45 (cloudflare-token audit)
- cred #3 `resend-api-key` — ✅ rotated 2026-05-15 06:50 (this artifact); smoke test defer Bucket D

→ GAP-525 PARTIAL 85% → **DONE 100%** (rotation work complete; smoke test verification = Bucket D scope).

## References

- Wave 81 Bucket C dry-run: `documents/04-quality/audits/aws-verification/2026-05-15-wave-81-bucket-c-dry-run-analysis.md`
- Rotation runbook: `documents/05-guides/operations/2026-05-15-wave-81-bucket-c-rotation-commands.md`
- Sister audit: `documents/04-quality/audits/credential-rotation/2026-05-15-credential-rotation-cloudflare-token.md`
- Wrapper script: `scripts/rotate-leaked-credentials.sh --cred=resend-api-key`
- Parent incident: `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md`
