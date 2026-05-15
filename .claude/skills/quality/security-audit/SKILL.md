---
name: security-audit
description: "Dùng khi user nói 'security audit', 'pentest', 'kiểm tra bảo mật', 'dependency scan', hoặc trước production deploy. Deep security check /100 — vượt xa quality-audit §2. v2 audit format mandatory per GAP-564: mỗi control bind với evidence block (Command run + Output + Verdict + Evidence artifact ID)."
user-invocable: true
---

# /security-audit — Deep Security Assessment (v2 format)

Score /100. Goes deeper than quality-audit's 10-point security category. Covers dependencies, secrets, OWASP, auth, and infra.

**v2 audit format (GAP-564 — paired same-PR Wave 80 Bucket A):** mỗi sub-check PHẢI ship per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) per SOC2 Type II / ISO27001 Annex A / OWASP ASVS baseline. Narrative-only PASS BANNED. See `reference/audit-report-template-v2.md` cho skeleton.

## Process

### 1. Run Automated Scans

```bash
# Dependency vulnerabilities
cd kitehub/kitehub-frontend && pnpm audit --json --audit-level=high 2>/dev/null | head -50
cd kiteclass/kiteclass-frontend && pnpm audit --json --audit-level=high 2>/dev/null | head -50

# Secret patterns (per pre-launch-secrets-hardening-checklist.md §2.1)
grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
  --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
  kitehub/ kiteclass/ scripts/ infrastructure/ \
  | grep -vE "(test|fixture|example|template|\.md:|noreply@|localhost|change-me|placeholder)" \
  | head -50

# Hardcoded IPs/URLs (all module src dirs)
grep -rn "localhost\|127\.0\.0\.1\|0\.0\.0\.0" --include="*.java" --include="*.yml" \
  kiteclass/*/src/main/ kitehub/*/src/main/ | head -20
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface bugs the dev team cannot trust other layers to catch. A high score with hidden P0 bugs is WORSE than a low score that lists every finding honestly.** Per Wave 71b incident (87/100 score missed 5 P0 OWASP A07 gaps) + Wave 78 incident (89/100 missed 11 hardcoded passwords Cat 2 caught by GitHub Secret Scanning post-merge), the previous rubric averaged sub-checks within a 20-pt category, which let major gaps hide behind passing sub-checks. v2 rubric: per-check pass/fail WITH per-control evidence block; any P0/P1 fail in any check = audit FAIL regardless of total.

Rules for every audit run:
1. Enumerate ALL §3 sub-checks. NEVER skip one because "obviously fine."
2. Each sub-check returns: PASS / FAIL / N/A-with-reason / ❓ UNCHECKED. No "partial credit."
3. **Per-control evidence block MANDATORY (v2)** — Command run + Output + Verdict + Evidence artifact ID. See per-category sections below + `reference/audit-report-template-v2.md`.
4. Final output starts with a **bug list** (every FAIL surfaces) BEFORE the score.
5. Score is descriptive only; the bug list is the deliverable.
6. If audit time-budget runs out, leave remaining sub-checks marked `❓ UNCHECKED` — do NOT mark PASS by default. Coordinator decides whether to defer.

### 3. Score 5 Categories with per-check rubric + v2 evidence blocks

Per Wave 72a Bucket E (GAP-522) + Wave 80 Bucket A (GAP-564), every category binds to (a) a per-check pass/fail rule from sister `pre-launch-*-hardening-checklist.md` AND (b) v2 evidence-block format. Within each 20-pt category: any P0/P1 sub-check FAIL caps category total ≤ 16/20.

| # | Category (20pts) | Sister rule (per-check rubric) | Min controls (evidence blocks) |
|---|-----------------|--------------------------------|-------------------------------|
| 1 | **Dependency Vulnerabilities** | `pre-launch-dependency-hardening-checklist.md` §2 (8 sub-checks) | ≥4 (FE audit, BE audit, lockfile, Dependabot/Trivy) |
| 2 | **Secrets & Credentials** | `pre-launch-secrets-hardening-checklist.md` §2 (8 sub-checks) | ≥4 (grep source, gitignore, Secrets Manager, IaC scan) |
| 3 | **OWASP Top 10 (A01-A06, A08-A10)** | `pre-launch-owasp-rest-hardening-checklist.md` §2 (9 sub-checks) | ≥9 (one per OWASP item ex-A07) |
| 4 | **Auth & Access Control (OWASP A07)** | `pre-launch-auth-hardening-checklist.md` §2 (8 sub-checks) | ≥4 (gateway rate-limit matrix, lockout test, 2FA enrollment, password validator) |
| 5 | **Infrastructure Security** | `pre-launch-infra-hardening-checklist.md` §2 (9 sub-checks) | ≥5 (TLS listener, CORS, Docker non-root, IAM least-priv, CloudTrail) |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED` (no partial credit).
3. **Produce evidence block for each check (v2 mandatory)** — see per-category template below.
4. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
5. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL regardless of total score.
6. Each FAIL surfaces in the audit-report bug list per §2 "Primacy: bug-finding > scoring" — bug list is the deliverable.

