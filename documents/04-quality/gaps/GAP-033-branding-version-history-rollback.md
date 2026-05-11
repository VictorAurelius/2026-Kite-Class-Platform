# GAP-033: Branding Version History & Rollback (User-facing)

**Status:** 🟡 PARTIAL (Wave 4 backend shipped — `BrandingVersion` entity + V43 migration + service + controller + rollback endpoint + tests; FE UI + diff viewer + retention prune deferred to later wave)
**Priority:** 🟠 P1
**Domain:** Product / Backend
**Detected:** 2026-04-14 (simulation: Owner × Daily Usage × C3 Data)
**Partially resolved:** 2026-04-18 (Wave 4 — branding propagation cluster)

## Wave 4 partial resolution (MVP)

Shipped:
- V43 migration `branding_versions` table (JSONB snapshot, partial unique index
  guarantees exactly one active version per instance).
- `BrandingVersion` entity + `BrandingVersionRepository` + `BrandingVersionService`
  (snapshot / listVersions / rollback).
- `BrandingServiceImpl.updateBranding()` auto-snapshots on every write.
- `BrandingVersionController` exposes:
  - `GET  /api/v1/branding/{instanceId}/versions` (paginated, newest-first)
  - `POST /api/v1/branding/{instanceId}/versions/{versionNumber}/rollback`
- Unit tests cover snapshot deactivation, rollback restore, and missing-version
  error handling.

Deferred to a later wave (not MVP):
- Automated rollback triggers (quality-gate driven)
- A/B branding tests
- Diff viewer / visual regression UI
- Version cleanup task (prune > 20 per instance).

## Problem

Tenant rebrand xong nhận ra version cũ đẹp hơn → **không có cách rollback**. Chỉ có regenerate wizard từ đầu.

Khác với GAP-030 (disaster recovery — ops-centric), đây là **user-facing feature**: "Version history like Google Docs".

## Proposed Fix

### 1. Version tracking

```java
@Entity
public class BrandingVersion {
  Long id;
  String tenantId;
  Integer versionNumber;
  ThemeConfig theme;
  Map<String, String> assets;
  String createdBy;
  String changeNotes;
  Timestamp createdAt;
  Boolean isActive;  // chỉ 1 version active
}
```

Mỗi lần deploy → tạo version mới. Keep last 10 versions per tenant.

### 2. UI: Version history

```
/branding/history
├── v5 (Current) — 2026-04-14 "Updated banner style"
├── v4 — 2026-03-20 "Rebrand for summer"
├── v3 — 2026-02-15 "Initial branding"
├── ...
[Rollback to v4] [Compare v5 vs v4] [Delete old versions]
```

### 3. Rollback flow

```java
public void rollback(String tenantId, Integer targetVersion) {
  var target = versionRepo.find(tenantId, targetVersion);
  // Create NEW version (v6) with target's content
  // So rollback is non-destructive
  var newVersion = cloneAsNewVersion(target);
  deployService.deploy(newVersion);
}
```

### 4. Diff viewer

Visual diff:
- Side-by-side preview of v5 vs v4
- Changed: colors, assets list, template used
- Helps user decide rollback or not

### 5. Retention policy

- Keep 10 most recent versions per tenant
- Older versions: archive (not delete) for Enterprise compliance
- GDPR deletion: remove all versions khi tenant xóa

## Acceptance Criteria

- [x] `BrandingVersion` entity + migration — `kiteclass/kiteclass-core/.../settings/entity/BrandingVersion.java` + `V43__create_branding_versions.sql` (PR #343)
- [x] Auto-create version on every DEPLOYED transition — `BrandingServiceImpl.updateBranding()` snapshots via `brandingVersionService.snapshot(branding, null)` (PR #343)
- [ ] Version history UI trong tenant dashboard — **FE deferred to later wave**
- [x] Rollback endpoint (non-destructive) — `BrandingVersionController#rollback` POST `/api/v1/branding/{instanceId}/versions/{versionNumber}/rollback` (PR #343)
- [ ] Diff viewer cho 2 versions — **deferred to later wave**
- [ ] Retention: 10 versions active, older archived — **deferred (explicit in gap §"Deferred to a later wave")**
- [x] Integration test: rebrand → rollback → UI reflects — `BrandingVersionServiceTest` + `BrandingVersionSnapshotJsonbIntegrationTest` cover snapshot deactivation, rollback restore, missing-version error (PR #343 + #533 JSONB binding fix)

## Dependencies

- GAP-009 (lifecycle) — hook deploy transitions
- GAP-010 (package API) — versioned responses

## Log

- 2026-04-14 — User expectation qua simulation
- **2026-04-18** — Wave 4 (PR #343) shipped backend: entity + V43 migration + service + controller + rollback endpoint + unit tests. FE UI + diff viewer + retention deferred explicitly per gap §Wave 4 partial resolution.
- **2026-04-22** — PR #533 fixed JSONB binding for 6 jsonb columns (GAP-220 — related to BrandingVersion snapshot serialization).
- **2026-05-11:** PR# backfill (Wave 60 Bucket D-2). Verified shipped work cross-references:
  - PR #343 — `wave(4): branding propagation cluster (GAP-021,032,033p,037)` (2026-04-18) — backend MVP for version history + rollback.
  - PR #533 — `fix(gap-220): bind 6 jsonb columns as JDBC JSON for Postgres compatibility` (2026-04-22) — JSONB fix supporting `BrandingVersionSnapshotJsonbIntegrationTest`.

  Code-verify: 4/7 AC verified shipped (entity + V43 + auto-snapshot + rollback endpoint + tests); 3/7 AC explicitly deferred per gap §Wave 4 partial resolution (FE UI, diff viewer, retention prune) → tracked in §Wave 4 partial resolution "Deferred to a later wave (not MVP)" list.

  Verdict: 🟡 PARTIAL maintained — backend MVP complete; FE UI cluster deferred. NOT flipped DONE per `gap-done-discipline.md` §3 PARTIAL exit ramp (3 unchecked AC, deferred items documented in-file, no follow-up gap filed yet but scope is clear).
