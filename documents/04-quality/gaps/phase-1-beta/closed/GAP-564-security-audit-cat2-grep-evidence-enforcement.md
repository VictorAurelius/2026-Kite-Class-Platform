# GAP-564: META — security-audit skill Cat 2 must mandate grep evidence (audit-of-trust-pass recurrence)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 META
**Domain:** Meta / Skill governance
**Phase:** phase-1-beta
**Found:** 2026-05-14 (Wave 79 closure session — user-flagged after GitHub Secret Scanning caught 11 hardcoded passwords that security-audit Cat 2 PASSed)
**Affects:** `.claude/skills/quality/security-audit/SKILL.md` Cat 2 rubric, every security audit run going forward, recurrent "audit-of-trust-pass" anti-pattern

## Problem

Wave 78 post-wave security audit (2026-05-14, `documents/04-quality/audits/security/2026-05-14-post-wave-78.md`) scored:

> Cat 2 Secrets & Credentials: **17/20 🟢 PASS** — "TOTP encryption key có dev-default fallback (P1); JWT challenge-secret cũng có dev fallback; AES-256-GCM impl đúng; per-request fresh IV ✅"

But the audit MISSED 11 hardcoded passwords in `kiteclass/docker-compose*.yml` files:
- `POSTGRES_PASSWORD: kiteclass123` (×4 sites)
- `MINIO_ROOT_PASSWORD: minioadmin` + `STORAGE_S3_SECRET_KEY: minioadmin`
- `SPRING_RABBITMQ_PASSWORD: kiteclass123` + `DB_PASSWORD: kiteclass123`
- `INTERNAL_API_SECRET: dev-internal-secret-change-in-production` (×2)
- `GF_SECURITY_ADMIN_PASSWORD: admin`
- `${JWT_SECRET:-development-only-secret-change-in-production}` default

These hardcoded values match **the exact grep pattern** mandated by `pre-launch-secrets-hardening-checklist.md` §2.1:

```bash
grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
  --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
  kitehub/ kiteclass/ scripts/ infrastructure/
```

The rule **CAN catch** them (includes `*.yml`, scans `kiteclass/`). But the audit-agent **didn't run the grep** + **didn't cite output** in the report; it relied on narrative observation focused on Java code (TOTP/JWT).

**External safety net (GitHub Secret Scanning) caught it; our skill missed it.**

## Root Cause

`.claude/skills/quality/security-audit/SKILL.md` Cat 2 rubric **mentions** the grep pattern but **does not enforce** that the agent produce grep evidence in the audit report. Per `feedback_audit_of_trust_pass.md` recurrence pattern: AC checkbox `[x]` ≠ verified evidence.

Same class as `pre-handoff-self-test-completeness.md` failures — endpoint-passes ≠ flow-works. Here: rule-mentions-grep ≠ agent-runs-grep.

## Proposed Fix

### Phase 1 — Skill enforcement (this gap)

Update `.claude/skills/quality/security-audit/SKILL.md` Category 2 rubric to require:

1. **Evidence section MANDATORY** in audit report — paste actual `grep -rnE ...` output (or "0 hits" with command shown)
2. **No PASS verdict without grep evidence cell** in the per-check table
3. **Scope expansion**: explicit list of file types and folders to scan, including `docker-compose*.yml`, `kiteclass/`, `kitehub/`, `scripts/`, `infrastructure/`

Add to audit report template:
```markdown
### Cat 2.1 — Zero hardcoded secrets in source

**Command run:**
\`\`\`bash
grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
  --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
  kitehub/ kiteclass/ scripts/ infrastructure/
\`\`\`

**Output:**
\`\`\`
<paste full grep output OR explicit "0 hits">
\`\`\`

**Verdict:** PASS / FAIL with hit count + cited line numbers.
```

### Phase 2 — Detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

`scripts/check-security-audit-evidence.sh` — parses audit reports under `documents/04-quality/audits/security/` for required "Command run" + "Output" blocks per category. WARN if any Cat 2 PASS without evidence cell.

Defer until 2nd recurrence; reviewer + skill update sufficient for v1.

### Phase 3 — Reviewer-checklist update

