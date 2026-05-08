# GAP-440: Spring Boot 3.5.14 → latest dep bump before v1.0.0 prod tag

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — required before v1.0.0 production tag (NOT blocking Phase 1 BETA staging)
**Domain:** Backend / Security / DevOps
**Found:** 2026-05-08 (Phase 3 staging.4 retro — 48 HIGH+ CVE found across 10 services)
**Affects:** All 7 Java services (kitehub-{subscription,gateway,branding,admin,email} + kiteclass-{core,gateway})

## Problem

Spring Boot 3.5.14 transitive deps trigger 48 HIGH+ CVEs across all 10 service images:
- 3× Tomcat 10.1.53 (CVE-2026-34483/486/487)
- 1× Jackson Core 2.19.4 (GHSA-72hv-8253-57qq)
- 1× Commons BeanUtils 1.9.4 (CVE-2025-48734)
- 1× Commons FileUpload (CVE-2025-48976)
- 1× PostgreSQL JDBC (CVE-2026-42198)
- 5× Netty (CVE-2026-42577/579/583/584/587)
- 1× BouncyCastle (CVE-2026-5598)
- 1× Spring Cloud Gateway (CVE-2025-41253)

Phase 1 BETA staging.* tags allowed CRITICAL-only gate per `release-deploy-standard.md` §3.1; production v1.0.0 tag requires HIGH+CRITICAL strict bar → MUST clear most CVEs before promotion.

## Root Cause

Spring Boot version pinned at parent pom 3.5.14. Latest Spring Boot 3.5.x patches likely bump tomcat-embed + jackson + netty to clean versions. Tika + Commons BeanUtils require explicit `<dependencyManagement>` overrides per-service.

## Proposed Fix

1. Update `kitehub/pom.xml` parent: `spring-boot-starter-parent 3.5.14 → 3.5.16+` (latest stable patch)
2. Verify Tomcat embed bumps to ≥10.1.55 (clears 3 CVEs)
3. Verify Jackson bumps to ≥2.20 (clears GHSA-72hv-8253-57qq)
4. Per-service `<dependencyManagement>` overrides for residual:
   - `commons-beanutils 1.10.0+` (CVE-2025-48734)
   - `commons-fileupload 1.6+` (CVE-2025-48976 if applicable)
5. Run full mvn clean verify -P strict-warnings on all 7 services
6. Re-tag staging.N → confirm GitHub Security alerts drop to 0 HIGH/CRITICAL
7. Then ship v1.0.0-rc.1 with strict gate

## Acceptance Criteria

- [ ] kitehub/pom.xml Spring Boot bumped + green CI
- [ ] All 7 Java services build clean with strict-warnings
- [ ] Trivy HIGH+CRITICAL count on production tag = 0 (per service) OR documented exception per Trivy best practice
- [ ] GitHub Security alerts ≤5 (only acceptable LOW/MEDIUM remaining)

## Related

- Parent: `release-deploy-standard.md` §3.1 (PRE-RELEASE vs MAJOR vuln gates)
- Sibling: GAP-441 (per-service pom override hygiene), GAP-442 (alpine bump)
- Phase 3 staging retro: this session 2026-05-08 (`staging.4` 48 CVE count)

## Log

- **2026-05-08** Filed during Phase 3 staging.5 retro. CRITICAL-only staging gate unblocks Phase 1 BETA; this gap closes before v1.0.0-rc.
- **2026-05-08** Wave 46 Bucket A (kitehub side) attempted but BLOCKED at pre-flight: Maven Central authoritative version list shows `3.5.14` IS the latest 3.5.x patch — no newer 3.5.15+ exists to bump to (verified via `maven-metadata.xml` + directory listing). Status remains 🔵 OPEN; blocking concern tracked in GAP-451 (await upstream Spring Boot 3.5.15 release). Bucket A PR #TBD ships the GAP-451 file + this Log entry only — no code changes. Bucket B (kiteclass) likely faces identical blocker; coordinator to verify.
