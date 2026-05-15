# Security Audit Report — v2 Format Template

Skeleton cho audit report v2 per GAP-564 (Wave 80 Bucket A). Mỗi control trong cả 5 categories MUST có per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) per SOC2 Type II Section 4 / ISO27001 Annex A / OWASP ASVS baseline.

**Cách dùng:** copy template này sang `documents/04-quality/audits/security/security-audit-YYYY-MM-DD.md`, điền theo từng section.

---

## Frontmatter (mandatory)

```yaml
---
title: Security Audit — <topic / wave / phase>
status: complete
created: YYYY-MM-DD
phase: <Phase 1 BETA pre-launch / Phase 1.5 PAID / Phase 2 / Phase 3 K-12>
wave: <NN>
auditor: <agent ID + model + session ID>
gaps: [GAP-NNN, ...]
baseline_security_100: <prior score + date + ref>
audit_format_version: v2
evidence_dir: documents/04-quality/audits/security/evidence/YYYY-MM-DD/
---
```

---

## 1. Header

**Phạm vi audit:** commit range `<sha1>..<sha2>` — N PRs / wave / scope description.

**Method:** Per `.claude/skills/quality/security-audit/SKILL.md` v2 — per-check pass/fail (no averaging) + per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) per GAP-564.

**Baselines so sánh:**
- <prior audit + date>: <score>
- <prior audit + date>: <score>

---

## 2. Methodology

**Tools used:**
- `pnpm audit --json --audit-level=high` (Cat 1 FE)
- `mvn dependency-check:check -DfailBuildOnCVSS=7` (Cat 1 BE)
- `grep -rnE` (Cat 2 + Cat 3 source scan)
- `aws elbv2 describe-listeners` / `aws rds describe-db-instances` / etc. (Cat 5)
- AWS Secrets Manager `describe-secret` (Cat 2)

**Scope coverage:**
- File paths scanned: `kitehub/`, `kiteclass/`, `scripts/`, `infrastructure/`, `documents/`
- Modules: <list>
- Environments: <production / staging / dev>
- Time window: <commit range / wave merge window>

**Sampling strategy:**
- Cat 1: 100% deps (all package manifests)
- Cat 2: 100% grep coverage on source (per GAP-564 mandate — include `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/`)
- Cat 3: 100% per-OWASP-item (9 items)
- Cat 4: 100% auth endpoints
- Cat 5: 100% infra checks (AWS resources via Tier 1 read-only)

---

## 3. Score Summary

| # | Category (20pt) | Score | Verdict | Evidence blocks |
|---|-----------------|:-----:|:-------:|:---------------:|
| 1 | Dependency Vulnerabilities | XX/20 | 🟢/🟡/🔴 | N |
| 2 | Secrets & Credentials | XX/20 | 🟢/🟡/🔴 | N |
| 3 | OWASP A01-A06/A08-A10 | XX/20 | 🟢/🟡/🔴 | N (≥9) |
| 4 | Auth & Access Control (A07) | XX/20 | 🟢/🟡/🔴 | N |
| 5 | Infrastructure Security | XX/20 | 🟢/🟡/🔴 | N |

**Tổng: XX/100 — Grade** (delta vs baseline).

**v2 evidence completeness:** N/Y total expected (target 100%).

---

## 4. Bug List (deliverable — surface BEFORE score)

### P0 — BLOCKING v1.0.0-rc promotion

**P0-1: <title>**
- File: <path:line>
- **Impact:** <description>
- **Fix:** <proposed>
- **Evidence:** EVIDENCE-YYYY-MM-DD-<CAT>-NNN

### P1 — Should fix before v1.0.0-rc

**P1-1: <title>**
- (same structure)

### P2 — Track for Phase 1.5+

**P2-1: <title>**

---

## 5. Per-Category Evidence Blocks (v2 mandatory — ≥15 blocks total)

### Cat 1 — Dependency Vulnerabilities (≥4 evidence blocks)

#### DEPS-001 — Frontend pnpm audit clean (P0)

**Control:** Per `pre-launch-dependency-hardening-checklist.md` §2.1 — `pnpm audit --json` returns ZERO CRITICAL/HIGH across both FE apps.

- **Command run:**
  ```bash
  cd kitehub/kitehub-frontend && pnpm audit --json --audit-level=high
  cd kiteclass/kiteclass-frontend && pnpm audit --json --audit-level=high
  ```
