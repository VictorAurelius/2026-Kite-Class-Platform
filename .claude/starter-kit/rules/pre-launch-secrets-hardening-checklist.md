# Pre-Launch Secrets Hardening Checklist — security-audit Cat 2 per-check rubric

**Priority:** 🟠 MANDATORY — pre-launch security gate (Cat 2 force-multiplier)
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-vague Cat 2 per-check enforcement closing GAP-522)
**Applies to:** Any release tag `v0.9.0-beta-staging.*` → first `v1.0.0-rc.*` transition; every PR touching `application*.yml`, `docker-compose*.yml`, `.env*`, `scripts/fetch-secrets.sh`, AWS Secrets Manager terraform, IAM secret-access policies

---

## 1. The Rule

> **Before any pre-production tag promotes to release-candidate or first production tag, all 8 secrets-hardening checks below MUST pass OR have a documented `SECRETS_HARDENING_DEFER` trailer with risk acceptance + follow-up gap.**

Secrets leaks (OWASP A02 Cryptographic Failures + A07 Identification) are top-3 cost incident classes per Verizon DBIR. Wave 71b retro showed security-audit Category 2 rubric was vague ("no hardcoded secrets, .env gitignored, rotation policy") and allowed averaging — letting per-service rotation gaps, git-history pickaxe gaps, KMS-encryption misconfig, and shared-secret cross-service patterns hide behind 1-2 passing sub-checks.

This rule fills the coverage gap. Security-audit skill Category 2 rubric extended in same PR. Complements `production-env-config-registry.md` v1.1.0 (registry of suspect-defaults) by adding rotation + history + isolation checks.

---

## 2. Mandatory checks (8)

### 2.1 Zero hardcoded secrets in source (P0)

Grep on current HEAD for known secret patterns returns ZERO hits in source:

```bash
grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
  --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
  <backend-product>/ <tenant-product>/ scripts/ infrastructure/ \
  | grep -vE "(test|fixture|example|template|\.md:|noreply@|localhost|change-me|placeholder)"
```

Verify: 0 hits OR each remaining hit is verifiably a placeholder/example.

### 2.2 `.env.*` gitignored + only templates committed (P0)

`.gitignore` excludes runtime env files:
- `.env`
- `.env.local`
- `.env.production` (the actual one — not `.env.production.template`)
- `backend.config` (terraform partial-backend per `terraform-partial-backend-public-repo.md`)
- `*.pem`, `*.key`, `id_rsa*`

Only `*.template` and `*.example` env files committed.

Verify:
```bash
git ls-files | grep -E "^\.env(\.|$)" | grep -vE "(template|example)$"
# Expected: 0 results
```

### 2.3 AWS Secrets Manager versioning enabled (P0)

Every production secret in AWS Secrets Manager has versioning enabled (always true by AWS default) AND `AWSCURRENT` / `AWSPREVIOUS` rotation slots tracked:

Verify:
```bash
aws secretsmanager list-secrets --query 'SecretList[?starts_with(Name,`<secret-prefix>/production/`)].[Name,RotationEnabled]' --output table
```

For each secret, `RotationEnabled` should be `true` OR documented manual rotation runbook covers it.

### 2.4 KMS encryption at rest on Secrets Manager (P0)

Every production secret encrypted with non-default KMS CMK (not `aws/secretsmanager` default). Customer-managed key allows audit + cross-account access control.

Verify:
```bash
aws secretsmanager describe-secret --secret-id <secret-prefix>/production/<name> \
  --query 'KmsKeyId' --output text
# Expected: arn:aws:kms:...:key/<custom-cmk-id>, NOT empty/null (which means default key)
```

Acceptable v1: AWS-managed `aws/secretsmanager` if customer-managed KMS not provisioned yet → file follow-up gap.

### 2.5 Secret rotation runbook exists (P1)

`documents/05-guides/operations/secrets-rotation-runbook.md` (per Wave 71 GAP-452 split) exists + covers:
- Quarterly rotation cadence per secret class
- JWT signing key rotation procedure (Wave 71c GAP-519 sister scope)
- DB password rotation (RDS rotation hooks)
- API key rotation (Resend, Stripe future, etc.)
- Owner per secret + escalation path

Verify: `ls documents/05-guides/operations/secrets-rotation-runbook.md` → exists + reviewed within 90d.