Cross-reference: each sister rule's §2 enumerates concrete checks + grep/AWS-CLI commands. Reading the bound rule IS the rubric.

---

### Category 1 — Dependency Vulnerabilities (per-control evidence v2)

**Per-control evidence (v2 format mandatory per GAP-564):**

For each Cat 1 check (per `pre-launch-dependency-hardening-checklist.md` §2.1-2.8), produce this block:

```
**Control:** <e.g., "Frontend pnpm audit clean across both FE apps">

- Command run: `cd kitehub/kitehub-frontend && pnpm audit --json --audit-level=high`
- Output (raw, ≤20 lines snippet OR full output as artifact):
  ```
  <stdout/stderr verbatim — e.g., "found 0 vulnerabilities" OR JSON snippet với severity counts>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — với rationale (e.g., "0 high/critical; 2 moderate accepted per pnpm.overrides documented in dependency-waivers.md")
- Evidence artifact ID: `EVIDENCE-{audit-date}-DEPS-{NNN}` (vd `EVIDENCE-2026-05-15-DEPS-001`); store under `documents/04-quality/audits/security/evidence/{audit-date}/{artifact-id}.txt` nếu output >20 lines
```

Min 4 controls Cat 1 (e.g., DEPS-001 pnpm audit FE, DEPS-002 mvn dependency-check BE, DEPS-003 Trivy image scan, DEPS-004 SBOM artifact).

---

### Category 2 — Secrets & Credentials (per-control evidence v2)

**Per-control evidence (v2 format mandatory per GAP-564):**

For each Cat 2 check (per `pre-launch-secrets-hardening-checklist.md` §2.1-2.8), produce this block:

```
**Control:** <e.g., "Zero hardcoded secrets in source per §2.1 grep mandate">

- Command run: `grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" kitehub/ kiteclass/ scripts/ infrastructure/`
- Output:
  ```
  <full grep output OR explicit "0 hits"; if hits remain, classify each as placeholder/example or REAL leak>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — với hit count + cited line numbers + risk tier
- Evidence artifact ID: `EVIDENCE-{audit-date}-SEC-{NNN}` (vd `EVIDENCE-2026-05-15-SEC-001`)
```

Min 4 controls Cat 2 (e.g., SEC-001 grep source, SEC-002 .env.* gitignored, SEC-003 Secrets Manager describe + KMS, SEC-004 terraform IaC scan).

**Mandatory scope expansion per GAP-564 §6 self-test:** grep MUST cover `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/` (closes Wave 78 11-hardcoded-password miss).

---

### Category 3 — OWASP A01-A06/A08-A10 (per-control evidence v2)

**Per-control evidence (v2 format mandatory per GAP-564):**

For each OWASP item (per `pre-launch-owasp-rest-hardening-checklist.md` §2.1-2.9), produce this block — one block PER OWASP ITEM:

```
**Control:** A0X <item name>

- Command run: `<exact command — vd grep @PreAuthorize coverage OR config snapshot>`
- Output:
  ```
  <command output OR file snippet>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL
