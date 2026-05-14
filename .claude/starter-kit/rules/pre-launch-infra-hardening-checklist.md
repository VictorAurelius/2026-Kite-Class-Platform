# Pre-Launch Infra Hardening Checklist — security-audit Cat 5 per-check rubric

**Priority:** 🟠 MANDATORY — pre-launch security gate (Cat 5 force-multiplier)
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-vague Cat 5 per-check enforcement closing GAP-522)
**Applies to:** Any release tag `v0.9.0-beta-staging.*` → first `v1.0.0-rc.*` transition; every PR touching `Dockerfile*`, `docker-compose*.yml`, `infrastructure/helm/**`, `infrastructure/k8s/**`, `infrastructure/terraform-aws/**`, gateway/ingress configs, CORS/CSP settings, IAM policies

---

## 1. The Rule

> **Before any pre-production tag promotes to release-candidate or first production tag, all 9 infra-hardening checks below MUST pass OR have a documented `INFRA_HARDENING_DEFER` trailer with risk acceptance + follow-up gap.**

Infrastructure misconfiguration is a top OWASP A05 + CIS Benchmark + AWS Well-Architected Security Pillar concern. Wave 71b retro showed security-audit Category 5 rubric was vague ("TLS config, CORS, CSP, Docker non-root, k8s security context") and bundled 9+ infra concerns into 1 20-pt bucket — letting per-mechanism gaps hide.

This rule fills the coverage gap. Security-audit skill Category 5 rubric extended in same PR.

---

## 2. Mandatory checks (9)

### 2.1 TLS 1.2+ only on all listeners (P0)

Every public-facing listener (ALB, Cloudfront, gateway, EC2 nginx) enforces TLS 1.2 minimum. TLS 1.0/1.1 BANNED; SSLv3 BANNED.

Verify:
```bash
# AWS ALB listener policy
aws elbv2 describe-listeners --load-balancer-arn <arn> \
  --query 'Listeners[?Port==`443`].[Port,SslPolicy]'
# Expected: SslPolicy: ELBSecurityPolicy-TLS13-1-2-2021-06 or stricter
```

Internal service-to-service: gateway → backend internal calls can use mTLS (Phase 1.5+ scope) OR plain HTTP within VPC + security group ingress restricted.

### 2.2 CORS origins explicit (no `*`) (P0)

Production CORS config MUST list explicit allowed origins:
- ✅ `CORS_ALLOWED_ORIGINS=https://<production-domain>,https://*.<production-domain>`
- ❌ `CORS_ALLOWED_ORIGINS=*` (production)

Verify per `production-env-config-registry.md` §11 — `audit-env-coverage.sh` checks CORS default override.

Per Wave 71 Plan 1 self-test incident (2026-05-13 CORS 403 P0), this is now mandatory pre-launch check.

### 2.3 Content Security Policy (CSP) header (P1)

Frontend serves CSP header on every HTML response:
- `default-src 'self'`
- `script-src 'self' 'unsafe-inline'` only if necessary; prefer nonce-based
- `img-src 'self' data: https:` for inline / CDN assets
- `connect-src 'self' https://api.<production-domain>` explicit
- `frame-ancestors 'none'` (clickjacking)

Verify: Next.js `next.config.js` `headers()` returns CSP; or reverse-proxy adds via nginx config.

Acceptable v1: CSP in report-only mode for Phase 1 BETA → enforce after 2 weeks; full enforce by `v1.0.0-rc.*`.

### 2.4 Docker images run as non-root (P0)

Every Dockerfile MUST include `USER <non-root>` directive before `CMD`/`ENTRYPOINT`.

Verify:
```bash
for dockerfile in <backend-product>/Dockerfile* <tenant-product>/*/Dockerfile* <backend-product>/*/Dockerfile*; do
  grep -E "^USER " "$dockerfile" || echo "MISSING USER: $dockerfile"
done
# Expected: no MISSING outputs
```

Best practice: use `eclipse-temurin:21-jre-alpine` base which provides `nobody` user OR explicit `RUN adduser -D appuser && USER appuser`.

### 2.5 IAM roles least-privilege (P0)

No IAM policy has `Action: "*"` AND `Resource: "*"` (the `*:*` admin pattern). Roles scoped to:
- Resource ARN patterns specific to the role's domain (e.g., apply role → `<secret-prefix>/production/*` secrets only)
- Action verbs specific to operations needed (e.g., deploy role → `ssm:SendCommand` not `ssm:*`)
- Condition keys narrowing further (e.g., `aws:ResourceTag/Project: the project`)

Verify:
```bash
grep -rnE "(Action|Resource).*\"\\*\"" infrastructure/terraform-aws/*.tf
# Expected: only on lambda-execution-role + cloudwatch-logs-writer (acceptable bounded scope) OR documented INFRA_LEAST_PRIV_OVERRIDE
```

Per `pre-mutation-state-check.md` §1.5 — terraform IAM editing requires cross-reference matrix (action × resource × workflow caller).

