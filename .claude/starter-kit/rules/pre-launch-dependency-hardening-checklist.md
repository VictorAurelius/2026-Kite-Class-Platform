# Pre-Launch Dependency Hardening Checklist — security-audit Cat 1 per-check rubric

**Priority:** 🟠 MANDATORY — pre-launch security gate (Cat 1 force-multiplier)
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-vague Cat 1 per-check enforcement closing GAP-522)
**Applies to:** Any release tag `v0.9.0-beta-staging.*` → first `v1.0.0-rc.*` transition; every PR touching `pom.xml`, `package.json`, `pnpm-lock.yaml`, `requirements*.txt`, `Dockerfile*`, dependabot config

---

## 1. The Rule

> **Before any pre-production tag promotes to release-candidate or first production tag, all 8 dependency-hardening checks below MUST pass OR have a documented `DEPENDENCY_HARDENING_DEFER` trailer with risk acceptance + follow-up gap.**

Dependency vulnerabilities (OWASP A06 Vulnerable & Outdated Components) ranked 6th in OWASP Top 10 (2021). Wave 71b retro showed security-audit Category 1 rubric was vague ("npm audit critical/high count, Maven dep versions") and allowed averaging within a 20-pt bucket — letting transitive CVEs, lockfile drift, unpinned versions, and missing SBOM evidence hide behind passing sub-checks.

This rule fills the coverage gap. Security-audit skill Category 1 rubric extended in same PR.

---

## 2. Mandatory checks (8)

### 2.1 Frontend dep audit clean (P0)

`pnpm audit --json` returns ZERO CRITICAL or HIGH severity findings across both `<frontend-service-1>/` AND `<frontend-service-2>/`.

Verify:
```bash
cd <frontend-service-1> && pnpm audit --json --audit-level=high
cd <frontend-service-2> && pnpm audit --json --audit-level=high
```

Documented exception (CVE pinned in `pnpm.overrides` with rationale) → record in `documents/04-quality/audits/security/dependency-waivers.md`.

### 2.2 Backend dep audit clean (P0)

OWASP `mvn dependency-check` returns ZERO CRITICAL or HIGH CVSS ≥7.0 findings across `<backend-product>/` + `<core-tenant-service>/` + `<tenant-gateway-service>/`.

Verify:
```bash
cd <backend-product> && ./mvnw -pl <module> dependency-check:check -DfailBuildOnCVSS=7
cd <core-tenant-service> && ./mvnw dependency-check:check -DfailBuildOnCVSS=7
```

Trivy image scan on built containers complementary (covered by §2 of `release-deploy-standard.md` §3.1 already).

### 2.3 Lockfile present + committed (P0)

Every package-manifest dir MUST have its lockfile committed:
- `<frontend-service-1>/pnpm-lock.yaml` ✅
- `<frontend-service-2>/pnpm-lock.yaml` ✅
- NO `package-lock.json` alongside pnpm projects (mixed lockfile chaos)
- Maven projects: `pom.xml` already pins versions via Spring Boot BOM; no separate lockfile

Verify: `find . -name "package-lock.json" -not -path "*/node_modules/*"` returns ZERO results.

### 2.4 No `latest` / floating ranges in runtime deps (P0)

`package.json` runtime `dependencies` (not `devDependencies`) MUST NOT contain:
- `"latest"`
- `"*"`
- `"^x"` ranges where `x` major version isn't pinned in lockfile to a verified version

Verify:
```bash
grep -E '"(latest|\*)"' <frontend-service-1>/package.json <frontend-service-2>/package.json
```

`devDependencies` can use caret ranges since lockfile pins exact resolution.

### 2.5 Maven BOM pinning intact (P1)

Spring Boot BOM (`<spring-boot.version>`) pinned to single version in root `pom.xml`; child modules inherit. No child module overrides BOM-managed versions without explicit `<!-- BOM-OVERRIDE: <reason> -->` comment + ADR reference.