- Evidence artifact ID: `EVIDENCE-{audit-date}-OWASP-A0X-{NNN}` (vd `EVIDENCE-2026-05-15-OWASP-A01-001`)
- Cross-reference: `pre-launch-owasp-rest-hardening-checklist.md` §2.X
```

Min 9 controls (one per OWASP item A01-A06, A08-A10 — A07 covered Cat 4):

| OWASP | Command pattern | Expected evidence |
|---|---|---|
| A01 Access Control | `grep -rn "@PreAuthorize" kitehub/*/src/main/java/**/*AdminController.java` | Per-endpoint coverage matrix |
| A02 Crypto | `grep -rnE "MessageDigest\.getInstance\(\"(MD5\|SHA-1)\"\)" kitehub/ kiteclass/` | 0 hits expected |
| A03 Injection | `grep -rnE "(SELECT\|UPDATE\|DELETE).*\+\s*\w+\s*\+\|String\.format.*WHERE.*%" --include="*.java"` | 0 hits in non-test |
| A04 Insecure Design | `ls documents/02-architecture/threat-models/*.md` | Threat models exist cho critical flows |
| A05 Misconfig | `cat kitehub/*/src/main/resources/application-production.yml \| grep -A2 "management.endpoints.web.exposure"` | Actuator scoped, no `'*'` |
| A06 Vuln Components | Cross-ref Cat 1 SBOM artifact | Delegated |
| A08 Supply Chain | `grep -rn "image:" Dockerfile* docker-compose*.yml` | SHA-pinned OR documented |
| A09 Logging | `grep -rn "AdminAuditLog\|admin_audit_log" kitehub/` | Audit log entity exists |
| A10 SSRF | `grep -rnE "(RestTemplate\|WebClient\|HttpClient).*\.(get\|post\|exchange)" --include="*.java" \| grep -iE "user\|input\|url"` | Each hit has allowlist |

---

### Category 4 — Auth & Access Control / OWASP A07 (per-control evidence v2)

**Per-control evidence (v2 format mandatory per GAP-564):**

For each Cat 4 check (per `pre-launch-auth-hardening-checklist.md` §2.1-2.8), produce this block:

```
**Control:** <e.g., "Auth endpoints rate-limited at gateway per §2.1 7-row matrix">

- Command run: `grep -A5 'id: auth\|id: kitehub-auth' kitehub/kitehub-gateway/src/main/resources/application.yml`
- Output:
  ```
  <YAML snippet showing route definitions với RequestRateLimiter — verify per-row matrix matches §2.1 minimum>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — với endpoint-by-endpoint coverage matrix
- Evidence artifact ID: `EVIDENCE-{audit-date}-AUTH-{NNN}`
```

Min 4 controls Cat 4 (e.g., AUTH-001 gateway rate-limit matrix, AUTH-002 AuthServiceLockoutTest output, AUTH-003 TwoFactorAuthService enrollment endpoints listed, AUTH-004 PasswordValidator + admin_audit_log entity verify).

---

### Category 5 — Infrastructure Security (per-control evidence v2)

**Per-control evidence (v2 format mandatory per GAP-564):**

For each Cat 5 check (per `pre-launch-infra-hardening-checklist.md` §2.1-2.9), produce this block:

```
**Control:** <e.g., "ALB TLS 1.2+ only on listeners per §2.1">

- Command run: `aws elbv2 describe-listeners --load-balancer-arn <arn> --query 'Listeners[?Port==`443`].[Port,SslPolicy]' --output table`
- Output:
  ```
  <AWS CLI output verbatim showing SslPolicy value>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — với policy name verification (expect TLS13-1-2-2021-06 or stricter)
