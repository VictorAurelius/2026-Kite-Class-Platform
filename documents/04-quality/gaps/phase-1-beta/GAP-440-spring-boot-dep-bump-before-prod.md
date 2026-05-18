# GAP-440: Spring Boot 3.5.14 → latest dep bump before v1.0.0 prod tag

**Status:** 🟡 PARTIAL — kiteclass side bumped (Wave 46 Bucket B); awaiting Bucket A (kitehub) + Bucket C (Alpine) + Trivy delta confirmation
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
- **2026-05-08** Wave 46 Bucket A (kitehub) BLOCKED pre-flight: Maven Central confirms 3.5.14 IS the latest 3.5.x patch (no 3.5.15+ exists). Tracked GAP-451. PR #1060 ships GAP-451 + Log entries only — no code changes.
- **2026-05-08** Wave 46 Bucket B (kiteclass): same Spring Boot upstream blocker. Bumped Spring Cloud 2025.0.0 → 2025.0.2 in `kiteclass/kiteclass-gateway/pom.xml` to clear CVE-2025-41253 (spring-cloud-gateway EL injection). kiteclass-core has no Spring Cloud dep. Local `mvn test-compile -P strict-warnings` exit 0 both modules; full `mvn verify` needs Docker (CI runners). PR: wave/46-bucket-b-kiteclass-spring-boot-bump.
- **2026-05-08** Wave 46 SHIPPED closure (PR #1059 plan + #1060 A + #1062 B + #1061 C). GAP-440 stays 🟡 PARTIAL — Spring Cloud delta cleared CVE-2025-41253 (Bucket B); Spring Boot bump pending GAP-451 (await 3.5.15+ upstream). Final close: when GAP-451 resolves + Trivy confirms HIGH count drop on next staging tag.
- **2026-05-16** Wave 86 Bucket B re-scoped: ship test scaffold + heap doc against 3.5.14 baseline; real bump defers GAP-451 upstream. Per `gap-done-discipline.md` §3 PARTIAL exit ramp — baseline scaffold ≠ real fix; GAP-440 stays PARTIAL until GAP-451 resolves. Shipped: (a) `BulkImportAsyncBaselineTest` (2 tests, PASS) pinning 202 ACCEPTED async-accept semantic at `kitehub-subscription/src/test/java/com/kitehub/subscription/baseline/`; (b) `WebhookIdempotencyReplayBaselineTest` (2 tests, PASS) pinning HMAC-SHA256 signature verification + replay handling semantic; (c) `documents/05-guides/operations/heap-baseline-procedure.md` — 7-step NMT procedure cho Ops chạy pre/post bump (alert >10% non-heap delta). Re-run tests post GAP-451 upstream bump để verify Spring Boot semantic preserved. Scope rationale: project does not ship literal `@Async` bulk-import endpoint nor `idempotency_key` column yet — baseline pins the closest existing async-accept + signed-payload-replay semantics so future Spring framework upgrade can be diffed against verified PASS state.
