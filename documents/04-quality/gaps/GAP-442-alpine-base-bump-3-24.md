# GAP-442: Alpine 3.23 → 3.24+ base image bump (10 Dockerfiles)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — required before v1.0.0 production tag
**Domain:** DevOps / Security
**Found:** 2026-05-08 (Phase 3 staging.5 — CVE-2026-33845 gnutls CRITICAL in alpine 3.23)
**Affects:** All 10 Docker images (Java + Node alpine bases)

## Problem

Alpine 3.23 base contains gnutls vulnerable to CVE-2026-33845 (CRITICAL). Affects all services because both `eclipse-temurin:*-jre-alpine` and `node:*-alpine` reuse same alpine base layer.

Currently masked by .trivyignore for staging.* tags. Production v1.0.0 strict gate would block.

## Root Cause

Dockerfile `FROM` lines pinned to alpine 3.23-derived images. Upstream fix in alpine 3.24+.

## Proposed Fix

1. Audit all 10 Dockerfiles for `FROM` lines
2. Bump base images:
   - `maven:3.9-eclipse-temurin-17/21-alpine` → check if newer alpine available; fallback `eclipse-temurin:*-jdk-alpine` (multi-arch supported per GAP-440)
   - `eclipse-temurin:17/21-jre-alpine` → latest patch
   - `node:20-alpine` → latest patch
3. Re-build all 10 services on staging.N
4. Verify CVE-2026-33845 cleared from Trivy scan
5. Remove `.trivyignore` entry for CVE-2026-33845

## Acceptance Criteria

- [ ] 10 Dockerfiles bumped to alpine 3.24+ derived
- [ ] Trivy scan finds 0 gnutls HIGH/CRITICAL
- [ ] `.trivyignore` entry for CVE-2026-33845 removed
- [ ] Image size delta acceptable (<10% growth)

## Related

- Sibling: GAP-440 (Spring Boot bump), GAP-441 (pom hygiene)
- Discovery: Phase 3 staging.5 retro

## Log

- **2026-05-08** Filed during Phase 3 staging.5 retro. Pairs with GAP-440 + GAP-441 as production-readiness trio before v1.0.0 tag.