- Evidence artifact ID: `EVIDENCE-{audit-date}-INFRA-{NNN}`
```

Min 5 controls Cat 5 (e.g., INFRA-001 ALB TLS, INFRA-002 CORS origins explicit, INFRA-003 Docker non-root USER sweep, INFRA-004 IAM least-priv grep, INFRA-005 CloudTrail multi-region status).

---

### 4. Output

Save to `documents/04-quality/audits/security/security-audit-[date].md` per `reference/audit-report-template-v2.md` skeleton.

**v2 mandatory sections:**
1. Header (date, wave, scope, auditor)
2. Methodology (tools, scope coverage, sampling strategy)
3. Bug list (every FAIL — deliverable)
4. Score per category with per-check evidence blocks (≥25 blocks total = ~4 Cat1 + ~4 Cat2 + ~9 Cat3 + ~4 Cat4 + ~5 Cat5)
5. Findings table (linking back to evidence artifact IDs)
6. Verdict + score (track v1 score / 100 + v2 evidence completeness % parallel — both required v2)
7. Recommendations + References

## Context Management

Token budget ~25-40K. Kiểm soát bằng:

1. **pnpm audit output** — `pnpm audit --json --audit-level=high | head -50`. KHÔNG đọc full JSON. Chỉ cần summary + critical/high count.
2. **Secret scan** — LUÔN `| grep -v node_modules | grep -v test | head -30`. Không scan toàn bộ repo. KHI scope expansion needed (per Cat 2 §2.1) MUST include `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/`.
3. **Staged execution** — Phase 1: automated scans (Cat 1-2). Phase 2: manual code review (Cat 3-5). Nếu context low sau phase 1, delegate phase 2 cho subagent.
4. **Config files** — Đọc CHỈ security-related sections của `application.yml`, không đọc toàn bộ file. Dùng `grep -A5 'security\|jwt\|cors\|csrf'`.
5. **Evidence artifacts** — large outputs (>20 lines) saved as separate file under `documents/04-quality/audits/security/evidence/{audit-date}/{artifact-id}.txt`; report references artifact ID only.

## Gotchas

- Wave 4 added SVG sanitizer, URL allowlist, CSRF provider — verify they're ACTIVE not just coded
- `application.yml` security keys: check both main AND test profiles
- Gateway CORS config is the real enforcement point — not individual service configs
- JSoup 1.18.1 was added for SVG sanitization — check for CVEs on that version
- Rate limiting config is in gateway `application.yml`, not core
- pnpm audit JSON output có thể rất lớn — LUÔN limit output
- **Multi-module scope** — narrow grep `kiteclass/ kitehub/` may miss submodule source files; prefer broad `--include="*.ext"` from root OR explicit `kiteclass/*/src/main/` glob. Ref: GAP-149.
- **GAP-564 lesson** — Wave 78 audit narrative-only PASS Cat 2 17/20 MISSED 11 hardcoded passwords in `docker-compose*.yml`; v2 evidence block (Command run + Output paste) catches what narrative misses. Mandatory grep scope: `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/`.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category (v1 legacy — v2 supersedes for evidence format)
- `reference/audit-report-template-v2.md` — v2 audit report skeleton with ≥15 evidence-block placeholders (GAP-564)
- `data/eval-fixtures/` — 3 synthetic scenarios for self-test (GAP-253)

## Eval Fixtures

3 synthetic fixtures live under `data/eval-fixtures/` to keep this skill
honest when its body is edited (per Anthropic 2026 eval-first guidance —
GAP-253 pilot). Each fixture has a `# Expected: PASS|FAIL` header.

- `good.md` — clean baseline; pnpm-audit empty, no hardcoded secrets, all
  endpoints `@PreAuthorize`-guarded; expected output `100/100 Grade A`.
- `bad-secret-in-config.md` — `application.yml` contains `sk-proj-…`
  literal + `password: admin123`; Cat 2 must report `-12+` and flag
  Severity 🛑 BLOCKER với evidence block grep output paste.
- `edge-transitive-cve.md` — pnpm-audit / mvn report a CVE already pinned by
  Spring Boot BOM or `pnpm.overrides`; Cat 1 must emit verify-vs-waive
  guidance instead of auto-failing.

**Run:** walk through the audit process steps mentally against the synthetic
content; each fixture's `Expected audit-report excerpt` section is the
regression contract. When extending this skill, re-walk all 3 fixtures and
confirm the expected outputs still hold + v2 evidence blocks produced for each check.

## Log

- **2026-05-15 (v2.0.0):** v2 audit format mandate per GAP-564 (Wave 80 Bucket A). Per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) now MANDATORY for all 5 categories per SOC2 Type II / ISO27001 Annex A / OWASP ASVS baseline. Added `reference/audit-report-template-v2.md` skeleton. Wave 78 5 audit reports retroactively annotated as "v1 format" (cost-benefit không re-run). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — expand from Cat 2-only fix to all 5 categories per outside-in audit Wave 79 closure).