- **Output:**
  ```
  <stdout snippet — vd "found 0 vulnerabilities" hoặc JSON summary>
  ```
- **Verdict:** ✅ PASS — 0 high/critical across cả 2 FE apps. (Hoặc ❌ FAIL với hit count + rationale.)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-001`

#### DEPS-002 — Backend Maven dependency-check (P0)

**Control:** Per §2.2 — `mvn dependency-check` returns ZERO CRITICAL/HIGH CVSS ≥7.

- **Command run:**
  ```bash
  cd kitehub && ./mvnw -pl <module> dependency-check:check -DfailBuildOnCVSS=7
  cd kiteclass/kiteclass-core && ./mvnw dependency-check:check -DfailBuildOnCVSS=7
  ```
- **Output:**
  ```
  <BUILD SUCCESS hoặc dependency-check report summary>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-002`

#### DEPS-003 — Trivy container image scan (P0)

**Control:** Per `release-deploy-standard.md` §3.1 + sister rule — Trivy scans built images for CRITICAL/HIGH OS + library CVEs.

- **Command run:**
  ```bash
  trivy image --severity HIGH,CRITICAL --format json <image:tag>
  ```
- **Output:**
  ```
  <trivy summary — count by severity>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-003`

#### DEPS-004 — SBOM artifact attached to release (P2)

**Control:** Per §2.8 — CycloneDX SBOM generated per release tag.

- **Command run:**
  ```bash
  ls documents/04-quality/audits/security/sbom/ | grep <version>
  # OR
  gh release view <tag> --json assets --jq '.assets[].name'
  ```
- **Output:**
  ```
  <list SBOM artifact files>
  ```
- **Verdict:** ✅/❌/⚠️ PARTIAL (manual generation acceptable v1)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-004`

---

### Cat 2 — Secrets & Credentials (≥4 evidence blocks)

#### SEC-001 — Zero hardcoded secrets in source (P0)

**Control:** Per `pre-launch-secrets-hardening-checklist.md` §2.1 — grep mandate covering `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/`. (Mandatory scope expansion per GAP-564 §6 self-test — Wave 78 incident.)

- **Command run:**
  ```bash
  grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
    --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
    kitehub/ kiteclass/ scripts/ infrastructure/ \
    | grep -vE "(test|fixture|example|template|\.md:|noreply@|localhost|change-me|placeholder)"
  ```
- **Output:**
  ```
  <full grep output OR explicit "0 hits"; classify each remaining hit>
  ```
