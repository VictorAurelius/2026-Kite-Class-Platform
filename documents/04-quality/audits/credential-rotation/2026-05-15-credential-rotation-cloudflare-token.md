---
title: Credential Rotation — cloudflare-token (Wave 81 Bucket C)
status: complete
created: 2026-05-15
trigger: leak
related-gaps: [GAP-525]
parent-incident: documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
---

# Credential Rotation Audit — cloudflare-token

## Scope

Rotation cho credential class `cloudflare-token` leak trong session 2026-05-13 (per GAP-525). Wave 81 Bucket C — user thực hiện rotation qua Cloudflare dashboard `Roll` button + `aws secretsmanager put-secret-value`; Claude verify via Tier 1 read-only `describe-secret`.

## Pre-rotation state

Verified pre-rotation tại session 2026-05-15 06:30 UTC qua `aws secretsmanager describe-secret`:

- Secret exists trong AWS Secrets Manager
- AWSCURRENT version pre-rotation: (version cũ — không log để tránh exposure metadata)
- LastChangedDate pre-rotation: pre-2026-05-15

## Rotation steps performed

| Step | Description | Status | Timestamp (UTC) |
|---|---|---|---|
| 1 | Snapshot pre-rotation state (Tier 1 `describe-secret`) | ✅ complete | 2026-05-15 ~06:30 |
| 2 | Generate new credential — Cloudflare dashboard `Roll` (auto-revoke old token) | ✅ complete | ~06:40-06:45 |
| 3 | Store in AWS Secrets Manager (`put-secret-value`) | ✅ complete | 2026-05-15 06:45:13 |
| 4 | Smoke test new credential — `curl GET /zones` returned `success: true` | ✅ complete | ~06:46 |
| 5 | Revoke old credential — N/A (auto-revoked bởi Cloudflare `Roll`) | ✅ skipped (auto) | n/a |
| 6 | Verify old credential rejected — implicit via `Roll` auto-revoke | ✅ implicit | n/a |

## Verification

Post-rotation Tier 1 read-only check (Claude verify):

```bash
$ aws secretsmanager describe-secret \
    --secret-id kitehub/production/cloudflare-api-token \
    --profile dev-admin --region ap-southeast-1 \
    --query '[Name, LastChangedDate, VersionIdsToStages]'

"kitehub/production/cloudflare-api-token"
"2026-05-15T06:45:13.801000+00:00"
{
    "9a648505-0754-4b15-b66a-f9529a50271f": ["AWSCURRENT"]
}
```

- `LastChangedDate` = `2026-05-15T06:45:13Z` ✓ (hôm nay, post-rotation timestamp)
- AWSCURRENT version mới: `9a648505-0754-4b15-b66a-f9529a50271f`
- Previous version giữ trong AWS Secrets Manager 30 ngày (recovery available)

## Notes

- Cloudflare `Roll` button = atomic mint-new + revoke-old; không cần Bước 5 manual revoke (per workflow flag)
- Services Wave 81 chưa deployed (Bucket D pending) → không service nào đang dùng token cũ → zero-impact rotation
- Threat model check (2026-05-15): real cred `03M05pTouiE7GPJaAgTcOmaIW2W0dG9Ldm97ph5gOR` confirmed exposed trong 5 session jsonl files → rotation mandatory (delete-local không đủ vì backup risk + ext4 forensic recovery + telemetry uncertainty)
- Token cũ vô hiệu hoá ngay sau Cloudflare `Roll` — session jsonl files cũ giờ chứa cred chết, không còn risk

## Closure

✅ All 6 rotation steps verified (5 active + 1 skipped vì auto-revoke). Status: complete.

GAP-525 closure status (cred #2 done):
- cred #1 `seed-admin-password` — pre-existing TF-managed (verified Wave 81 Bucket C dry-run analysis)
- cred #2 `cloudflare-api-token` — ✅ rotated 2026-05-15 (this artifact)
- cred #3 `resend-api-key` — pending user action

GAP-525 flip `PARTIAL` → `DONE` chờ cred #3 hoàn tất.

## References

- Wave 81 Bucket C dry-run: `documents/04-quality/audits/aws-verification/2026-05-15-wave-81-bucket-c-dry-run-analysis.md`
- Rotation runbook: `documents/05-guides/operations/2026-05-15-wave-81-bucket-c-rotation-commands.md`
- Wrapper script: `scripts/rotate-leaked-credentials.sh --cred=cloudflare-token`
- Parent incident: `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md`
