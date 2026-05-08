# GAP-441: Per-service pom.xml dep override hygiene

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 — quality improvement
**Domain:** Backend / DevOps
**Found:** 2026-05-08 (Phase 3 staging.4 — 48 unique HIGH+ CVE not consistently overridden)
**Affects:** All 7 Java services

## Problem

Phase 3 staging.4 Trivy scan found `tika-core 2.9.2` in kitehub-email image despite kiteclass-core/pom.xml declaring `<tika.version>3.3.0</tika.version>` override. Override scoped to single pom; doesn't propagate.

Similar pattern: jackson-core override in 1 service doesn't carry to siblings.

## Root Cause

Maven multi-module project: each service has own pom.xml. `<dependencyManagement>` in parent kitehub/pom.xml would propagate; per-service overrides are local-only.

## Proposed Fix

1. Audit which deps need centralized override (Tika, Jackson, BouncyCastle, etc.)
2. Move overrides from per-service pom to parent kitehub/pom.xml `<dependencyManagement>` block
3. Same for kiteclass parent pom
4. Re-build + verify Trivy detection sees correct version across all images

## Acceptance Criteria

- [ ] Centralized dep version overrides in 2 parent poms
- [ ] Per-service pom only declares what's truly service-specific
- [ ] Trivy detects same version of shared dep across all images

## Related

- Sibling: GAP-440 (Spring Boot bump), GAP-442 (alpine bump)
- Discovery: Phase 3 staging.4 retro

## Log

- **2026-05-08** Filed during Phase 3 staging retro. Quality gap, not deploy-blocking.