- **Verdict:** ✅ PASS (0 hits) / ❌ FAIL (N hits — list line numbers)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-001`

#### SEC-002 — .env.* gitignored + only templates committed (P0)

**Control:** Per §2.2 — runtime env files gitignored, templates only.

- **Command run:**
  ```bash
  git ls-files | grep -E "^\.env(\.|$)" | grep -vE "(template|example)$"
  ```
- **Output:**
  ```
  <expected: empty; otherwise list paths to investigate>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-002`

#### SEC-003 — AWS Secrets Manager versioning + KMS (P0)

**Control:** Per §2.3 + §2.4 — versioning enabled + customer-managed KMS CMK.

- **Command run:**
  ```bash
  aws secretsmanager list-secrets \
    --query 'SecretList[?starts_with(Name,`kitehub/production/`)].[Name,RotationEnabled,KmsKeyId]' \
    --output table
  ```
- **Output:**
  ```
  <table output showing secrets + rotation + KMS arn>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-003`

#### SEC-004 — Terraform IaC scan (P1)

**Control:** Per §2.7 — terraform `*.tf` files free of secret literals.

- **Command run:**
  ```bash
  grep -rnE "(password|api_key|secret|token)\s*=\s*\"[a-zA-Z0-9_-]{8,}\"" \
    infrastructure/terraform-aws/*.tf
  ```
- **Output:**
  ```
  <expected: 0 hits>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-004`

---

### Cat 3 — OWASP A01-A06/A08-A10 (≥9 evidence blocks — 1 per item)

#### OWASP-A01-001 — Broken Access Control (P0)

**Control:** Per `pre-launch-owasp-rest-hardening-checklist.md` §2.1 — every admin/privileged endpoint has explicit `@PreAuthorize`.

- **Command run:**
  ```bash
  grep -rn "@PreAuthorize" kitehub/*/src/main/java/**/*AdminController.java | wc -l
  grep -rnE "@(Post|Put|Patch|Delete|Get)Mapping" kitehub/*/src/main/java/**/*AdminController.java | wc -l
  # Compare counts — should match (every admin endpoint has @PreAuthorize)
  ```
- **Output:**
  ```
  @PreAuthorize count: N
  Mapping count: M
  Coverage: N/M (X%)
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A01-001`

#### OWASP-A02-001 — Cryptographic Failures (P0)

**Control:** Per §2.2 — no weak ciphers (MD5/SHA1/DES/RC4).

- **Command run:**
  ```bash
  grep -rnE "MessageDigest\.getInstance\(\"(MD5|SHA-1)\"\)" kitehub/ kiteclass/ --include="*.java"
  ```
- **Output:** `<expected 0 hits>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A02-001`

#### OWASP-A03-001 — Injection (P0)

**Control:** Per §2.3 — parameterized queries only.

- **Command run:**
  ```bash
  grep -rnE "(SELECT|UPDATE|DELETE|INSERT).*\+\s*\w+\s*\+|String\.format.*WHERE.*%" \
    kitehub/ kiteclass/ --include="*.java"
  ```
- **Output:** `<expected 0 hits non-test>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A03-001`

#### OWASP-A04-001 — Insecure Design (P1)

**Control:** Per §2.4 — threat models per critical flow.

- **Command run:**
  ```bash
  ls documents/02-architecture/threat-models/*.md
  ```
- **Output:** `<list files OR "directory not found">`
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A04-001`

#### OWASP-A05-001 — Security Misconfiguration (P1)

**Control:** Per §2.5 — production profile hardened (actuator scoped, no stacktrace).

- **Command run:**
  ```bash
  grep -A2 "management.endpoints.web.exposure" \
    kitehub/*/src/main/resources/application-production.yml
  grep -A2 "include-stacktrace\|include-message" \
    kitehub/*/src/main/resources/application-production.yml
  ```
- **Output:**
  ```
  <config snippets — actuator should be 'health' only; stacktrace 'never'>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A05-001`

#### OWASP-A06-001 — Vulnerable Components (delegated to Cat 1)

**Control:** Cross-reference DEPS-001 + DEPS-002 + DEPS-003 evidence blocks.

- **Verdict:** PASS if Cat 1 PASS; else delegate findings.
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A06-001` → references DEPS-001 to DEPS-003

#### OWASP-A08-001 — Software & Data Integrity (P1)

**Control:** Per §2.7 — Docker images + GH Actions SHA-pinned.

- **Command run:**
  ```bash
  grep -rn "image:" Dockerfile* docker-compose*.yml | grep -v "@sha256"
  grep -rn "uses:" .github/workflows/*.yml | grep -v "@[a-f0-9]\{40\}"
  ```
- **Output:** `<list non-SHA-pinned refs>`
- **Verdict:** ✅/❌/⚠️ PARTIAL (tag-pinned + Dependabot active = acceptable v1)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A08-001`

#### OWASP-A09-001 — Logging & Monitoring (P1)

**Control:** Per §2.8 — admin_audit_log entity + PII scrubbing per `logs-format-standard.md`.

- **Command run:**
  ```bash
  grep -rn "AdminAuditLog\|admin_audit_log" kitehub/ --include="*.java"
  ```
- **Output:** `<list entity + interceptor refs>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A09-001`

#### OWASP-A10-001 — SSRF (P1)

**Control:** Per §2.9 — outbound HTTP clients have URL allowlist.

- **Command run:**
  ```bash
  grep -rnE "(RestTemplate|WebClient|HttpClient).*\.(get|post|exchange)" \
    kitehub/ kiteclass/ --include="*.java" | grep -iE "user|input|url"
  ```
- **Output:** `<each hit reviewed for allowlist>`
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A10-001`

---

### Cat 4 — Auth & Access Control (OWASP A07) (≥4 evidence blocks)

#### AUTH-001 — Auth endpoints rate-limited (P0)

**Control:** Per `pre-launch-auth-hardening-checklist.md` §2.1 — gateway RequestRateLimiter coverage matrix.

- **Command run:**
  ```bash
  grep -A5 'id: auth\|id: kitehub-auth' \
    kitehub/kitehub-gateway/src/main/resources/application.yml
  ```
- **Output:**
  ```
  <YAML route snippets — verify per §2.1 7-row matrix (register/login/refresh/verify-email/resend/password-reset/beta-access)>
  ```
- **Verdict:** ✅/❌/⚠️ với coverage matrix M/7
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-001`

#### AUTH-002 — Account lockout (P0)

**Control:** Per §2.2 — 5 failed attempts / 15-min lockout + exponential backoff.

- **Command run:**
  ```bash
  grep -rn "failedLoginAttempts\|accountLocked\|lockoutUntil" \
    kitehub/ --include="*.java"
  ./mvnw -pl kitehub-subscription test -Dtest=AuthServiceLockoutTest
  ```
- **Output:**
  ```
  <entity field + service logic + test output BUILD SUCCESS>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-002`

#### AUTH-003 — 2FA mandatory PLATFORM_ADMIN (P1)

**Control:** Per §2.4 — TwoFactorAuthService + admin login enforces TOTP challenge.

- **Command run:**
  ```bash
  grep -rn "TwoFactorAuthService\|TotpSecretCipher\|@TwoFactorRequired" \
    kitehub/ --include="*.java"
  grep -rn "twofactor\|2fa" \
    kitehub/kitehub-subscription/src/main/java/**/controller/*.java
  ```
- **Output:** `<list 5 endpoints + service implementations>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-003`

#### AUTH-004 — Password complexity (P1)

**Control:** Per §2.3 — PasswordValidator min 12 chars + complexity + reuse check.

- **Command run:**
  ```bash
  grep -rn "PasswordValidator\|MIN_PASSWORD_LENGTH\|zxcvbn" \
    kitehub/ --include="*.java" --include="*.ts"
  ```
- **Output:** `<list validator + applied at registration + reset>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-004`

---

### Cat 5 — Infrastructure Security (≥5 evidence blocks)

#### INFRA-001 — TLS 1.2+ on ALB (P0)

**Control:** Per `pre-launch-infra-hardening-checklist.md` §2.1 — every public listener enforces TLS 1.2+.

- **Command run:**
  ```bash
  aws elbv2 describe-listeners --load-balancer-arn <arn> \
    --query 'Listeners[?Port==`443`].[Port,SslPolicy]' --output table
  ```
- **Output:**
  ```
  <table showing SslPolicy — expect ELBSecurityPolicy-TLS13-1-2-2021-06 or stricter>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-001`

#### INFRA-002 — CORS origins explicit (P0)

**Control:** Per §2.2 — production CORS không có `*`.

- **Command run:**
  ```bash
  bash scripts/audit-env-coverage.sh production | grep -A2 "CORS_ALLOWED_ORIGINS"
  # OR
  aws ssm get-parameters-by-path --path /kitehub/production/cors \
    --query 'Parameters[].[Name,Value]' --output table
  ```
- **Output:** `<CORS config value — expect explicit domain list>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-002`

#### INFRA-003 — Docker non-root USER (P0)

**Control:** Per §2.4 — every Dockerfile has `USER <non-root>` before CMD.

- **Command run:**
  ```bash
  for dockerfile in kitehub/Dockerfile* kiteclass/*/Dockerfile* kitehub/*/Dockerfile*; do
    grep -E "^USER " "$dockerfile" || echo "MISSING USER: $dockerfile"
  done
  ```
- **Output:** `<expected: no MISSING outputs>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-003`

#### INFRA-004 — IAM least-privilege (P0)

**Control:** Per §2.5 — no `Action: "*" + Resource: "*"` admin patterns.

- **Command run:**
  ```bash
  grep -rnE "(Action|Resource).*\"\*\"" infrastructure/terraform-aws/*.tf
  ```
- **Output:** `<list overly-permissive policies — should be only bounded exceptions documented>`
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-004`

#### INFRA-005 — CloudTrail multi-region (P0)

**Control:** Per §2.8 + `aws-observability-first.md` — multi-region trail + `IsLogging=true`.

- **Command run:**
  ```bash
  aws cloudtrail get-trail-status --name kitehub-main --query 'IsLogging' --output text
  aws cloudtrail describe-trails --query 'trailList[?Name==`kitehub-main`].[IsMultiRegionTrail,IncludeGlobalServiceEvents]'
  ```
- **Output:**
  ```
  True
  [[true, true]]
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-005`

---

## 6. Findings Table (linking to evidence artifact IDs)

| Finding ID | Severity | Category | Title | Evidence | Status |
|---|---|---|---|---|---|
| F-001 | P0 | Cat 2 | <title> | EVIDENCE-YYYY-MM-DD-SEC-001 | 🔵 OPEN GAP-NNN |
| F-002 | P1 | Cat 3 A05 | <title> | EVIDENCE-YYYY-MM-DD-OWASP-A05-001 | 🔵 OPEN GAP-NNN |

---

## 7. Aggregate Verdict + Score Delta

| Baseline | Date | Score | This audit delta |
|---|---|:---:|:---:|
| <prior baseline> | YYYY-MM-DD | XX/100 | +/- N |

**Phase 1 BETA threshold ≥80:** ✅ PASS / ❌ FAIL với buffer N điểm.

**v2 evidence completeness:** N/M total expected (target 100% per `audit-skill-rubric-security-audit.md` self-test).

---

## 8. Recommendations

1. <Priority + recommendation + owner>
2. <...>

---

## 9. Pending (post-audit actions)

| Action | Owner | Notes |
|---|---|---|
| File N new gap files | Coordinator | Per `audit-to-gap-pipeline.md` §3 |
| Update `gap-status.csv` với new rows | Coordinator | Per `gap-architecture-v2.md` |
| Update `audits-index.csv` row cho audit này | Coordinator | Per `meta-csv-index-pattern.md` |
| Update `documents/04-quality/gaps/ROADMAP.md` §🎯 Current Status | Coordinator | Per §5 audit-to-gap-pipeline |
| Update `output-review-mandate.md` §3 row Security Baseline | Coordinator | Reflect new score + version |

---

## 10. References

- **Audit skill:** `.claude/skills/quality/security-audit/SKILL.md` v2 (per GAP-564)
- **Audit format template:** `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md` (this file)
- **Sister rules (Cat 1-5 per-check):**
  - `.claude/rules/pre-launch-dependency-hardening-checklist.md` v1.0.x
  - `.claude/rules/pre-launch-secrets-hardening-checklist.md` v1.0.x
  - `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` v1.0.x
  - `.claude/rules/pre-launch-auth-hardening-checklist.md` v1.0.x
  - `.claude/rules/pre-launch-infra-hardening-checklist.md` v1.0.x
- **Baseline audits:** `documents/04-quality/audits/security/<earlier-reports>.md`
- **Governance:**
  - `.claude/rules/post-wave-audit-mandate.md` §2.1 (audit trigger per file-pattern matrix)
  - `.claude/rules/audit-to-gap-pipeline.md` §3 (gap filing pipeline)
  - `.claude/rules/output-review-mandate.md` §3 (Security audit row)
  - GAP-564 (META v2 format mandate)

---

## Evidence directory structure

```
documents/04-quality/audits/security/evidence/YYYY-MM-DD/
├── EVIDENCE-YYYY-MM-DD-DEPS-001.txt
├── EVIDENCE-YYYY-MM-DD-DEPS-002.txt
├── EVIDENCE-YYYY-MM-DD-SEC-001.txt
├── EVIDENCE-YYYY-MM-DD-SEC-002.txt
├── EVIDENCE-YYYY-MM-DD-OWASP-A01-001.txt
├── EVIDENCE-YYYY-MM-DD-OWASP-A02-001.txt
├── ...
└── README.md  # index + audit cross-reference
```

Mỗi `EVIDENCE-*.txt` file chứa full Command run + raw Output. Audit report cites artifact ID only (compact); reader xem artifact file cho full evidence.

---

## Self-test (worked example — applied to Wave 78 Cat 2 incident)

Per GAP-564 §6 self-test: v2 format catches what v1 narrative missed.

See `documents/04-quality/gaps/GAP-564-security-audit-cat2-grep-evidence-enforcement.md` §Self-test section cho 1 retroactive worked example: Cat 2 hardcoded password grep evidence on Wave 78 → 11 hits surfaced (caught by GitHub Secret Scanning post-merge; v2 format would have caught at audit time).

---

## Version

- **v2.0** (2026-05-15) — per GAP-564 Wave 80 Bucket A. Per-control evidence block mandatory for all 5 categories. SOC2 Type II / ISO27001 / OWASP ASVS aligned.