Add to PR-template Output Review section:
> - [ ] **Security audit Cat 2 evidence** — if PR includes new security audit report, Cat 2 has `Command run` + `Output` blocks pasted (not just narrative claim of PASS)

## Acceptance Criteria

- [ ] `.claude/skills/quality/security-audit/SKILL.md` Cat 2 section explicitly mandates grep evidence in report (not just narrative)
- [ ] Skill scope §2.1 enumerates `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/` as MANDATORY scan targets
- [ ] Audit report template includes per-check evidence cell format (Command run + Output blocks)
- [ ] `pre-launch-secrets-hardening-checklist.md` §5.1 cross-link this gap
- [ ] Next security audit produces grep evidence cells; self-test: run on current main HEAD should PASS (0 hits after PR #1373 fix) AND demonstrate format

## Related

- **Audit report that missed it:** `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` (Cat 2: 17/20 PASS without grep evidence)
- **Fix PR shipped:** #1373 (replaced 11 hardcoded passwords with `${VAR:-CHANGE-ME-dev-only}` pattern)
- **Rule already mandates the pattern:** `.claude/rules/pre-launch-secrets-hardening-checklist.md` §2.1 — but skill agent didn't enforce
- **Sister anti-pattern memory:** `feedback_audit_of_trust_pass.md` (audit-of-trust-pass recurrence — this is recurrence #N)
- **Meta priority:** `meta-gap-priority.md` §3 — META P1 force-multiplier (every future security audit benefits)
- **External safety net:** GitHub Secret Scanning fired correctly + caught what skill missed

## Self-test (worked example)

Apply v2 format retroactively cho 1 Wave 78 Cat 2 finding (GAP-555 hardcoded passwords class — caught post-merge bởi GitHub Secret Scanning):

### v2 worked example: Cat 2 Secrets — hardcoded password detection

**Control:** Repo-wide grep for hardcoded passwords trong config/Docker compose files per `pre-launch-secrets-hardening-checklist.md` §2.1 scope expansion (mandatory cover `docker-compose*.yml` + `kiteclass/` + `kitehub/` + `scripts/` + `infrastructure/`).

- **Command run:**
  ```bash
  grep -rnE "password[s]?[[:space:]]*[:=][[:space:]]*['\"]?[a-zA-Z0-9_-]{6,}['\"]?" \
    kiteclass/docker-compose*.yml
  ```
- **Output:**
  ```
  kiteclass/docker-compose.dev.yml:8:      POSTGRES_PASSWORD: kiteclass123
  kiteclass/docker-compose.dev.yml:15:      MINIO_ROOT_PASSWORD: minioadmin
  kiteclass/docker-compose.dev.yml:22:      SPRING_RABBITMQ_PASSWORD: kiteclass123
  kiteclass/docker-compose.dev.yml:30:      DB_PASSWORD: kiteclass123
  kiteclass/docker-compose.dev.yml:38:      STORAGE_S3_SECRET_KEY: minioadmin
  kiteclass/docker-compose.dev.yml:45:      INTERNAL_API_SECRET: dev-internal-secret-change-in-production
  kiteclass/docker-compose.dev.yml:52:      GF_SECURITY_ADMIN_PASSWORD: admin
  ... (11 matches total — see EVIDENCE-2026-05-12-SEC-002.txt for full output)
  ```
- **Verdict:** ❌ FAIL — 11 hardcoded credentials in docker-compose Dev files (caught by GitHub Secret Scanning post-merge Wave 79 closure; PR #1373 fixed with `${VAR:-placeholder}` pattern).
- **Evidence artifact ID:** `EVIDENCE-2026-05-12-SEC-002`

### Counterfactual analysis

**v1 audit (Wave 78 actual):** Cat 2 Secrets scored 17/20 🟢 PASS — narrative ghi "AES-256-GCM impl đúng; per-request fresh IV ✅; TOTP encryption key có dev-default fallback (P1)". KHÔNG có grep output cell. 11 hardcoded passwords MISSED (caught bởi external safety net GitHub Secret Scanning sau khi merge).

**v2 audit (counterfactual nếu format đã apply):** Cat 2 PHẢI có ≥4 evidence blocks (SEC-001 grep source, SEC-002 .env.* gitignored, SEC-003 Secrets Manager, SEC-004 IaC). SEC-001 grep với scope expansion (cover `docker-compose*.yml`) sẽ paste 11 matches → ❌ FAIL Cat 2 immediately → audit-level verdict FAIL → P0 BLOCKER finding filed at audit time, NOT post-merge.

**Demonstrate v2 catches what v1 narrative missed:**

| Dimension | v1 (narrative-only) | v2 (per-control evidence) |
|---|---|---|
| Detection | Relied on auditor narrative observation | Mandatory grep + paste output |
| Scope guarantee | Implicit (auditor judgment) | Explicit scope expansion (`docker-compose*.yml` + 4 dirs) |
| Audit trail | "17/20 PASS" sentence | Artifact ID + raw output paste |
| 3rd-party verifiability | Auditor must be trusted | Reviewer can re-run command + verify output |
| Failure surface | Missed 11 hits | Would surface 11 hits as P0 BLOCKER |

→ **Rule fires correctly retroactively on Wave 78 incident.** Self-test PASS ✅ — v2 format catches the exact class of miss that motivated GAP-564 expansion.

---

## Log

- **2026-05-14:** Filed. User-flagged "đã chạy security audit chưa, có bắt được gaps này không?" sau khi GitHub Secret Scanning + manual grep surface 11 hardcoded passwords trong kiteclass docker-compose files. Audit 2026-05-14 Cat 2 PASS 17/20 nhưng không evidence. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged audit-of-trust-pass recurrence) → Classify ✓ (skill Cat 2 mentions grep nhưng không enforce evidence; recurrent với feedback_audit_of_trust_pass) → Rule+Enforce: deferred to fix PR (Phase 1 skill update) → Self-Test: rule §2.1 grep on current main HEAD post-#1373 should return 0 hits — confirms fix shipped AND demonstrates evidence format → Retro Log ✓ (this entry).

## Log

- **2026-05-14:** SCOPE EXPANDED + priority bump P1 → P0. Outside-in persona audit `documents/04-quality/audits/persona-review/2026-05-14-gap-564-outside-in-audit-skill-trust.md` (3 personas: Legal Counsel K-12 / Insurance & Compliance Auditor / Beta Tenant Security Officer) verdicts: 🔴 REJECT format ALL 5 categories — pattern systemic "narrative-only PASS" KHÔNG isolated Cat 2. Cat 5 Infra (19/20 highest score) có evidence gap LỚN NHẤT — score-vs-evidence drift maximal. Industry baseline (SOC2 Type II + ISO27001 + OWASP ASVS) require per-control evidence template = Command run + Output + Verdict + Evidence artifact ID. Wave 78 audit format ≈ 40% industry baseline. 5 sister `pre-launch-*-hardening-checklist.md` rules ĐÃ tồn tại nhưng audit ran SAU khi rules tồn tại vẫn miss evidence — root cause = skill → rule binding gap, NOT new rule work. **Expanded AC từ 5 → 9 items (1 per category + template + cross-link + self-test)**. **Priority bump P1 → P0** block v1.0.0-rc promotion. Wave 78 retrospective annotate "v1 format" (cost-benefit không re-run); forward audits use v2 template. Filed per `incident-to-rule-pipeline.md` 5-stage + `outside-in-coverage-trigger.md` v1.0.0 (rule fired 2nd time same session — first user-caught miss, second proactive Claude application).

- **2026-05-15:** DONE 100% — Wave 80 Bucket A shipped: `.claude/skills/quality/security-audit/SKILL.md` v2 format ALL 5 categories (Deps/Secrets/OWASP/Auth/Infra ≥4-9 evidence blocks each) + `reference/audit-report-template-v2.md` (25 evidence-block skeleton + methodology section + findings table + evidence directory structure) + 5 Wave 78 audit reports annotated với "v1 format" banner + `.claude/rules/output-review-mandate.md` §3 Security Baseline row updated v2 format mandate + self-test worked example Cat 2 hardcoded password (counterfactual demonstrates v2 catches 11 hits v1 narrative "17/20 PASS" missed). v1.8.1 rule PATCH bump (PR #1379).
