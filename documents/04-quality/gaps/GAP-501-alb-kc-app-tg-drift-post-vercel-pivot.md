# GAP-501: ALB kc_app target group drift post-Vercel pivot — HTTPS 502 on root/auth/dashboard paths

**Status:** 🟢 DONE 2026-05-13 — terraform apply run 25783192647 destroyed 3 resources; smoke verified 502→404 on all FE paths; TG kitehub-kc-app-tg returns TargetGroupNotFound
**Priority:** 🟠 P1 (Phase 1 BETA — production routing returns 502 on FE paths via api.kitehub.me; backend health unaffected because falls through default rule)
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-13 (Wave 68 verification pass — smoke E2E surfaced 502 on `api.kitehub.me/`)
**Affects:** `api.kitehub.me` HTTPS routing for paths `/`, `/_next/*`, `/static/*`, `/auth/*`, `/dashboard/*`

## Problem

ALB listener 443 priority-100 rule forwards FE paths (`/`, `/_next/*`, `/static/*`, `/auth/*`, `/dashboard/*`) to `kitehub-kc-app-tg` (port 3000, expecting Next.js FE on kc_app EC2). After Vercel pivot (2026-05-07), FE moved to Vercel CDN; no service listens on port 3000 of kc_app EC2 anymore. Target group reports `unhealthy` (`Target.FailedHealthChecks`), so ALB returns **HTTP 502** for any path matching the rule.

Probed 2026-05-13 06:30 UTC:

| Path | HTTP | Routing |
|---|---|---|
| `api.kitehub.me/actuator/health` | ✅ 200 | default → `kh_backend` TG (healthy) |
| `api.kitehub.me/` | 🔴 **502** | priority 100 → `kc_app` TG :3000 (unhealthy) |
| `api.kitehub.me/auth/login` | 🔴 **502** | priority 100 → `kc_app` TG :3000 (unhealthy) |
| `api.kitehub.me/dashboard` | 🟡 404 | priority 100 → `kc_app` TG :3000 (mismatched but reachable) |

## Root Cause

Terraform `infrastructure/terraform-aws/ec2.tf` lines 180-268 define three resources reflecting pre-Vercel-pivot architecture (when KC frontend Next.js ran on kc_app EC2 :3000):

1. `aws_lb_target_group.kc_app` — TG port 3000, healthcheck `/api/health`
2. `aws_lb_target_group_attachment.kc_app` — binds kc_app EC2 :3000
3. `aws_lb_listener_rule.kc_app_default` — priority 100, path-pattern routing to TG

After Vercel pivot:
- `kitehub.me` apex → Cloudflare → Vercel CDN (FE)
- `api.kitehub.me` → ALB (only BE Java services expected on `kh_backend`)
- kc_app EC2 still runs (BE services kiteclass-core + gateway + redis + rabbitmq, **not on port 3000**) per GAP-447 right-sizing decision
- The 3 ALB resources were never removed → drift

## Proposed Fix

**Option A (recommended — clean cutover):** Delete 3 resources from `ec2.tf`:

```hcl
# REMOVE: aws_lb_target_group.kc_app (lines 180-198)
# REMOVE: aws_lb_target_group_attachment.kc_app (lines 200-205)
# REMOVE: aws_lb_listener_rule.kc_app_default (lines 253-268)
```

After removal:
- HTTPS listener default action already forwards to `kh_backend` TG (existing line 248)
- All paths → kh_backend → handles `/actuator/*` (200) + returns 404 from Spring Boot for unknown paths (vs 502 currently)
- Cleanup: ~$0 cost saving (TG itself free; healthcheck calls negligible) — primary value = remove 502 production noise + reduce attack surface

**Option B (rejected):** Keep TG, repoint port to a BE service port. Rejected because:
- `api.kitehub.me` is API-only; FE paths shouldn't terminate at ALB at all
- Vercel serves all FE traffic via `kitehub.me` apex
- Wiring a BE service to FE-path rule would mask real 404s as routed-but-wrong responses

## Acceptance Criteria

- [x] `infrastructure/terraform-aws/ec2.tf` — 3 resources removed (`aws_lb_target_group.kc_app`, `aws_lb_target_group_attachment.kc_app`, `aws_lb_listener_rule.kc_app_default`) — PR #1250 merged 2026-05-13
- [x] Pre-mutation audit artifact filed per `pre-mutation-state-check.md` §3 (verifies terraform plan = 3 destroys + 0 surprises) — `documents/04-quality/audits/aws-verification/2026-05-13-gap-501-pre-apply-kc-app-tg-removal.md`
- [x] User-triggered `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` per `release-deploy-standard.md` §9 — dry_run run 25783133968 + real apply run 25783192647 both SUCCESS
- [x] Post-apply verification: `curl -sS -o /dev/null -w "%{http_code}\n" https://api.kitehub.me/` returns **404** — **not 502** (verified `/`, `/auth/login`, `/dashboard` all → 404)
- [x] Post-apply verification: `aws elbv2 describe-target-groups --names kitehub-kc-app-tg` returns `TargetGroupNotFound` ✓
- [x] `gap-status.csv` row updated to DONE/100 post-verification (this PR)

## Out-of-scope

- kc_app EC2 instance lifecycle — still needed for BE services per GAP-447 (right-sized t3.medium 2026-05-08); this gap touches only ALB-side resources
- BE service port-exposure via ALB — separate decision, kc_app BE services currently reached only via internal Docker network on `kc_app` host (no public ALB rule); if external exposure needed in future, file separate gap
- Spring Boot 404 customization on unknown paths — out of scope; default 404 from `kh_backend` is acceptable for now

## Related

- **Parent finding:** Wave 68 verification pass 2026-05-13 — smoke E2E flagged 502
- **Sibling:** GAP-447 (PARTIAL 75% — EC2 right-sizing post-Vercel pivot; kc_app sized for BE workload, not FE)
- **Driving decision:** 2026-05-07 Vercel pivot (FE moved off kc_app EC2 to Vercel CDN)
- **Terraform file:** `infrastructure/terraform-aws/ec2.tf` lines 180-268

## Log

- **2026-05-13:** Filed Wave 68 verification pass. Smoke probe surfaced 3 paths returning 502; ALB DescribeRules + DescribeTargetHealth confirmed priority-100 rule + unhealthy TG drift. Pre-mutation audit artifact to be filed before terraform apply per `pre-mutation-state-check.md`.
- **2026-05-13:** PR #1250 merged (commit `217b2ef2`) — terraform `ec2.tf` removed 3 resources + pre-mutation audit artifact filed. CI terraform-plan + dry_run workflow both confirmed `Plan: 0 to add, 0 to change, 3 to destroy.`
- **2026-05-13:** 🟢 DONE — terraform-apply.yml dry_run run 25783133968 + real apply run 25783192647 both completed SUCCESS. `Apply complete! Resources: 0 added, 0 changed, 3 destroyed.` Post-apply smoke evidence:
  - `https://api.kitehub.me/` → HTTP **404** (was 502) ✅
  - `https://api.kitehub.me/auth/login` → HTTP **404** (was 502) ✅
  - `https://api.kitehub.me/dashboard` → HTTP 404 (now via kh_backend default rule) ✅
  - `https://api.kitehub.me/actuator/health` → HTTP 200 (unchanged) ✅
  - `aws elbv2 describe-target-groups --names kitehub-kc-app-tg` → `TargetGroupNotFound` ✅
  - ALB listener 443 rules: only `default → kh_backend` remains (priority-100 rule gone) ✅
  All 6 AC items verified; status flipped + CSV row sync per `post-merge-sync-completeness.md` Rule 17.
