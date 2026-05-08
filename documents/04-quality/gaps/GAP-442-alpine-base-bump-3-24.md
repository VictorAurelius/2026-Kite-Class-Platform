# GAP-442: Alpine 3.23 → 3.24+ base image bump (10 Dockerfiles)

**Status:** 🟡 PARTIAL — Dockerfiles bumped to noble (alpine 3.24+ unavailable upstream); CI Trivy confirmation pending before `.trivyignore` removal
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

- [x] 10 Dockerfiles bumped — pivoted to Debian noble (Ubuntu 24.04) / `node:22-trixie-slim` because eclipse-temurin / maven / node alpine variants are still pinned at 3.23 upstream (no 3.24+ tag published yet, verified Docker Hub 2026-05-08). Noble is a documented fallback per task spec.
- [ ] Trivy scan finds 0 gnutls HIGH/CRITICAL — pending CI verification on next staging tag
- [ ] `.trivyignore` entry for CVE-2026-33845 removed — gated by CI Trivy confirmation, follow-up PR
- [ ] Image size delta acceptable (<10% growth) — pending CI build size measurement (noble is larger than alpine; expect ~30-50MB delta per service which is acceptable for security parity)

## Related

- Sibling: GAP-440 (Spring Boot bump), GAP-441 (pom hygiene)
- Discovery: Phase 3 staging.5 retro

## Log

- **2026-05-08** Filed during Phase 3 staging.5 retro. Pairs with GAP-440 + GAP-441 as production-readiness trio before v1.0.0 tag.
- **2026-05-08** Wave 46 Bucket C — 10 Dockerfiles bumped to Debian `noble` / `trixie-slim` base. State-check on Docker Hub: eclipse-temurin / maven / node alpine variants topped at 3.23 (no 3.24+ published upstream as of today). Per task spec fallback, switched Java images to `*-noble` (Ubuntu 24.04 LTS, glibc) and Node images to `node:22-trixie-slim` (Debian 13 trixie, Node 22 LTS — clears 11 npm-in-base CVE pile: tar/minimatch/cross-spawn/glob). Adapted package-manager calls (`apk add` → `apt-get install`) and user-creation commands (BusyBox `addgroup -S` / `adduser -S` → util-linux `groupadd --system` / `useradd --system`). Added explicit `wget` install for kiteclass-core/kiteclass-gateway HEALTHCHECK (BusyBox provided `wget` on alpine; Debian doesn't by default). Removed `apk add libc6-compat` from Node frontends (alpine-only musl→glibc shim). Status flipped to 🟡 PARTIAL per `gap-done-discipline.md` §3 — CI Trivy scan + `.trivyignore` removal queued as follow-up PR after staging.N tag confirms CVE-2026-33845 cleared. Local Docker build verification not run (Docker not available in worktree); CI is the verification gate. PR: wave/46-bucket-c-alpine-base-bump.

- **2026-05-08** Wave 46 Bucket C SHIPPED (PR #1061). 10 Dockerfiles bumped: alpine 3.23 → noble (Java services: kitehub-{base,admin,branding,email,gateway,subscription} + kiteclass-{core,gateway}) + node:20-alpine → node:22-trixie-slim (kitehub-frontend + kiteclass-frontend). Coordinator-applied gate raise 220MB → 320MB on `kitehub-frontend-ci.yml` (Debian +60MB vs alpine, acknowledged trade-off). Status 🟡 PARTIAL — `.trivyignore` gnutls cleanup deferred pending Trivy CI confirm CVE-2026-33845 cleared on next staging tag.
