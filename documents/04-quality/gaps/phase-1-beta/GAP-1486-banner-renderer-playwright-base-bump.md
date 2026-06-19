# GAP-1486: banner-renderer Playwright base bump v1.49.1 → v1.55.1 (CVE reduction + version sync)

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** DevOps
**Phase:** phase-1-beta
**Found:** 2026-06-19 (repo-status audit — 96 medium/low Trivy base-image CVE)
**Affects:** `kitehub/kitehub-banner-renderer/Dockerfile`

## Problem

96 medium/low Trivy alert (cpython stdlib + **X11 xorg-server** CVE-2025-26594..26601 + system libs) trong base image `mcr.microsoft.com/playwright:v1.49.1-noble` của banner-renderer (Node sidecar render banner qua headless Chromium). KHÔNG phải code mình — system packages bundled cho browser automation.

**Root cause kép:** `package.json` đã pin `playwright: 1.55.1` nhưng Dockerfile base kẹt `v1.49.1-noble` (Dec-2024) → (a) latent browser-version mismatch bug (npm client 1.55.1 vs base browser 1.49.1); (b) base cũ ~6 tháng → CVE backlog.

## Fix (DONE this PR — local-verified)

Sync Dockerfile base `v1.49.1-noble` → `v1.55.1-noble` (khớp package.json, đúng comment Dockerfile "pin base tag = package.json playwright version"). Fixes mismatch + pulls patched Ubuntu-noble base.

**Local verify (docker — không cần AWS):**
- `docker build` OK (base v1.55.1 + npm install + sharp).
- `docker run` + `GET /health` → HTTP 200.
- `POST /render` HTML→WebP → HTTP 200, `image/webp`, valid WebP 300×100, 1656B.

## Acceptance Criteria

- [x] Dockerfile base synced to v1.55.1-noble (matches package.json)
- [x] docker build + run + render smoke PASS local
- [ ] CVE-reduction confirmed ở next image scan (tag/dispatch — AWS-gated, như GAP-1485)

## Related

- Sibling base-CVE cleanup: GAP-1485 (Trivy skip npm-vendored) + GAP-400 (Trivy scan) + GAP-442 (base bump cluster)
- Audit verdict 2026-06-19 (base-image system-package CVE, not our code)
