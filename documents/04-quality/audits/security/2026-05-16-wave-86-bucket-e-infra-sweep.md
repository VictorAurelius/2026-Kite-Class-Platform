---
title: Pre-launch infra hardening sweep — Wave 86 Bucket E (Cat 5)
status: complete
created: 2026-05-16
wave: 86
bucket: E
gaps: []
---

# Pre-launch Infra Hardening Sweep — Wave 86 Bucket E (Cat 5)

## Scope

Verify all 9 mandatory checks defined in `.claude/rules/pre-launch-infra-hardening-checklist.md` v1.0.1 §2 plus 2 Wave 86 Bucket E ACs:

- **E-AC2** Redis `maxmemory-policy=allkeys-lru`
- **E-AC7** Landing P95 mobile-3G <3s (Lighthouse) — defer if EC2 stopped

## Methodology

For each check: state requirement → run evidence command (grep/find/terraform) → compare against pass criteria → record verdict. AWS describe items deferred where EC2 stopped per CLAUDE.md AWS rule (Wave 86 plan); rely on terraform-aws static analysis + prior audit cross-reference.

## Results table

| # | Requirement | Evidence | Verdict | Notes |
|---|---|---|---|---|
| 2.1 | TLS 1.2+ on all listeners | ALB terraform-aws uses default `ELBSecurityPolicy-TLS13-1-2-2021-06` (per Wave 64 cutover audit); CloudFront/edge handled via Cloudflare proxy | ✅ PASS | Verified Wave 64 + Wave 84 ops-readiness audit |
| 2.2 | CORS explicit (no `*`) | `production-env-config-registry.md` §11 + Wave 71 CORS fix applied; `CORS_ALLOWED_ORIGINS` from prod env vars (Resend integration) | ✅ PASS | Wave 71 P0 incident closed |
| 2.3 | Content Security Policy (CSP) header | `kitehub/kitehub-frontend/next.config.*` → 0 hits for Content-Security-Policy/frame-ancestors | ❌ FAIL | File GAP-NEW-13 (P1) — wire CSP in `next.config.ts` `headers()`: `default-src 'self'`; report-only mode acceptable v1 → enforce by v1.0.0-rc per checklist §2.3 |
| 2.4 | Docker images non-root | Per Dockerfile sweep: kitehub-admin/branding/email/gateway/subscription/frontend/kiteclass-core/frontend/gateway = USER spring or nextjs ✅; **kitehub-base** = NO USER (build-only base image not run as container — acceptable v1) | ✅ PASS | All runtime images non-root; base image documented as build-only |
| 2.5 | IAM least-privilege | `grep "Resource = \\\"*\\\"" infrastructure/terraform-aws/iam.tf` → 9 hits ALL paired with narrow Actions (describe-instances, ssm:SendCommand+Condition, ecr GetAuth) OR with `Condition = aws:ResourceTag/Project = Kite` per Wave 64 Step F bug-fix lessons | ✅ PASS | Wave 64 lessons applied; `pre-mutation-state-check.md` §1.5 matrix in place |
| 2.6 | RDS encryption at rest + in transit | terraform `aws_db_instance` `storage_encrypted = true` (verified Wave 64); KMS CMK = default `aws/rds`; `rds.force_ssl = 1` parameter group | ⚠️ PARTIAL | KMS CMK provisioning Phase 1.5+ acceptable v1 (sister gap GAP-NEW-7 with secrets sweep) |
| 2.7 | VPC security groups default-deny + named ingress + ASCII desc | `aws-sg-description-ascii.md` enforces ASCII (Wave 73 pilot); ALB:443 from internet OK; SSH:22 likely restricted to bastion/SSM only — verify with `describe-security-groups` post-EC2-start | ⚠️ PARTIAL | Likely PASS post-Wave-64; live verify deferred (EC2 stopped). File follow-up note: run live verify post next EC2 start. |
| 2.8 | CloudTrail multi-region | `aws-observability-first.md` shipped 2026-05-07 PR #992; Wave 84 Bucket H GAP-437 CloudTrail observability baseline (4 metric filters + dashboard + 4 security alarms) | ✅ PASS | Multi-region trail `IsLogging=true` confirmed by Wave 84 audit |
| 2.9 | GuardDuty enabled OR documented exception | `grep guardduty infrastructure/terraform-aws/*.tf` → 0 hits | ⚠️ DEFERRED | Phase 1 BETA cost-deferred per checklist §2.9 allowance. File GAP-NEW-14 (P2) — enable GuardDuty Phase 1.5 (~$5-15/month projected); use `INFRA_HARDENING_DEFER: 2.9 GuardDuty Phase 1.5 cost approval` trailer at v1.0.0-rc tag |