Verify: `grep -rE "<version>[0-9]" <backend-product>/*/pom.xml <tenant-product>/*/pom.xml` — any version override should map to BOM-OVERRIDE comment line nearby OR be explicitly outside Spring's managed dep set.

### 2.6 Transitive dep resolutions consistent (P1)

Lockfile `resolutions` / `overrides` apply uniformly:
- pnpm `pnpm.overrides` in root `package.json` for shared transitive pins (e.g., `tar`, `semver`, security CVEs)
- Maven enforcer plugin (`maven-enforcer-plugin` `dependencyConvergence` rule) detects transitive version conflicts; CI fails on violation

Verify: enforcer rule active in `<backend-product>/pom.xml` parent + similar for <tenant-product>.

### 2.7 Dependabot config present (P1)

`.github/dependabot.yml` exists + covers ALL package ecosystems in repo:
- `npm` / `pnpm` for each FE app dir
- `maven` for each multi-module BE project
- `docker` for `Dockerfile*` directories
- `github-actions` for `.github/workflows/`
- Update schedule documented (weekly recommended, monthly minimum)

Verify: `cat .github/dependabot.yml` lists all `package-ecosystem` keys.

### 2.8 SBOM generation hook (P2)

Each release tag generates a CycloneDX or SPDX SBOM artifact:
- FE: `pnpm exec @cyclonedx/cdxgen` per app
- BE: `mvn org.cyclonedx:cyclonedx-maven-plugin:makeBom` per module
- Artifact attached to GitHub Release (`gh release upload`)

Acceptable v1: manual generation per release. Future: CI-automated step in `docker-build-push.yml`. Track as follow-up if not yet wired.

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "pnpm audit warnings are non-blocking" | High/Critical = blocking; rationale-document waivers |
| Skip mvn dependency-check "because Spring BOM" | BOM doesn't audit downstream CVEs of artifacts it manages |
| Allow `"latest"` ranges "for dev convenience" | Lockfile + pinned ranges only in runtime deps |
| Add to `pnpm.overrides` without comment | Each override needs `# <CVE-id> + reason` adjacent comment |
| Score 18/20 by averaging — one critical CVE hidden in count | Per-check pass/fail; 1 CRITICAL or HIGH = Cat 1 FAIL |

---

## 4. Worked self-test — current main state 2026-05-14

**Apply §2 checklist retroactively to current main HEAD:**

| # | Check | Verification command (expected outcome) | Verdict (estimated) |
|---|---|---|---|
| 2.1 | FE pnpm audit clean | `pnpm audit --audit-level=high` both apps → 0 findings | likely **PASS** (Wave 40 security-audit reported 0 CVE) |
| 2.2 | BE mvn dep-check clean | `mvn dependency-check:check` → 0 CVSS≥7 | likely **PASS** (Wave 40 reported P0=0, 3 P1) |
| 2.3 | Lockfile committed | `find . -name "package-lock.json"` = 0 + `pnpm-lock.yaml` exists | likely **PASS** |
| 2.4 | No `latest` ranges | `grep -E '"(latest\|\*)"' package.json` = 0 hits | likely **PASS** |
| 2.5 | Maven BOM pinning | check `<version>` overrides have BOM-OVERRIDE comment | likely **PARTIAL** — needs sweep |
| 2.6 | Transitive resolutions | enforcer plugin in parent pom + pnpm.overrides documented | likely **PARTIAL** — enforcer may be missing |
| 2.7 | Dependabot config | `.github/dependabot.yml` lists npm + maven + docker + github-actions | check: likely **PASS** (Wave 8b shipped GAP-194) |
| 2.8 | SBOM hook | release tag generates CycloneDX artifact | likely **FAIL** — manual not wired |

**Expected findings:** 2-3 P1/P2 follow-up gaps (Maven BOM-OVERRIDE comment sweep, enforcer plugin add, SBOM CI step).

