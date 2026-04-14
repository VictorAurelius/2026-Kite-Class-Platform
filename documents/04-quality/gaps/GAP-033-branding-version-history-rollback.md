# GAP-033: Branding Version History & Rollback (User-facing)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Product / Backend
**Detected:** 2026-04-14 (simulation: Owner × Daily Usage × C3 Data)

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

- [ ] `BrandingVersion` entity + migration
- [ ] Auto-create version on every DEPLOYED transition
- [ ] Version history UI trong tenant dashboard
- [ ] Rollback endpoint (non-destructive)
- [ ] Diff viewer cho 2 versions
- [ ] Retention: 10 versions active, older archived
- [ ] Integration test: rebrand → rollback → UI reflects

## Dependencies

- GAP-009 (lifecycle) — hook deploy transitions
- GAP-010 (package API) — versioned responses

## Log

- 2026-04-14 — User expectation qua simulation
