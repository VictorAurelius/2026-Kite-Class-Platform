# GAP-728: `TestSecurityConfig` missing `@EnableMethodSecurity` — `@PreAuthorize` NO-OP trong test

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core test infrastructure)
**Detected:** 2026-05-24 (Wave beta-readiness-1 Bucket D audit, PR #1763)
**Affects:** Mọi `@SpringBootTest` integration test verify `@PreAuthorize` annotation

## Problem

`TestSecurityConfig` trong test profile thiếu `@EnableMethodSecurity` annotation. Hậu quả: `@PreAuthorize`/`@PostAuthorize` annotations trên controller methods là **NO-OP** trong test context.

→ Tests PASS dù authz logic broken hoặc absent. False-confidence test coverage = potential security regression escape qua CI.

## Evidence (PR #1763 Bucket D)

Bucket D audit thử test cross-tenant authz qua `@PreAuthorize` guards. Tests PASS bằng cách Hibernate filter (tenant_id WHERE clause) chứ KHÔNG phải @PreAuthorize guard fire.

Verified empirically: thêm fake `@PreAuthorize("denyAll()")` lên controller method → test STILL PASSES (annotation NO-OP).

## Root Cause

Spring Security 6+ chuyển `@EnableGlobalMethodSecurity` → `@EnableMethodSecurity` (more granular). Test config (`TestSecurityConfig.java` hoặc equivalent) chưa migrate hoặc thiếu annotation.

## Proposed Fix

```java
@TestConfiguration
@EnableMethodSecurity(prePostEnabled = true)  // ADD this annotation
public class TestSecurityConfig {
    // ...
}
```

OR if test uses `@SpringBootTest` with full app context, ensure `SecurityConfig` (production) has `@EnableMethodSecurity` enabled (and not overridden by test config).

## Acceptance Criteria

- [ ] `TestSecurityConfig` has `@EnableMethodSecurity(prePostEnabled = true)`
- [ ] Verify with sanity test: `@PreAuthorize("denyAll()")` on test endpoint → 403 Forbidden trong test (not 200)
- [ ] Re-run all existing authz IT tests to verify no regression
- [ ] Document fix in `documents/05-guides/dev/testing-spring-security.md` (or similar)

### Out-of-scope

- Re-audit all existing `@PreAuthorize` annotations — separate scope per GAP-727 + GAP-729

## Priority Rationale (P1)

Không direct user-facing impact NHƯNG enables hidden security regression. P1 high vì authz cluster (GAP-727 + GAP-729) depend on fixed test infrastructure.

## Related

- PR #1763 Wave beta-readiness-1 Bucket D audit finding A01-METH-01
- GAP-727 (related — broken guard discovered partly do this test gap)
- GAP-729 (related — 11/19 controllers no per-resource guard discovered without test enforcement)
- Wave beta-readiness-2+ candidate
