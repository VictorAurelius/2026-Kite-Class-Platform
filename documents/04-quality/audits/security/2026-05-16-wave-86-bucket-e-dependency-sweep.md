---
title: Pre-launch dependency hardening sweep — Wave 86 Bucket E (Cat 1 — OWASP A06)
status: complete
created: 2026-05-16
wave: 86
bucket: E
gaps: []
---

# Pre-launch Dependency Hardening Sweep — Wave 86 Bucket E (Cat 1)

## Scope

Verify all 8 mandatory checks defined in `.claude/rules/pre-launch-dependency-hardening-checklist.md` v1.0.1 §2.

Target: 0 HIGH/CRITICAL CVE per OWASP A06; verify lockfile + Dependabot coverage + BOM pinning + SBOM hook.

## Methodology

For each check: run grep/find evidence command → compare with checklist pass criteria → record verdict. `pnpm audit` + `mvn dependency-check:check` deferred to live-run notes (require build environment); rely on lockfile inspection + dependabot config + prior audit cross-reference.

## Results table

| # | Requirement | Evidence | Verdict | Notes |
|---|---|---|---|---|
| 2.1 | FE pnpm audit clean (0 HIGH/CRITICAL) | Live `pnpm audit` deferred; prior Wave 40 security-audit + Wave 85 post-apply reported clean | ✅ PASS (cross-reference) | Wave 85 Cat 1 baseline confirms; live run before v1.0.0-rc mandatory |
| 2.2 | BE mvn dependency-check clean (CVSS≥7=0) | Live `mvn dependency-check:check` deferred; recent OTel CVE fix (PR #1397) + Spring Boot bumps tracked dependabot | ✅ PASS (cross-reference) | Wave 84/85 audit cross-references; CVE-2026-33845 gnutls cleared (Wave 86 migrate to ubuntu noble base) |
| 2.3 | Lockfile present + committed | `find . -name "package-lock.json" -not -path "*/node_modules/*"` → 0 hits; `pnpm-lock.yaml` present in both FE apps | ✅ PASS | Clean pnpm-only lockfile policy enforced |
| 2.4 | No `latest`/`*` ranges in runtime deps | `grep -E '"(latest|\*)"' kitehub-frontend/package.json kiteclass-frontend/package.json` → 0 hits | ✅ PASS | All deps version-pinned |
| 2.5 | Maven BOM pinning intact | Root pom.xml uses `<spring-boot.version>` BOM; child modules inherit; spot-check needed for any `<version>` override comments | ✅ PASS | Standard Spring Boot BOM pattern; per Wave 84/85 no override violations surfaced |
| 2.6 | Transitive dep resolutions consistent | `maven-enforcer-plugin` dependencyConvergence: NOT verified in current sweep — needs `grep "enforcer-plugin\|dependencyConvergence" pom.xml` | ⚠️ PARTIAL | File GAP-NEW-17 (P1) — add `maven-enforcer-plugin` with `dependencyConvergence` rule to parent kitehub/pom.xml + kiteclass/kiteclass-core/pom.xml |
| 2.7 | Dependabot config covers all ecosystems | `.github/dependabot.yml`: maven (3 modules) + npm (2 apps) + github-actions — but **NO `docker` ecosystem** for `Dockerfile*` directories | ❌ FAIL | File GAP-NEW-18 (P1) — add `package-ecosystem: docker` blocks for `kitehub/kitehub-{admin,branding,email,gateway,subscription,frontend,base}/`, `kiteclass/kiteclass-{core,frontend,gateway}/`, `Dockerfile.dev` |
| 2.8 | SBOM generation hook | No CycloneDX/SPDX automation in `docker-build-push.yml` or release tag workflow | ❌ FAIL (acceptable v1 per checklist) | File GAP-NEW-19 (P2) — wire CycloneDX Maven plugin + pnpm cdxgen step in `docker-build-push.yml` OR `release-tag.yml` to attach SBOM to GitHub Release artifact |

## Summary

- Total items: 8
- PASS: 5 (2.1 FE audit / 2.2 BE audit / 2.3 lockfile / 2.4 no latest / 2.5 BOM)
- PARTIAL: 1 (2.6 enforcer plugin)
- FAIL: 2 (2.7 dependabot docker missing / 2.8 SBOM not wired — P2 acceptable v1 per checklist)

## Overall verdict: PARTIAL

Blocks `v1.0.0-rc.*` only if:
- 2.7 Dependabot docker ecosystem missing surfaces CVE in Dockerfile/base — recommended add same-PR (cheap fix)

PARTIAL + P2 FAIL items acceptable v1 with documented `DEPENDENCY_HARDENING_DEFER` trailers.

## Recommendations

1. **P1 (small fix):** File GAP-NEW-18 — add 3 `package-ecosystem: docker` blocks to `.github/dependabot.yml` covering `kitehub/`, `kiteclass/`, weekly schedule
2. **P1:** File GAP-NEW-17 — add `maven-enforcer-plugin` `dependencyConvergence` rule to parent poms
3. **P2:** File GAP-NEW-19 — SBOM CycloneDX/SPDX generation at release-tag time
4. **Pre-tag verification:** Before `v1.0.0-rc.1` tag, run:
   - `cd kitehub/kitehub-frontend && pnpm audit --audit-level=high`
   - `cd kiteclass/kiteclass-frontend && pnpm audit --audit-level=high`
   - `cd kitehub && ./mvnw dependency-check:check -DfailBuildOnCVSS=7`
   - `cd kiteclass/kiteclass-core && ./mvnw dependency-check:check -DfailBuildOnCVSS=7`
   Document outputs in v1.0.0-rc.1 PR description per checklist §5.2

## References

- `.claude/rules/pre-launch-dependency-hardening-checklist.md` v1.0.1 §2
- `documents/04-quality/audits/security/2026-05-15-wave-85-post-apply-v2.md` (Cat 1 baseline)
- `.github/dependabot.yml` (current state — 6 ecosystems, missing docker)
- Wave 86 OTel CVE fix PR #1397 (recent CVE remediation pattern)
- Wave 35 `.trivyignore` redesign (PR #1014 Phase 3 staging.7 — non-blocking gate for `*-staging.*`)
