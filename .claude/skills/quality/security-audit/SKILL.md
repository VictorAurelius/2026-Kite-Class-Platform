---
name: security-audit
description: "Dùng khi user nói 'security audit', 'pentest', 'kiểm tra bảo mật', 'dependency scan', hoặc trước production deploy. Deep security check /100 — vượt xa quality-audit §2."
user-invocable: true
---

# /security-audit — Deep Security Assessment

Score /100. Goes deeper than quality-audit's 10-point security category. Covers dependencies, secrets, OWASP, auth, and infra.

## Process

### 1. Run Automated Scans

```bash
# Dependency vulnerabilities
cd kitehub/kitehub-frontend && npm audit --json 2>/dev/null | head -50
cd kiteclass/kiteclass-frontend && npm audit --json 2>/dev/null | head -50

# Secret patterns (broad scope — grep OK for scanning, not executing)
grep -rn "password\s*=\|secret\s*=\|api_key\|Bearer " --include="*.java" --include="*.ts" --include="*.yml" \
  | grep -v node_modules | grep -v test | grep -v target | grep -v ".example" | head -30

# Hardcoded IPs/URLs (all module src dirs)
grep -rn "localhost\|127\.0\.0\.1\|0\.0\.0\.0" --include="*.java" --include="*.yml" \
  kiteclass/*/src/main/ kitehub/*/src/main/ | head -20
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface bugs the dev team cannot trust other layers to catch. A high score with hidden P0 bugs is WORSE than a low score that lists every finding honestly.** Per Wave 71b incident (87/100 score missed 5 P0 OWASP A07 gaps), the previous rubric averaged sub-checks within a 20-pt category, which let major gaps hide behind passing sub-checks. New rubric: per-check pass/fail; any P0/P1 fail in any check = audit FAIL regardless of total.

Rules for every audit run:
1. Enumerate ALL §3 sub-checks. NEVER skip one because "obviously fine."
2. Each sub-check returns: PASS / FAIL / N/A-with-reason. No "partial credit."
3. Final output starts with a **bug list** (every FAIL surfaces) BEFORE the score.
4. Score is descriptive only; the bug list is the deliverable.
5. If audit time-budget runs out, leave remaining sub-checks marked `❓ UNCHECKED` — do NOT mark PASS by default. Coordinator decides whether to defer.

### 3. Score 5 Categories with per-check rubric

Per Wave 72a Bucket E (GAP-522 closure), every category binds to a per-check pass/fail rule. Within each 20-pt category: any P0/P1 sub-check FAIL caps category total ≤ 16/20 (pattern from `pre-launch-auth-hardening-checklist.md` §4 "1 fail = checklist fail"). No averaging hides P0 gaps.

| # | Category (20pts) | Per-check rubric file |
|---|-----------------|------------------------|
| 1 | **Dependency Vulnerabilities** | **`pre-launch-dependency-hardening-checklist.md` §2 (8 sub-checks, per-check pass/fail)** |
| 2 | **Secrets & Credentials** | **`pre-launch-secrets-hardening-checklist.md` §2 (8 sub-checks, per-check pass/fail)** |
| 3 | **OWASP Top 10 (A01-A06, A08-A10)** | **`pre-launch-owasp-rest-hardening-checklist.md` §2 (9 sub-checks, per-OWASP-item pass/fail)** |
| 4 | **Auth & Access Control (OWASP A07)** | **`pre-launch-auth-hardening-checklist.md` §2 (8 sub-checks, per-check pass/fail)** |
| 5 | **Infrastructure Security** | **`pre-launch-infra-hardening-checklist.md` §2 (9 sub-checks, per-check pass/fail)** |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED` (no partial credit).
3. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
4. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL regardless of total score.
5. Each FAIL surfaces in the audit-report bug list per §2 "Primacy: bug-finding > scoring" — bug list is the deliverable.

Cross-reference: each rule's §2 enumerates concrete checks; each rule's §4 has worked self-test demonstrating rubric fires on current main. Reading the bound rule IS the rubric — `reference/scoring-guide.md` legacy file retained for backward-compat narrative only.

Scoring details: `reference/scoring-guide.md` (legacy) — superseded by per-category rule files above per Wave 72a Bucket E.

### 3. Output

Save to `documents/04-quality/audits/security/security-audit-[date].md`

## Context Management

Token budget ~25-40K. Kiểm soát bằng:

1. **npm audit output** — `npm audit --json 2>/dev/null | head -50`. KHÔNG đọc full JSON (có thể 10K+ lines). Chỉ cần summary + critical/high count.
2. **Secret scan** — LUÔN `| grep -v node_modules | grep -v test | head -30`. Không scan toàn bộ repo.
3. **Staged execution** — Phase 1: automated scans (categories 1-2). Phase 2: manual code review (categories 3-5). Nếu context low sau phase 1, delegate phase 2 cho subagent.
4. **Config files** — Đọc CHỈ security-related sections của `application.yml`, không đọc toàn bộ file. Dùng `grep -A5 'security\|jwt\|cors\|csrf'`.

## Gotchas

- Wave 4 added SVG sanitizer, URL allowlist, CSRF provider — verify they're ACTIVE not just coded
- `application.yml` security keys: check both main AND test profiles
- Gateway CORS config is the real enforcement point — not individual service configs
- JSoup 1.18.1 was added for SVG sanitization — check for CVEs on that version
- Rate limiting config is in gateway `application.yml`, not core
- npm audit JSON output có thể rất lớn — LUÔN limit output
- **Multi-module scope** — narrow grep `kiteclass/ kitehub/` may miss submodule source files; prefer broad `--include="*.ext"` from root OR explicit `kiteclass/*/src/main/` glob. Ref: GAP-149.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
- `data/eval-fixtures/` — 3 synthetic scenarios for self-test (GAP-253)

## Eval Fixtures

3 synthetic fixtures live under `data/eval-fixtures/` to keep this skill
honest when its body is edited (per Anthropic 2026 eval-first guidance —
GAP-253 pilot). Each fixture has a `# Expected: PASS|FAIL` header.

- `good.md` — clean baseline; npm-audit empty, no hardcoded secrets, all
  endpoints `@PreAuthorize`-guarded; expected output `100/100 Grade A`.
- `bad-secret-in-config.md` — `application.yml` contains `sk-proj-…`
  literal + `password: admin123`; Cat 2 must report `-12+` and flag
  Severity 🛑 BLOCKER.
- `edge-transitive-cve.md` — npm-audit / mvn report a CVE already pinned by
  Spring Boot BOM or `pnpm.overrides`; Cat 1 must emit verify-vs-waive
  guidance instead of auto-failing.

**Run:** walk through the audit process steps mentally against the synthetic
content; each fixture's `Expected audit-report excerpt` section is the
regression contract. When extending this skill, re-walk all 3 fixtures and
confirm the expected outputs still hold.
