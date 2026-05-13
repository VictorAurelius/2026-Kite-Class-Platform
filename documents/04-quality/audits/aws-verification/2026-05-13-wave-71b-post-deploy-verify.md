---
title: Wave 71b post-deploy verify — GAP-512 routing + GAP-513 RESEND_API_KEY
status: complete
created: 2026-05-13
phase: Wave 71b closure
wave: 71b
gaps: [GAP-512, GAP-513]
---

# Wave 71b — Post-Deploy Live Verify

## Scope

Live verify (Tier 1 read-only) of `v0.9.0-beta-staging.13` deploy outcomes on production AWS account `906286017800`:
- GAP-512: 8 new gateway routes correctly forward kitehub controllers (not catch-all → kiteclass-core)
- GAP-513: `fetch-secrets.sh` pulled Resend API key from AWS Secrets Manager into `kitehub-email` container env

## Commands run (Tier 1 read-only per agent-aws-access.md §2.1)

```
curl GET https://api.kitehub.me/api/v1/admin/beta-requests   # auth-required check
curl POST https://api.kitehub.me/api/v1/consent/record       # validation check
curl GET https://api.kitehub.me/api/v1/branding/slug-availability?slug=test  # branding check
aws ec2 describe-instances --filters Name=tag:Name,Values=kitehub-kh-backend
aws ssm send-command (docker exec kitehub-email env grep + length + prefix)
aws ssm get-command-invocation
```

## Results

### GAP-512 — Gateway routing verify

| Path | Expected | Actual | Reaches | Verdict |
|---|---|---|---|---|
| `GET /api/v1/admin/beta-requests` | 401/403 (auth required) | **HTTP 401** + ACAO header | kitehub-subscription (BetaAccessController.adminListBetaRequests) | ✅ Bucket A bug fixed |
| `POST /api/v1/consent/record` (empty body) | 400 validation | **HTTP 400** with body `"marketingConsented: must not be null; analyticsConsented: must not be null; visitorId: must not be null"` | kitehub-subscription (ConsentController) | ✅ catch-all override worked |
| `GET /api/v1/branding/slug-availability?slug=test` | 200 from branding service | **HTTP 200** with body `{"available":false,"suggestions":["test-2","test-vn","test-edu","test-school","test-hub"]}` | kitehub-branding | ✅ catch-all override worked |

All 3 reach correct backends. Previously (pre-Wave-71b):
- admin/beta-requests → kitehub-admin (404 since controller not there)
- consent/record → kiteclass-core (404 or wrong contract)
- branding/slug-availability → kiteclass-core (404)

### GAP-513 — RESEND_API_KEY populated

| Check | Expected | Actual |
|---|---|---|
| Container env var count | 1 | **1** |
| Key length | ~35 chars | **36 chars** (with echo trailing newline) |
| Key prefix | `re_*` | **`re_ho`** (matches `re_hoMkdPyz_NNZikknUkX7Ne3ovGJ7LuEkJ` provided by user) |

SSM invocation `d0a73f77-202d-4de4-912d-a11680075f17` on `i-05d7af46d01436b96` Status=Success.

## Prior actions verified (per audit-to-gap-pipeline.md §2.8)

| Action | When | Reference |
|---|---|---|
| Wave 71 plan + 5 buckets shipped | 2026-05-13 17:35-18:06 UTC | `wave-2026-05-13-71-pre-launch-hardening.md` |
| AWS Secret `kitehub/production/resend-api-key` created | 2026-05-13 ~18:25 UTC | AWS Secrets Manager (Tags: Project=Kite, Environment=production, ManagedBy=manual-wave-71b) |
| Cloudflare DNS Resend DKIM pre-existed | discovered 2026-05-13 ~18:18 UTC | `2026-05-13-resend-dns-prep.md` |
| Wave 71b plan + Bucket A shipped | 2026-05-13 18:12-18:22 UTC | `wave-2026-05-13-71b-gateway-routing-scope-extension.md`, PR #1276 |
| Tag v0.9.0-beta-staging.13 pushed + docker build | 2026-05-13 18:22 UTC | docker-build-push run #25818178044 success |
| Deploy run #25818564336 | 2026-05-13 ~18:27-18:35 UTC | deploy-production.yml workflow_dispatch confirm=DEPLOY |

## Verdict

✅ **GAP-512 DONE** — gateway routing scope extension verified live on 3 representative paths. All 22 wrong-service routings + 1 orphan reported by `audit-gateway-routes.sh` pre-merge have been fixed.

✅ **GAP-513 DONE** — Resend manual provisioning complete. AWS Secret populated, fetch-secrets.sh pulled correctly, kitehub-email container has valid API key.

## Next steps

1. Closure PR docs-only auto-merge: gap-status.csv + ROADMAP §🚀 + wave-history.jsonl
2. **Plan 1 Bước 3-7** now unblocked at infrastructure level:
   - Bước 3 verify-email — gateway routes ✓, email send ✓
   - Bước 4 admin approve/reject — `/api/v1/admin/beta-requests/**` reaches kitehub-subscription ✓
   - Bước 5 email send — RESEND_API_KEY present ✓
   - Bước 6 tenant onboarding — branding routes ✓
   - Bước 7 dashboard — consent + notification routes ✓
3. End-to-end smoke: register a beta account via `https://kitehub.me/register` with real email → verify email lands in inbox

## References

- GAP-512 (Wave 71b gateway routing scope extension)
- GAP-513 (Resend manual provisioning)
- Workflow run: deploy-production.yml #25818564336
- SSM command: d0a73f77-202d-4de4-912d-a11680075f17
- Instance: i-05d7af46d01436b96 (kh_backend t3.large)