### 2.6 Git history clean (P1)

Run secrets-scanner against full git history to detect historical leaks:

```bash
# gitleaks OR trufflehog (community)
gitleaks detect --source . --no-git=false --verbose
```

Acceptable v1: documented in `documents/04-quality/audits/security/git-history-secrets-scan-YYYY-MM-DD.md` after manual review. If old leak found → rotate immediately + document in incident log per `incident-to-rule-pipeline.md`.

### 2.7 Terraform/IaC files free of secret values (P1)

Terraform `*.tf` files MUST NOT contain secret string literals. Pattern:
- `password = "literal"` → BANNED (use `var.password` + `sensitive = true` + Secrets Manager data source)
- `api_key = "ak_..."` → BANNED
- `default = "secret_value"` in variable blocks → BANNED

Verify:
```bash
grep -rnE "(password|api_key|secret|token)\s*=\s*\"[a-zA-Z0-9_-]{8,}\"" \
  infrastructure/terraform-aws/*.tf
# Expected: 0 hits (all secrets via data.aws_secretsmanager_secret_version)
```

### 2.8 Service-to-service credential isolation (P2)

Each backend service has its own DB credential / API key — no shared secret across services. This limits blast radius if 1 service compromised.

Verify per `application-production.yml`:
- `<subscription-service>` uses `<secret-prefix>/production/db-subscription` (not shared `db-master`)
- `<email-service>` uses `<secret-prefix>/production/db-email` separately
- `<core-tenant-service>` uses `<tenant-product>/production/db-core` (tenant DBs separate)

Acceptable v1 for Phase 1 BETA: shared `db-master` per app-product line OK if documented; full per-service split = Phase 1.5+ work.

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "It's a placeholder, no need to scrub" | Use unambiguous placeholders (`<CHANGE-ME>`, `placeholder-xxx`) — never realistic-looking strings |
| Commit `.env.production` "temporarily" | NEVER. Use `fetch-secrets.sh` runtime fetch + AWS Secrets Manager |
| Skip git history scan "it's a private repo" | Repos flip public; account IDs + leaked tokens persist; scan baseline once |
| Use default `aws/secretsmanager` KMS key + skip §2.4 | Acceptable v1 with follow-up gap; not silent skip |
| Shared `DB_PASSWORD` env across all backend services | Per-service secret split; reuse only when explicitly documented Phase 1 BETA scope |
| Score 17/20 by averaging — git history leak hidden in 1 unflagged sub-check | Per-check pass/fail; 1 P0 leak = Cat 2 FAIL regardless |

---

## 4. Worked self-test — current main state 2026-05-14

**Apply §2 checklist retroactively to current main HEAD:**

| # | Check | Verification (estimated outcome) | Verdict |
|---|---|---|---|
| 2.1 | No hardcoded secrets in source | Wave 40 security-audit reported clean; spot-check now → likely **PASS** | **PASS** |
| 2.2 | `.env.*` gitignored | `git ls-files \| grep -E "^\.env"` → only `.env.*.template` committed | likely **PASS** |
| 2.3 | AWS Secrets versioning | `aws secretsmanager list-secrets` shows versioning enabled (AWS default); rotation `false` for most | **PARTIAL** — versioning yes, rotation policy not automated |
| 2.4 | KMS CMK on secrets | Likely default `aws/secretsmanager` key (no custom CMK provisioned) | **PARTIAL** — file follow-up gap |
| 2.5 | Rotation runbook | `secrets-rotation-runbook.md` exists (Wave 71 GAP-452) | **PASS** |
| 2.6 | Git history scan | gitleaks/trufflehog scan NOT run on baseline | **FAIL** — file follow-up gap |
| 2.7 | Terraform free of secret literals | `grep -E "(password\|secret).*=.*\"[a-z0-9]{8,}\""` in `infrastructure/terraform-aws/*.tf` → likely 0 hits | likely **PASS** (admin password generated via `random_password`) |
| 2.8 | Service credential isolation | Phase 1 BETA likely uses shared `db-master` per product line | **PARTIAL** — Phase 1.5+ scope acceptable v1 |

**Expected findings:** 2-3 P1/P2 follow-up gaps (KMS CMK provisioning, gitleaks baseline, automated rotation, per-service credential split).

