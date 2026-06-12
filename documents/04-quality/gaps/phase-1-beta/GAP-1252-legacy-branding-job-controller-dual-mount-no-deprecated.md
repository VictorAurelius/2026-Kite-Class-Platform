# GAP-1252: Legacy BrandingJobController dual-mount cạnh V1 không có @Deprecated + removal contract

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (API versioning/deprecation)
**Found:** 2026-06-12 (post-wave audit suite — api-contract-audit, base SHA `1f6baea26`)
**Affects:** `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/controller/BrandingJobController.java`

## Problem

Hai job controller cùng tồn tại (dual-mount):
- Legacy: `BrandingJobController` `@RequestMapping("/api/platform/branding/jobs")` (line 43)
- V1: `BrandingJobV1Controller` `@RequestMapping("/api/v1/branding/jobs")` (line 58)

`BrandingJobV1Controller` javadoc (line 50) NHẮC đến legacy controller ("The legacy controller `BrandingJobController` at...") → có awareness về supersession, NHƯNG:
- `BrandingJobController` KHÔNG có annotation `@Deprecated` (state-check `grep -nE "@Deprecated" BrandingJobController.java` → 0 hit)
- Không có removal-date contract / migration guide cho consumer
- Legacy `/api/platform/branding/jobs/**` endpoints cũng undocumented (xem GAP-1251)

Rubric §2.4.3 P1 ("Deprecated endpoints marked `@Deprecated` + docs flag + removal date") FAIL: superseded controller chạy song song mà không signal deprecation cho consumer → ambiguity về endpoint nào canonical.

## State-check evidence

```
grep -nE "@Deprecated|RequestMapping" BrandingJobController.java
  → 43: @RequestMapping("/api/platform/branding/jobs")   (no @Deprecated)
grep -nE "@Deprecated|legacy" BrandingJobV1Controller.java
  → 50: javadoc mentions "legacy controller BrandingJobController"  (awareness, no annotation)
```

## Proposed Fix

Mark `BrandingJobController` `@Deprecated` + javadoc removal-date (≥6 tháng notice per `versioning-policy.md`) + api-contract.md deprecation flag. Hoặc nếu legacy đã không consumer nào dùng → xác nhận + remove (cùng `cross-flow-bug-class-sweep.md` sweep các consumer).

## Acceptance Criteria

- [ ] `BrandingJobController` có `@Deprecated` + removal-date javadoc HOẶC được remove sau caller-sweep
- [ ] api-contract.md flag legacy namespace deprecated với migration → `/api/v1/branding/jobs`

## Related

- Discovered in: post-wave audit suite 2026-06-12 (`documents/04-quality/audits/api-contract/2026-06-12-api-contract-audit.md`)
- Sister: GAP-1251 (branding endpoints undocumented — legacy /jobs namespace overlap)
- Cross-rule: `api-contract-change-caller-sweep.md` (nếu remove); `versioning-policy.md` §7.1
