---
paths:
  - "documents/04-quality/audits/quality/**"
---

# Audit Skill Rubric — quality-audit (11 categories, per-check pass/fail)

**Priority:** 🟠 MANDATORY — audit primacy + per-check rubric for `quality-audit` skill
**Version:** 1.0.1
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (11-category per-check rubric + bug-finding-primacy section + extends `quality-audit/SKILL.md` Cat 1-11 + worked self-test on current main surfaces ≥1 finding) per §6.5 Enforcement Parity Mandate; no constraint loosening — generalizes Wave 71c security-audit pattern to comprehensive 11-category rubric closing GAP-523)
**Applies to:** Every invocation of `.claude/skills/quality-audit/SKILL.md` (the /110 top-of-funnel quality audit covering all 11 categories: E2E, Security, BE Tests, FE Tests, CI/CD, UI/UX, DevOps, Docs, Code Quality, PM, Persona Coverage)

---

## 1. The Rule

> **`quality-audit` skill must score every category by per-check pass/fail (no averaging within a 10-pt or /10 category that hides P0 sub-check failures). Any P0/P1 sub-check FAIL caps category total ≤ (max - 4) AND audit-level verdict = FAIL regardless of total score. The bug list (every FAIL) is the deliverable; the score is descriptive only.**

This rule mirrors the Wave 71c security-audit pattern (`pre-launch-auth-hardening-checklist.md` §1 "1 fail = checklist fail" + security-audit/SKILL.md §2 "bug-finding > scoring"). Wave 53 baseline `85/110` averaged sub-checks within /10 categories, letting P0 gaps hide (e.g., Cat 11 Persona Coverage scored `5/10` data-pending while Tier 1 personas not reviewed — that's a P1 hide). Per-check pass/fail eliminates averaging masquerade.

---

## 2. Mandatory per-check enumeration (5+ checks per category)

Every Category N's score derives from explicit per-check rolldown. Below are the canonical sub-checks. Skill body §"Bước 2" `quality-audit/SKILL.md` already enumerates most via its `| Tiêu chí | Điểm | Check |` rows — those tables ARE the per-check rubric. This rule binds them to per-check pass/fail semantics + audits sample checks for concreteness.

### 2.1 Category 1 — E2E Functionality (P0/P1 split)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 1.1 | `kiteclass/scripts/test-api-e2e.sh` returns 100% pass | P0 | Exit 0; no failures |
| 1.2 | E2E pass on FIRST run (no cold-start flake) | P1 | First run = green; not "green on retry" |
| 1.3 | Critical flows manual-walked: Register→Login→Dashboard→Create-Instance | P0 | Each step result captured |
| 1.4 | AI features hit real provider (not mock) | P1 | `ai.provider` config ≠ `mock`; verify via curl |
| 1.5 | E2E covers ≥1 negative path (invalid input → 400) | P1 | grep `assertStatus(400` or equiv |

### 2.2 Category 2 — Security (P0 for hardcoded secrets, P1 for rest)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 2.1 | Auth implemented: JWT + email verify + (captcha OR rate-limit) | P0 | `JwtAuthenticationFilter` + `EmailVerificationService` present |
| 2.2 | Rate limiting active on auth + sensitive endpoints | P0 | Gateway YAML `RequestRateLimiter` present (per `pre-launch-auth-hardening-checklist.md` §2.1) |
| 2.3 | Zero hardcoded secrets in source code | P0 | `grep -rE 'password\s*=\s*"[^${]\|api[_-]?key\s*=\s*"[^${]' --include='*.java' --include='*.yml'` returns 0 |
| 2.4 | CORS configured (not `*` wildcard in prod) | P1 | Gateway YAML `cors.allowed-origins` is specific list |
| 2.5 | Input validation present on all DTOs | P1 | `grep -l '@Valid' --include='*.java'` per controller |

### 2.3 Category 3 — Backend Tests (P0 build, P1 coverage)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 3.1 | All Maven modules `mvn test` pass (0 errors, 0 failures) | P0 | Exit 0 each module |
| 3.2 | 0 `@Disabled` / `@Ignore` annotations in production tests | P1 | grep returns 0 |
| 3.3 | Jacoco coverage ≥70% on changed modules | P1 | jacoco report `LINE` coverage ≥70 |
| 3.4 | Integration tests exist for every critical service | P1 | `*IT.java` count ≥1 per service module |
| 3.5 | Test fixtures isolated (Testcontainers OR @DirtiesContext) | P2 | grep `@Testcontainers\|@DirtiesContext` |

### 2.4 Category 4 — Frontend Tests (P0 build, P1 coverage)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 4.1 | KiteClass FE `pnpm test --run` pass with <10% skipped | P0 | Exit 0; skipped count <10% |
| 4.2 | KiteHub FE `pnpm build` pass (no broken pages) | P0 | Exit 0 |
| 4.3 | Component tests for ≥80% of critical pages | P1 | grep `.test.tsx\|.spec.tsx` count vs page count |
| 4.4 | E2E browser tests (Playwright) present | P1 | `e2e/` folder exists with ≥1 spec |
| 4.5 | No `pnpm-lock.yaml` drift between local + CI | P1 | `git diff --quiet pnpm-lock.yaml` |

### 2.5 Category 5 — CI/CD (P0 green, P1 hygiene)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 5.1 | All required CI workflows green on `main` HEAD | P0 | `gh run list --branch main --limit 10` all `success` |
| 5.2 | Zero stale feature branches >30 days | P1 | `git branch -r --merged main` filtered |
| 5.3 | Zero open PRs stale >14 days | P1 | `gh pr list --state open --json updatedAt` |
| 5.4 | CI history under 100 runs (per `CLAUDE.md` retention cap) | P1 | `gh run list --limit 200 \| wc -l` |
| 5.5 | Required status checks defined on `main` branch protection | P0 | `gh api repos/.../branches/main/protection` |

### 2.6 Category 6 — UI/UX (P0 design system, P1 a11y)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 6.1 | All pages use design-system tokens (no inline hex colors in `.tsx`) | P1 | `grep -E '#[0-9a-fA-F]{6}' src/app --include='*.tsx'` returns ≤5 |
| 6.2 | Theme system: `?primary=FF0000` URL changes visible color | P1 | Manual or screenshot test |
| 6.3 | Responsive breakpoints: 320/768/1024/1440 all render | P1 | `ui-review` screenshots at 4 widths |
| 6.4 | Onboarding/empty states present for top-3 user flows | P1 | grep `<EmptyState\|wizard\|tooltip` |
| 6.5 | a11y basics: every interactive element has aria-label OR semantic tag | P0 | axe-core scan: 0 critical violations |

### 2.7 Category 7 — DevOps/Infrastructure (P0 containers, P1 docs)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 7.1 | All Docker containers healthy via `docker compose ps` | P0 | All services `running (healthy)` |
| 7.2 | Production Terraform plan documented + reviewed | P0 | `infrastructure/terraform-aws/` has plan output committed for last apply |
| 7.3 | Backup strategy documented per `release-deploy-standard.md` §3.1 | P0 | `documents/05-guides/operations/db-backup-*.md` exists |
| 7.4 | Monitoring + alerting active (Prometheus + Grafana scraping) | P0 | `/actuator/prometheus` returns 200 + dashboard exists |
| 7.5 | Secrets management runbook present | P1 | `documents/05-guides/operations/secrets-rotation-runbook.md` exists |

### 2.8 Category 8 — Documentation (P0 business docs, P1 architecture)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 8.1 | Business docs exist for ALL implemented domains | P0 | every `kiteclass-core/module/*` + `kitehub-*/module/*` has `documents/01-business/.../{rules,use-cases,api-contract}.md` |
| 8.2 | Business docs code-sync: config keys in rules.md match `application.yml` | P0 | `verify-business-docs.sh` exit 0 |
| 8.3 | README + CLAUDE.md last-updated ≤30 days | P1 | grep `Last Updated` |
| 8.4 | Architecture ADRs current (last ≤60 days) | P1 | `adrs-index.csv` newest `created` ≤60 days |
| 8.5 | Plans + roadmap reflect current wave state | P1 | ROADMAP.md §Status Snapshot last commit ≤7 days |

### 2.9 Category 9 — Code Quality (P0 anti-pattern, P1 polish)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 9.1 | Zero TODO/FIXME/HACK in `src/main/` Java + `src/app/` TS | P1 | `grep -rE 'TODO\|FIXME\|HACK' --include='*.java' --include='*.tsx'` returns 0 |
| 9.2 | Zero IDE warnings (Java compile + TS strict) | P1 | `pnpm tsc --noEmit` exit 0; `mvn compile -Werror` exit 0 |
| 9.3 | Lint config enforced (ESLint + Checkstyle) via pre-commit | P1 | `.husky/pre-commit` runs both |
| 9.4 | No God Services (>500 lines OR >15 public methods) per `design-patterns.md` §3.1 | P0 | `find -size +20k *Service.java` returns 0 |
| 9.5 | Outbox pattern applied (no direct `rabbitTemplate.convertAndSend` outside dispatcher) per `design-patterns.md` §3.5 | P0 | grep returns 0 sites without `outbox.*reliability net` marker comment |

### 2.10 Category 10 — Project Management (P1 governance)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 10.1 | All wave plans have status: complete OR active (no orphan plans) | P1 | grep `status:` in `documents/03-planning/waves/` |
| 10.2 | PRs follow Superpowers methodology (PR body has brainstorm/breakdown/TDD/review sections) | P1 | last 10 PRs sampled |
| 10.3 | Commit messages follow conventional commits | P2 | `git log --oneline -20` pattern match |
| 10.4 | Gap status CSV in sync with markdown frontmatter per `gap-architecture-v2.md` | P0 | `check-gap-status-csv.sh` exit 0 |
| 10.5 | ROADMAP §🚀 Next Action references active gap IDs (not stale) | P1 | grep gap IDs cross-ref to gap-status.csv |

### 2.11 Category 11 — Persona Coverage (P1 review cadence)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 11.1 | All Tier 1 personas have review report ≤90 days old per `personas-catalog.md` | P0 | every Tier 1 row → review file exists + date check |
| 11.2 | Zero 🔴 critical (blocking-launch) gaps in Tier 1 reports | P0 | each report's "Coverage Analysis" table |
| 11.3 | Quarterly cadence respected (latest report ≤current quarter) | P1 | EOQ window check |
| 11.4 | `personas-catalog.md` `next_review` frontmatter not overdue | P1 | grep `next_review:` |
| 11.5 | Audit-driven persona gaps tracked in ROADMAP §🚀 | P1 | cross-ref persona-* gap IDs |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Cat 6 UI/UX 7/10 — averaged design + a11y + responsive" | Each sub-check pass/fail; lowest sub-check pulls category |
| "Score 8/10, only minor gaps" without listing the gaps | Bug list MUST enumerate every FAIL before computing score |
| Skip Cat 11 because "data pending" | Cat 11 stays 5/10 baseline (per current §11) but P0 sub-checks (Tier 1 persona reviews exist) MUST be evaluated honestly |
| "Total 85/110, B+ grade" while Cat 8 has P0 fail | Audit-level verdict = FAIL when ANY P0 sub-check FAILS; total score is descriptive only |
| Score subagents return aggregated category score only | Subagents MUST return per-check evidence list per §3 process step |
| Use "audit pending CI" as excuse to skip Cat 5 | If CI in_progress per `quality-audit/SKILL.md` §"CRITICAL: CI phải hoàn thành" — mark Cat 5 ❓ UNCHECKED + retry |

---

## 4. Bug-finding > scoring primacy (BLOCKING)

> **A `quality-audit` run's purpose is to surface bugs the dev team cannot trust other layers (CI, code review, per-domain skills) to catch. A high `/110` score with hidden P0 bugs is WORSE than a low score that lists every finding honestly.** Per Wave 71c security-audit pattern (PR #1278), Wave 71b incident scored `87/100` on security while missing 5 P0 OWASP A07 gaps because averaging hid them. Same averaging risk applies to `quality-audit` Cat 1-11.

Rules for every `quality-audit` run:

1. **Enumerate ALL §2 sub-checks across 11 categories.** NEVER skip "obviously fine."
2. **Each sub-check returns** `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED`. No partial credit.
3. **Final output starts with bug list** (every FAIL surfaces with severity + evidence) BEFORE the score table.
4. **Score is descriptive only.** Audit-level verdict (`PASS` / `FAIL`) is the deliverable. FAIL if ANY P0 sub-check FAILS regardless of total.
5. **If audit time-budget runs out**, leave remaining sub-checks `❓ UNCHECKED` — do NOT mark PASS by default.

---

## 5. Worked self-test — apply rubric to current main HEAD (2026-05-14)

Walking sample sub-checks against current main state. Surfaces ≥1 finding rubric is concrete:

| Sub-check | Verification (sampled) | Verdict |
|---|---|---|
| 2.3 (Sec) Zero hardcoded secrets | `grep -rE 'password\s*=\s*"[^${]' --include='*.yml'` → check application.yml defaults | ⚠️ Likely FAIL — multiple `${VAR:default}` with localhost defaults per `production-env-config-registry.md` self-test (6 gaps surfaced Wave 71c) |
| 8.1 (Docs) Business docs for every module | `ls documents/01-business/` vs module count | likely PASS (verified Wave 53) |
| 8.2 (Docs) Business docs code-sync | `bash scripts/verify-business-docs.sh` exit 0 | ⚠️ UNCHECKED — script existence not verified in this rule's worked-test scope |
| 9.5 (Code) Outbox pattern enforced | `grep -rn 'rabbitTemplate.convertAndSend' --include='*.java' \| grep -v 'reliability net\|dedicated dispatcher\|test'` | ⚠️ Likely surface ≥1 finding (per `design-patterns.md` §3.5.1 baseline 5 services bypassing Outbox 2026-04-26) |
| 10.4 (PM) Gap CSV sync | `bash scripts/check-gap-status-csv.sh` exit 0 | PASS (verified Wave 64 baseline) |
| 11.1 (Persona) Tier 1 reviews ≤90 days | `ls -lt documents/00-brd/persona-reviews/*.md` | ⚠️ Likely FAIL — per current quality-audit/SKILL.md §Cat 11 "data pending GAP-152" (reviews not shipped) |

**Verdict:** ≥3 P0/P1 FAIL surfaced retroactively (2.3 hardcoded-secret-defaults, 9.5 Outbox bypass, 11.1 persona reviews). Rule fires correctly — averaging within Wave 53 `85/110` baseline hid these because Cat 2, Cat 9, Cat 11 sub-checks weren't enumerated per-check. **Self-test PASS** as worked example: rubric concrete + surfaces real bugs ✅.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 quality-audit/SKILL.md rubric extension (paired same PR)

`.claude/skills/quality-audit/SKILL.md` Bước 2 extended with §"Per-check scoring" subsection citing this rule. Each Category's `| Tiêu chí | Điểm | Check |` table is the per-check rubric; FAIL semantics per §1 apply.

### 6.2 Pre-promotion gate

Before any release tag `v1.0.0-rc.*` or `v1.0.0`, `quality-audit` run MUST report ZERO P0 FAILs across §2.1-§2.11. Manual checklist suffices for v1.0.0; script detector deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

### 6.3 Reviewer checklist

PR reviewer for any `quality-audit` output:
- [ ] Bug list precedes score table?
- [ ] Each Category lists per-check verdicts (not aggregated)?
- [ ] If any P0 FAIL: audit-level verdict marked FAIL?

### 6.4 Override mechanism

```
git commit -m "...
QUALITY_AUDIT_RUBRIC_DEFER: <check ID + reason — e.g., Cat 11 data pending GAP-152>
QUALITY_AUDIT_RUBRIC_FOLLOWUP: <gap link with completion date>"
```

Trailer logged. Pattern frequency >2 defers per audit = meta-review of rubric.

### 6.5 Detector (deferred)

Future `scripts/check-quality-audit-rubric.sh` parses audit report markdown + verifies bug list precedes score + every Category has per-check breakdown. Defer until 2nd recurrence of averaging-hide incident.

---

## 7. Log

- **2026-05-14 (v1.0.1):** PATCH — added `paths:` frontmatter per Wave 73 Bucket A1 path-scope. No constraint change; rule auto-loads only when matching files in context.
- **2026-05-14 (v1.0.0):** Rule created closing GAP-523 META P0 (Wave 72b Bucket E). Triggered by Wave 71c security-audit pattern (`pre-launch-auth-hardening-checklist.md` + security-audit/SKILL.md §2 primacy) — Wave 71b incident `87/100` averaged sub-checks hiding 5 P0 OWASP A07 gaps. `quality-audit` skill /110 11 categories has same averaging risk; this rule generalizes per-check pass/fail + bug-finding-primacy pattern. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (Wave 71c retro identified 6 audit skills with same flaw) → Classify ✓ (no rule enforces per-check pass/fail for quality-audit Cat 1-11) → Rule+Enforce ✓ (this file + quality-audit/SKILL.md §"Per-check scoring" extension paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§5 worked example on current main — 3 P0/P1 FAILs surfaced: 2.3 hardcoded-secret-defaults, 9.5 Outbox bypass, 11.1 persona reviews) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds previously-vague per-check enforcement, no constraint loosening for prior audits; existing `85/110` baseline grandfathered; rule applies prospectively from Wave 72b forward). Detector wiring deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days; v1.0.0 enforcement = skill rubric extension + reviewer-checklist sufficient.
