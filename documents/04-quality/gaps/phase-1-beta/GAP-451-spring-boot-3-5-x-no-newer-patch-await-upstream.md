# GAP-451: Spring Boot 3.5.x — no newer patch available, await upstream

**Status:** 🔵 OPEN — blocking GAP-440 closure
**Priority:** 🟠 P1 — gates production v1.0.0 tag together with GAP-440
**Domain:** Backend / Security / DevOps
**Found:** 2026-05-08 (Wave 46 Bucket A pre-flight state-check)
**Affects:** All 7 Java services (kitehub × 5 + kiteclass × 2) — same scope as GAP-440

## Problem

Wave 46 Bucket A attempted to bump Spring Boot from `3.5.14` → `3.5.X` (latest stable patch). Pre-flight state-check against Maven Central revealed **3.5.14 IS the latest 3.5.x release**. No newer patch (3.5.15, 3.5.16, ...) exists yet to bump to.

GAP-440 §Proposed Fix step 1 ("Update parent: spring-boot-starter-parent 3.5.14 → 3.5.16+") cannot complete on this date because the target version does not yet exist upstream.

## Verification (state-check evidence)

```bash
# 1. maven-metadata.xml authoritative version list
curl -sf https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml \
  | grep -oE '<version>3\.5\.[0-9]+</version>' | tail -15

# Output:
# <version>3.5.0</version> ... <version>3.5.13</version> <version>3.5.14</version>
# (3.5.14 is the LAST entry — no 3.5.15+)

# 2. Directory listing cross-check
curl -sf https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/ \
  | grep -oE 'href="3\.5\.[0-9]+/"' | sort -V | tail -5
# → 3.5.10/, 3.5.11/, 3.5.12/, 3.5.13/, 3.5.14/  (no 3.5.15+)

# 3. Current parent confirmed at 3.5.14
grep -A1 spring-boot-starter-parent kitehub/pom.xml
# → <version>3.5.14</version>
```

Spring Boot 4.1.0-RC1 is `<latest>` per metadata but NOT a 3.5.x patch — out of scope (would be MAJOR upgrade requiring separate evaluation; both `kitehub/pom.xml` and `kiteclass/kiteclass-core/pom.xml` are pinned to 3.5.x line per current architecture).

## Root Cause

Upstream Spring Boot release cadence — the gap was filed 2026-05-08 anticipating CVE-clearing patches that have not yet shipped. GAP-440 §Proposed Fix assumed "3.5.16+" available; reality is that 3.5.14 (released earlier 2026) is still current.

## Proposed Fix

Two-track wait + monitor:

1. **Subscribe to Spring Boot release notifications** — watch `https://github.com/spring-projects/spring-boot/releases` OR check Maven Central weekly via:
   ```bash
   curl -sf https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml \
     | grep -oE '<version>3\.5\.[0-9]+</version>' | tail -1
   ```
2. **When 3.5.15+ ships:** re-run Wave 46 Bucket A (kitehub) + Bucket B (kiteclass) with the new target version. GAP-440 closure unblocks at that point.
3. **Alternative path (if patch delays >4 weeks):** re-evaluate per-service `<dependencyManagement>` overrides for the most critical CVEs (Tomcat, Jackson, Netty, BouncyCastle) per GAP-440 §Proposed Fix step 4. This is a higher-effort/risk path because it bypasses Spring Boot's curated dependency BOM — defer unless v1.0.0 tag deadline forces it.

## Acceptance Criteria

- [ ] Spring Boot 3.5.15 (or higher 3.5.x) confirmed available on Maven Central
- [ ] GAP-440 Bucket A + Bucket B re-run with target version
- [ ] Per-service `mvn verify -P strict-warnings` clean across 7 modules
- [ ] Trivy CVE count clears HIGH+CRITICAL gate per GAP-440 §AC
- [ ] OR (if alternative path taken): `<dependencyManagement>` overrides documented per service + tested

## Related

- Parent: GAP-440 (Spring Boot 3.5.14 → latest dep bump before v1.0.0 prod tag) — this gap blocks step 1 of GAP-440 §Proposed Fix
- Sibling: GAP-441 (per-service pom override hygiene), GAP-442 (alpine base bump)
- Wave plan: `documents/03-planning/waves/wave-46-java-deps-bump.md`
- Standard: `release-deploy-standard.md` §3.4 (production v1.0.0 vuln gate)

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (Spring Boot 3.5.x patch wait; gates v1.0.0 production release).
- **2026-05-08** Filed during Wave 46 Bucket A pre-flight state-check. Maven Central confirmed `<release>4.1.0-RC1</release>` and last 3.5.x = `3.5.14` — no newer patch available. Bucket A implementation halted per task spec ("if 3.5.14 is still latest, document and STOP"). Bucket B (kiteclass) likely faces same blocker; coordinator to confirm. GAP-440 stays 🔵 OPEN; blocked on upstream.
- **2026-05-16** Wave 86 Bucket B prior agent re-confirmed Maven Central state: 3.5.14 IS still latest 3.5.x patch; no 3.5.15+ release yet. GAP-451 remains OPEN/blocked on upstream. Wave 86 Bucket B re-scoped to ship baseline scaffold against current 3.5.14 — this PR shipped: (a) 2 baseline tests `BulkImportAsyncBaselineTest` + `WebhookIdempotencyReplayBaselineTest` at `kitehub-subscription/src/test/java/com/kitehub/subscription/baseline/` (4 tests PASS); (b) `documents/05-guides/operations/heap-baseline-procedure.md` — 7-step NMT procedure Ops runs pre/post any future Spring Boot bump. When GAP-451 resolves (3.5.15+ upstream lands), re-run these 4 baseline tests + execute heap-baseline-procedure pre/post the bump to verify semantic preserved.
