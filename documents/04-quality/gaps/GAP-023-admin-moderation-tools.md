# GAP-023: Admin Moderation Tools for AI Branding

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Admin / Compliance / Backend
**Detected:** 2026-04-14 (simulation)

## Problem

Platform admin **không có công cụ** để giám sát và moderate tenant brandings:

- ❌ Không admin UI review tenant brandings
- ❌ Không flag inappropriate content workflow
- ❌ Không override branding (for compliance issues)
- ❌ Không tenant branding audit log for admins
- ❌ Không bulk actions (mass deprecate, mass approve)

**Risk:** Inappropriate content deployed → admin không biết → legal issue.

## Proposed Fix

### 1. Admin Dashboard — Branding Review

```
/admin/branding/
├── List all tenant instances
│   - Filter: status, tier, created date, flagged
│   - Sort: last updated, quality score
├── Tenant detail
│   - View current branding (preview)
│   - View history (all branding versions)
│   - Quality scores over time
│   - Flag/report buttons
│   - Override actions
└── Moderation queue
    - Flagged items awaiting review
    - Automated safety failures
    - User-reported issues
```

### 2. Admin Actions

```java
@RestController
@RequestMapping("/admin/branding")
@RequireRole("PLATFORM_ADMIN")
public class AdminBrandingController {

  @PostMapping("/{instanceId}/flag")
  public void flag(@PathVariable String instanceId, @RequestBody FlagReason reason);

  @PostMapping("/{instanceId}/force-rebrand")
  public void forceRebrand(@PathVariable String instanceId);

  @PostMapping("/{instanceId}/take-down")
  public void takeDown(@PathVariable String instanceId, @RequestBody TakedownReason reason);

  @GetMapping("/{instanceId}/audit-log")
  public List<AuditEntry> getAuditLog(@PathVariable String instanceId);

  @PostMapping("/bulk/flag")
  public void bulkFlag(@RequestBody List<String> instanceIds, @RequestBody FlagReason reason);
}
```

### 3. User-report Flow

Tenant/end-user có thể report inappropriate content:
- Button "Report" trên branding preview
- Report category: Inappropriate / Copyright / Spam / Other
- Goes to admin moderation queue

### 4. Automated Flagging

Integrate với GAP-018 content safety:
- Score >threshold → auto-flag
- Multiple regenerations from same tenant → suspicious
- Keyword blocklist

### 5. Audit Trail

All admin actions logged:
- Who did what, when, to which tenant
- Reason required for sensitive actions (take-down)
- Immutable log (append-only)

## Acceptance Criteria

- [ ] Admin dashboard UI cho branding review
- [ ] 5+ admin action endpoints (flag, force-rebrand, take-down, etc.)
- [ ] Moderation queue with filter/sort
- [ ] User-report flow
- [ ] Audit log table + UI
- [ ] Automated flagging integration
- [ ] Role-based access control (PLATFORM_ADMIN only)
- [ ] Email notification khi tenant bị flagged/taken down

## Dependencies

- GAP-018 (content safety) — automated flagging source
- kitehub-admin service (admin dashboard host)

## Log

- 2026-04-14 — Compliance gap phát hiện qua admin journey simulation