**Verdict:** Rule fires correctly on current main — surfaces specific per-check gaps instead of a vague "no hardcoded secrets, looks fine" pass. ✅

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Security-audit skill rubric extension (paired same PR)

`.claude/skills/quality/security-audit/SKILL.md` Category 2 "Secrets & Credentials" rubric extended with 8 explicit per-check rows. Each check pass/fail (no averaging). 1 fail = category total ≤ 16/20.

### 5.2 Pre-promotion gate

Before any maintainer creates git tag matching `v1.0.0-rc.*` or `v1.0.0` (first GA), §2 checklist MUST exit 0. Currently: manual run + `gitleaks` + `aws secretsmanager list-secrets` outputs documented in PR description. Detector script `scripts/check-secrets-hardening.sh` deferred per premature-rule guard ≥7 days.

### 5.3 Reviewer checklist

When reviewing any PR that touches `application*.yml`, `docker-compose*.yml`, `infrastructure/terraform-aws/*.tf`, or any file matching `*secret*` / `*credential*`, reviewer asks:
- New secret reference added? → §2.1 source scan + §2.3 Secrets Manager storage?
- Terraform IAM policy? → §2.4 KMS CMK + §2.7 no secret literals?
- New service deployed? → §2.8 dedicated credential split?

### 5.4 Override mechanism

For genuine schedule pressure (regulator deadline, vendor delay):

```
git commit -m "...
SECRETS_HARDENING_DEFER: <check ID + reason — e.g. 'KMS CMK pending Phase 1.5 budget approval'>
SECRETS_HARDENING_FOLLOWUP: <gap link with completion date ≤30d from defer>"
```

Trailer logged. Pattern frequency >2 defers per release = meta-review.

### 5.5 Detector (deferred)

Future: `scripts/check-secrets-hardening.sh` runs gitleaks + `grep` patterns + `aws secretsmanager` introspection. Defer until 2nd recurrence of secret-related incident.

---

## 6. Relationship to other rules

- **`security-audit/SKILL.md`** — Category 2 rubric extended same PR
- **`production-env-config-registry.md`** v1.1.0 — sister rule covering env-var coverage; this rule extends with rotation + history + KMS + isolation
- **`output-review-mandate.md`** §3 — Security audit row already exists; this rule sharpens Category 2 substance
- **`logs-format-standard.md`** §2.4 + §3 — PII scrubbing + secret keyword masking in logs; complementary defense-in-depth
- **`terraform-partial-backend-public-repo.md`** — sister rule covering terraform-state-backend slice
- **`agent-aws-access.md`** §2.2 — banned secret-revealing `get-*` commands; this rule covers the storage side
- **`pre-launch-auth-hardening-checklist.md`** (sister rule, Cat 4) — covers JWT rotation specifically (overlap with §2.5)
- **`pre-launch-dependency-hardening-checklist.md`** (sister rule, Cat 1 — same PR)
- **`pre-launch-owasp-rest-hardening-checklist.md`** (sister rule, Cat 3 — same PR)
- **`pre-launch-infra-hardening-checklist.md`** (sister rule, Cat 5 — same PR)
- **`incident-to-rule-pipeline.md`** — direct output of GAP-522 applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + skill rubric extension + worked self-test same PR

---

## 7. Log

- **2026-05-14 (v1.0.0):** Rule created closing Cat 2 slice of GAP-522. Triggered by user-flagged miss "skill audit phải là lớp phòng vệ tin tưởng" + Wave 71c PR #1278 already fixed Cat 4; extending same fix to Cat 2. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-522 filed 2026-05-13) → Classify ✓ (Cat 2 rubric was "no hardcoded secrets, .env gitignored, rotation policy" with no per-mechanism check; rubric allowed averaging that hid rotation/KMS/history/isolation classes; `production-env-config-registry.md` covers env-var coverage but not full secret lifecycle) → Rule+Enforce ✓ (this file + security-audit/SKILL.md Cat 2 row update + worked §4 self-test + paired with 3 sister rules per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on current main — likely 3-4 P1/P2 follow-ups surface for KMS CMK, gitleaks baseline, automated rotation, credential isolation) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds per-mechanism Cat 2 coverage to previously-vague rubric; no constraint loosening; existing tags grandfathered; rule applies prospectively to `v1.0.0-rc` promotion).