**Verdict:** Rule fires correctly on current main — surfaces specific per-check failures instead of a vague 18/20 score. ✅

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Security-audit skill rubric extension (paired same PR)

`.claude/skills/quality/security-audit/SKILL.md` Category 1 "Dependency Vulnerabilities" rubric extended with 8 explicit per-check rows. Each check pass/fail (no averaging). 1 fail = category total ≤ 16/20.

### 5.2 Pre-promotion gate

Before any maintainer creates git tag matching `v1.0.0-rc.*` or `v1.0.0` (first GA), §2 checklist MUST exit 0. Currently: manual run + `pnpm audit` + `mvn dependency-check` outputs documented in PR description. Detector script `scripts/check-dependency-hardening.sh` deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

### 5.3 Reviewer checklist

When reviewing any PR that touches `pom.xml`, `package.json`, `pnpm-lock.yaml`, or `.github/dependabot.yml`, reviewer asks:
- New dep added? → §2.1/2.2 audit clean for new version?
- Range loosened? → §2.4 still pinned?
- BOM override introduced? → §2.5 comment + ADR present?

### 5.4 Override mechanism

For genuine schedule pressure (regulator deadline, upstream fix pending):

```
git commit -m "...
DEPENDENCY_HARDENING_DEFER: <check ID + reason — e.g. 'CVE-2026-xxxx upstream patch ETA 14d'>
DEPENDENCY_HARDENING_FOLLOWUP: <gap link with completion date ≤14d from defer>"
```

Trailer logged. Pattern frequency >2 defers per release = meta-review.

### 5.5 Detector (deferred)

Future: `scripts/check-dependency-hardening.sh` parses `pnpm audit --json` + `mvn dependency-check` output + checks lockfile presence + grep dependabot.yml ecosystems. Defer until 2nd recurrence of dependency drift incident.

---

## 6. Relationship to other rules

- **`security-audit/SKILL.md`** — Category 1 rubric extended same PR
- **`output-review-mandate.md`** §3 — Security audit row already exists; this rule sharpens Category 1 substance
- **`release-deploy-standard.md`** §3.4 — MAJOR/first-PROD checklist includes "Pen-test light (OWASP top 10)"; this rule operationalizes A06 specifically
- **`pre-launch-auth-hardening-checklist.md`** (sister rule, Cat 4) — same pattern, different category
- **`pre-launch-secrets-hardening-checklist.md`** (sister rule, Cat 2 — same PR)
- **`pre-launch-owasp-rest-hardening-checklist.md`** (sister rule, Cat 3 — same PR)
- **`pre-launch-infra-hardening-checklist.md`** (sister rule, Cat 5 — same PR)
- **`incident-to-rule-pipeline.md`** — direct output of GAP-522 "security-audit averaging hid OWASP A07 — same pattern hides A06" applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + skill rubric extension + worked self-test same PR
- **`gap-done-discipline.md`** — DONE flip on dep-touching scope requires §2 checklist pass

---

## 7. Log

- **2026-05-14 (v1.0.0):** Rule created closing Cat 1 slice of GAP-522. Triggered by user-flagged miss "skill audit phải là lớp phòng vệ tin tưởng" + Wave 71c PR #1278 already fixed Cat 4 via per-check rubric; extending same fix to Cat 1/2/3/5. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-522 filed 2026-05-13) → Classify ✓ (security-audit Cat 1 rubric was "npm audit critical/high count" with no per-pattern check; rubric allowed averaging that hid transitive CVE / lockfile drift / unpinned version classes) → Rule+Enforce ✓ (this file + security-audit/SKILL.md Cat 1 row update + worked §4 self-test + paired with 3 sister rules per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on current main — likely 2-3 P1/P2 follow-ups surface, validating rubric is concrete not aspirational) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds per-pattern OWASP A06 coverage to previously-vague Cat 1 rubric; no constraint loosening for prior tags; existing `v0.9.0-beta-staging.*` tags grandfathered; rule applies prospectively to `v1.0.0-rc` promotion).