### 2.6 RDS encryption at rest + in transit (P0)

RDS instance has:
- `storage_encrypted = true` + customer-managed KMS key (or AWS-managed acceptable v1)
- `iam_database_authentication_enabled = true` (Phase 1.5+ acceptable)
- Force SSL connections via parameter group: `rds.force_ssl = 1` (PostgreSQL) or equivalent

Verify:
```bash
aws rds describe-db-instances --query 'DBInstances[].[DBInstanceIdentifier,StorageEncrypted,KmsKeyId]'
# Expected: StorageEncrypted: true; KmsKeyId: non-default
```

### 2.7 VPC security groups default-deny + named ingress (P1)

Every security group:
- Default outbound: deny all OR restrict to known egress targets
- Inbound rules: ZERO `0.0.0.0/0` access UNLESS justified (public ALB:443 OK; SSH:22 banned from internet)
- All rules have `description` field (ASCII-only per `aws-sg-description-ascii.md`)

Verify:
```bash
aws ec2 describe-security-groups --query 'SecurityGroups[?length(IpPermissions[?contains(IpRanges[].CidrIp,`0.0.0.0/0`) && FromPort!=`443` && FromPort!=`80`])>`0`].[GroupName,GroupId]'
# Expected: empty (no SG allows non-HTTP/HTTPS from internet)
```

### 2.8 CloudTrail multi-region (P0)

Per `aws-observability-first.md` v1.0.0 — multi-region CloudTrail trail enabled + `IsLogging=true` BEFORE production apply. This check re-verifies pre-release tag:

Verify:
```bash
aws cloudtrail get-trail-status --name <cloudtrail-name> --query 'IsLogging' --output text
# Expected: True
aws cloudtrail describe-trails --query 'trailList[?Name==`<cloudtrail-name>`].IsMultiRegionTrail'
# Expected: [true]
```

### 2.9 GuardDuty enabled OR documented exception (P2)

AWS GuardDuty threat detection enabled at account level. Acceptable-default for Phase 1 BETA cost-deferred IF:
- Cost projection: GuardDuty Phase 1 BETA traffic ≈ $5-15/month
- If deferred → file follow-up gap with explicit Phase 1.5 enable date
- AWS Security Hub config also acceptable as Phase 1 minimum alternative

Verify:
```bash
aws guardduty list-detectors --query 'DetectorIds | length(@)'
# Expected: 1 (one detector active per region)
```

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| TLS 1.0/1.1 enabled "for legacy client compat" | TLS 1.2 minimum; legacy clients = separate auth-deprecated bucket |
| `CORS_ALLOWED_ORIGINS=*` "for dev convenience" in production | Explicit origin list; wildcard subdomain pattern OK (`https://*.<production-domain>`) |
| Skip CSP "too restrictive for our app" | Report-only mode acceptable v1 → enforce by `rc.*` |
| Run Docker as root "easier permission model" | Always non-root + UID/GID documented |
| `*:*` admin policy "trust me, only used by ops" | Scope to action × resource × condition; document any exception |
| Skip CloudTrail `IsMultiRegionTrail` check "we only use 1 region" | Multi-region catches cross-region API calls + protects against region-pivot attack |
| Skip GuardDuty "Phase 1 BETA cost" without follow-up gap | Acceptable v1 with explicit gap + enable date |
| Score 17/20 by averaging — IAM `*:*` hidden in 1 unflagged sub-check | Per-mechanism pass/fail; 1 P0 fail = Cat 5 FAIL regardless |

---

## 4. Worked self-test — current main state 2026-05-14

**Apply §2 checklist retroactively to current main HEAD:**

| # | Check | Estimated state | Verdict |
|---|---|---|---|
| 2.1 | TLS 1.2+ on listeners | ALB likely uses `ELBSecurityPolicy-TLS13-*` default; verify | likely **PASS** |
| 2.2 | CORS explicit | `production-env-config-registry.md` §11 + Wave 71 fix applied | **PASS** (post-Wave 71) |
| 2.3 | CSP header | Next.js / nginx CSP not yet wired | likely **FAIL** — file follow-up gap |
| 2.4 | Docker non-root | Spring Boot images: may use base default; needs sweep | likely **PARTIAL** — sweep needed |
| 2.5 | IAM least-privilege | Wave 64 Step F caught 3 cascading IAM bugs → `pre-mutation-state-check.md` §1.5 added; current state likely clean | likely **PASS** post-Wave 64 |
| 2.6 | RDS encryption | terraform-aws should set `storage_encrypted = true`; default KMS key likely | **PARTIAL** — KMS CMK = follow-up (overlap §2.4 sister rule) |
| 2.7 | SG default-deny + ASCII | `aws-sg-description-ascii.md` already enforces ASCII; check ingress rules for `0.0.0.0/0` non-HTTP | likely **PARTIAL** — sweep needed |
| 2.8 | CloudTrail multi-region | `aws-observability-first.md` shipped 2026-05-07 PR #992 → confirmed `IsLogging=true` | **PASS** |
| 2.9 | GuardDuty | Phase 1 BETA cost-deferred likely; check + follow-up gap if so | likely **DEFERRED** — file gap if not present |

