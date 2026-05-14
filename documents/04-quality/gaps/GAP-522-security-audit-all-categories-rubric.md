# GAP-522: Extend per-check pass/fail rubric to all 5 security-audit categories (META)

**Status:** 🟢 DONE 2026-05-14 (Wave 72a Bucket E)
**Priority:** 🔴 P0 META (force-multiplier — same failure mode that hid OWASP A07 also hides bugs in 4 other categories)
**Domain:** Meta / Skills
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2 audit per user-flagged "skill audit phải là lớp phòng vệ tin tưởng")
**Affects:** `.claude/skills/quality/security-audit/SKILL.md` Categories 1, 2, 3, 5 (Category 4 already fixed PR #1278)

## Problem

Wave 71b incident: security-audit Wave 40 milestone scored 87/100 nhưng miss 5 confirmed P0 OWASP A07 gaps because Category 4 rubric was vague ("rate limiting, session mgmt") and allowed averaging within 20-pt bucket. Wave 71c PR #1278 đã bind Category 4 vào `pre-launch-auth-hardening-checklist.md` 8 per-check sub-checks.

**Same failure mode applies to remaining 4 categories** — vague rubric + 20-pt averaging:

| Cat | Current rubric (vague) | Likely hidden bug class |
|---|---|---|
| 1 Dependency | "npm audit critical/high count, Maven dep versions" | Specific CVE class not surfaced (e.g., Snyk top-10 patterns; supply chain risk; transitive deps; lockfile drift) |
| 2 Secrets | "No hardcoded secrets, .env gitignored, rotation policy" | AWS Secret rotation cadence; KMS key rotation; SSH key vault scope; git history pickaxe gaps; env file inheritance |
| 3 OWASP A01-A06/A08-A10 | "XSS/SQLi/CSRF/SSRF guards per Wave 4" | A01 broken access (per-resource authz); A02 crypto failures (weak cipher, weak hash); A05 misconfig (default creds, debug endpoints); A06 vuln components (covered by Cat 1?); A08 software integrity (CI signing); A09 logging failures (missing audit log — overlap GAP-521); A10 SSRF specific |
| 5 Infra | "TLS config, CORS, CSP, Docker non-root, k8s security context" | mTLS internal; container image scanning; least-privilege IAM; secret-in-volume vs secret-in-env; CIS benchmark; pod security standard |

## Proposed Fix

Per `pre-launch-auth-hardening-checklist.md` v1.0.0 pattern, write 4 sister rules:
1. `pre-launch-dependency-hardening-checklist.md` v1.0.0 — N specific checks per Cat 1
2. `pre-launch-secrets-hardening-checklist.md` v1.0.0 — N specific checks per Cat 2 (overlap with `production-env-config-registry` already shipped Wave 71 + extend)
3. `pre-launch-owasp-noA07-checklist.md` v1.0.0 — per OWASP item A01-A06/A08-A10 specific checks
4. `pre-launch-infra-security-checklist.md` v1.0.0 — TLS/CSP/CORS/container/k8s per-check

Update `security-audit/SKILL.md` §3 table: each category binds to its rule (mirror Cat 4 → auth-hardening).

## Acceptance Criteria

- [x] 4 sister rules shipped với v1.0.0 + frontmatter + worked self-test on current main
- [x] `security-audit/SKILL.md` §3 — 5 categories all bind to per-check rule
- [x] Self-test: each rule §4 surfaces concrete current-main gaps (per-OWASP-item Cat 3 FAIL × 3, Cat 1/2/5 PARTIAL × multiple)
- [x] `rules-index.csv` 4 new rows

## Related

- Parent: PR #1278 (Cat 4 fix; this gap extends to other 4 categories)
- Sibling: GAP-523 (audit-skill review across 6 OTHER skills)
- Rule: `meta-gap-priority.md` §3 (Meta-P0 boost)

## Log

- **2026-05-14** (Wave 72a Bucket E): Shipped 4 sister rules — `pre-launch-dependency-hardening-checklist.md` (Cat 1, 8 checks per-pattern), `pre-launch-secrets-hardening-checklist.md` (Cat 2, 8 checks per-mechanism), `pre-launch-owasp-rest-hardening-checklist.md` (Cat 3, 9 checks per OWASP item A01-A06/A08-A10), `pre-launch-infra-hardening-checklist.md` (Cat 5, 9 checks per-mechanism). Updated `security-audit/SKILL.md` §3 — all 5 categories bind to per-check rule + "Per-check scoring" subsection added (P0 fail caps category ≤ 16/20 + audit FAIL). Updated `rules-index.csv` (4 new rows alphabetical). Validators pass: `check-rule-frontmatter.sh` 45/45, `check-rules-index-csv.sh` 45/45, `check-skill-conventions.sh` 52 PASS 0 FAIL. Detector wiring deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days. Per `gap-done-discipline.md` §2: all AC checked, no banned phrases in this Log entry, verification artifact pointers = §4 worked self-tests in each new rule file.