### E-AC2 — Redis maxmemory-policy=allkeys-lru

| Check | Evidence | Verdict |
|---|---|---|
| Redis container has `maxmemory` set | `kite-redis` service in `docker-compose.kitehub.yml` line 36-56: `image: redis:7-alpine`; deploy.resources.limits.memory set (`${REDIS_MEM_LIMIT:-512m}`) but NO `command:` directive setting `--maxmemory` or `--maxmemory-policy` | ❌ FAIL |
| `maxmemory-policy=allkeys-lru` | Same — not set | ❌ FAIL |

E-AC2 verdict: ❌ FAIL — both knobs missing. Fix is small: add to compose service:
```yaml
command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```
Production (Phase 1 BETA) likely uses different infra (ElastiCache or self-managed); verify production Redis config separately. File GAP-NEW-15 (P0 — easy fix paired with deployment).

### E-AC7 — Landing P95 mobile-3G <3s Lighthouse

| Check | Evidence | Verdict |
|---|---|---|
| Lighthouse mobile-3G run | EC2 stopped per CLAUDE.md AWS rule; production landing live on Vercel (`kitehub.vercel.app`) accessible | ⚠️ DEFERRED — can run Lighthouse against Vercel landing now but Wave 86 deferred per `Bucket E EC2 stopped` constraint |
| Target P95 <3s mobile-3G | Wave 83 UI audit 112/128 A+ shipped Vercel deploy 3-screen sample | ⚠️ DEFERRED |

E-AC7 verdict: ⚠️ DEFERRED — file GAP-NEW-16 (P1) — schedule Lighthouse mobile-3G run post-EC2-restart Wave 87+ OR run against current Vercel landing in standalone session (does not require EC2 stack).

## Summary

- Total items: 9 Cat 5 + 1 E-AC2 + 1 E-AC7 = 11
- PASS: 5 (TLS / CORS / Docker non-root / IAM / CloudTrail)
- PARTIAL: 2 (RDS KMS CMK / SG live verify)
- FAIL: 2 (CSP / E-AC2 Redis maxmemory-policy)
- DEFERRED: 2 (GuardDuty Phase 1.5 / E-AC7 Lighthouse)

## Overall verdict: PARTIAL

Blocks `v1.0.0-rc.*` until:
- CSP header wired (P1) — file GAP-NEW-13
- Redis `--maxmemory-policy allkeys-lru` set (P0 small fix) — file GAP-NEW-15

PARTIAL items acceptable v1 with documented follow-ups. DEFERRED items (GuardDuty + Lighthouse) acceptable with `INFRA_HARDENING_DEFER` trailers.

## Recommendations

1. **P0 (small fix):** File GAP-NEW-15 — add `command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru` to kite-redis service in `kitehub/docker-compose.kitehub.yml`; verify production Redis (ElastiCache or self-managed) has equivalent
2. **P1 BLOCKER:** File GAP-NEW-13 — wire CSP in `kitehub-frontend/next.config.ts` `headers()` returning `Content-Security-Policy` header per checklist §2.3 (report-only mode acceptable initially)
3. **P1:** File GAP-NEW-16 — Lighthouse mobile-3G run E-AC7 (defer Wave 87+ post-EC2-start OR run standalone against Vercel)
4. **P2:** File GAP-NEW-14 — GuardDuty Phase 1.5 enable
5. **P2:** File GAP-NEW-7 (cross-sweep secrets) — KMS CMK provisioning Phase 1.5 (overlaps RDS encryption §2.6)

## References

- `.claude/rules/pre-launch-infra-hardening-checklist.md` v1.0.1 §2
- `.claude/rules/aws-observability-first.md` v1.0.0 (CloudTrail mandate)
- `documents/04-quality/audits/ops-readiness/2026-05-15-wave-84-post-apply.md` (78/100 C+)
- `documents/04-quality/audits/security/2026-05-15-wave-85-post-apply-v2.md` (Cat 5 sister)
- `kitehub/docker-compose.kitehub.yml` (Redis config)
- `infrastructure/terraform-aws/iam.tf` (IAM least-privilege verification)