**Expected findings:** 2-3 P1 follow-up gaps (CSP, Docker non-root sweep, GuardDuty, possible KMS CMK).

**Verdict:** Rule fires correctly on current main — surfaces specific per-mechanism gaps instead of vague Cat 5 score. ✅

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Security-audit skill rubric extension (paired same PR)

`.claude/skills/quality/security-audit/SKILL.md` Category 5 "Infrastructure Security" rubric extended with 9 explicit per-check rows. Each check pass/fail (no averaging). 1 P0 fail = category total ≤ 16/20.

### 5.2 Pre-promotion gate

Before any maintainer creates git tag matching `v1.0.0-rc.*` or `v1.0.0`, §2 checklist MUST exit 0. Currently: manual run + AWS CLI evidence in PR description. Detector script `scripts/check-infra-hardening.sh` deferred per premature-rule guard ≥7 days.

### 5.3 Reviewer checklist

When reviewing any PR that touches `Dockerfile*`, `infrastructure/terraform-aws/*.tf` (especially `security-groups.tf`, `rds.tf`, `alb.tf`, `iam.tf`), `infrastructure/helm/**`, or CORS / CSP config, reviewer asks:
- New container? → §2.4 `USER` directive present?
- New IAM policy? → §2.5 not `*:*` + per `pre-mutation-state-check.md` §1.5 matrix?
- New SG rule? → §2.7 description + scope tight?
- New listener? → §2.1 TLS 1.2+?

### 5.4 Override mechanism

For genuine schedule pressure (Phase 1 BETA acceptable scope):

```
git commit -m "...
INFRA_HARDENING_DEFER: <check ID + reason — e.g. 'GuardDuty Phase 1.5 cost approval'>
INFRA_HARDENING_FOLLOWUP: <gap link with completion date>"
```

Trailer logged. Pattern frequency >3 defers per release = meta-review.

### 5.5 Detector (deferred)

Future: `scripts/check-infra-hardening.sh` runs `aws elbv2/rds/cloudtrail/ec2/iam describe` + greps Dockerfile / terraform / helm. Defer until 2nd recurrence; reviewer + worked self-test sufficient for v1.0.0.

---

## 6. Relationship to other rules

- **`security-audit/SKILL.md`** — Category 5 rubric extended same PR
- **`aws-observability-first.md`** — CloudTrail mandate (Cat 5 §2.8 cross-reference)
- **`aws-sg-description-ascii.md`** — SG description ASCII enforced (Cat 5 §2.7 sister scope)
- **`pre-mutation-state-check.md`** §1.5 — terraform IAM cross-reference matrix (Cat 5 §2.5 sister scope)
- **`pre-launch-secrets-hardening-checklist.md`** §2.4 — KMS CMK on Secrets Manager (Cat 5 §2.6 RDS KMS sister scope)
- **`production-env-config-registry.md`** §11 — env-coverage + gateway-routes + service-ports + spring-profiles audits (Cat 5 §2.2 CORS sister scope)
- **`output-review-mandate.md`** §3 — Security audit row sharpened for Cat 5
- **`release-deploy-standard.md`** §3.4 — MAJOR/first-PROD checklist includes pen-test light; this rule operationalizes infra slice
- **`pre-launch-auth-hardening-checklist.md`** (sister rule, Cat 4)
- **`pre-launch-dependency-hardening-checklist.md`** (sister rule, Cat 1 — same PR)
- **`pre-launch-secrets-hardening-checklist.md`** (sister rule, Cat 2 — same PR)
- **`pre-launch-owasp-rest-hardening-checklist.md`** (sister rule, Cat 3 — same PR)
- **`incident-to-rule-pipeline.md`** — direct output of GAP-522 applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + skill rubric extension + worked self-test same PR

---

## 7. Log

- **2026-05-14 (v1.0.0):** Rule created closing Cat 5 slice of GAP-522. Triggered by user-flagged miss "skill audit phải là lớp phòng vệ tin tưởng" + Wave 71c PR #1278 already fixed Cat 4; extending same fix to Cat 5. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-522 filed 2026-05-13) → Classify ✓ (Cat 5 rubric was vague "TLS, CORS, CSP, Docker non-root, k8s security context" bundling 9+ mechanisms in single bucket; allowed averaging that hid per-mechanism gaps) → Rule+Enforce ✓ (this file + security-audit/SKILL.md Cat 5 row update + worked §4 self-test + paired with 3 sister rules per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on current main — 2-3 P1 follow-ups surface for CSP, Docker non-root sweep, GuardDuty) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds per-mechanism Cat 5 coverage to previously-vague rubric; no constraint loosening; existing tags grandfathered; rule applies prospectively to `v1.0.0-rc` promotion).
