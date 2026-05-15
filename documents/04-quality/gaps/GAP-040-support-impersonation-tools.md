# GAP-040: Support Impersonation & Troubleshooting Tools

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Support / Operations
**Detected:** 2026-04-14 (simulation: Support × Daily × C7)

## Problem

Khi tenant complaint "branding hiển thị sai" → support staff **không có tool** để debug:
- Không view-as-tenant
- Không xem logs per tenant
- Không có diagnostics button
- Không có ticket integration
- Support phải ask tenant screenshots → slow, frustrating

Khác với GAP-023 (admin moderation — compliance-focused) — đây là support troubleshooting.

## Proposed Fix

### 1. Impersonation Feature

```
Admin panel /admin/tenants/{id}
[View as tenant owner] — opens new browser session logged in as tenant
  - Read-only mode (cannot modify)
  - Banner "Support view: logged in as {email}"
  - Expires 30 min
  - Audit logged
```

### 2. Diagnostics Panel

```
/admin/tenants/{id}/diagnostics
├── Branding status: DEPLOYED / version 3
├── Last deploy: 2 days ago
├── Quality score: 85/100
├── Recent jobs (last 10)
├── Asset health check
│   ├── Logo URL: ✓ accessible (304ms)
│   ├── Banner URL: ✓ accessible (145ms)
│   ├── Hero URL: ⚠️ slow (1200ms)
├── Frontend integration
│   ├── Package API response time: 200ms
│   ├── CSS vars applied: ✓
│   ├── Cache hit rate: 80%
├── Recent errors (per service)
└── [Run full diagnostics]
```

### 3. Log Aggregation per Tenant

Support query logs filtered by tenant:
- Elasticsearch / Grafana Loki
- Filter: `tenant_id={id} AND service=kitehub-branding`
- Time range picker
- Export logs for analysis

### 4. Ticket Integration

```
Tenant dashboard:
[Need help?] → /support/new
  - Auto-fills: tenant ID, current branding version, last error
  - Screenshot attach
  - Creates ticket in Zendesk/Freshdesk

Support view ticket → 1-click:
  [Open tenant dashboard]
  [View diagnostics]
  [Impersonate]
  [Escalate to engineering]
```

### 5. Common Issue Playbooks

```
/support/runbooks/
├── branding-not-showing.md
├── wrong-colors.md
├── slow-preview-generation.md
├── upload-failed.md
├── regenerate-stuck.md
```

Step-by-step diagnostics per issue.

### 6. Remote Actions

Support có thể (with audit):
- Clear tenant branding cache
- Re-trigger provisioning
- Regenerate specific asset
- Force quality recheck

## Acceptance Criteria

- [ ] Impersonation feature với audit log + expiry
- [ ] Diagnostics panel showing branding health
- [ ] Log aggregation searchable by tenant
- [ ] Ticket system integration
- [ ] 5+ runbooks for common issues
- [ ] Remote actions (cache clear, retrigger)
- [ ] RBAC: SUPPORT role permissions
- [ ] SLA metrics: mean time to resolve

## Dependencies

- GAP-023 (admin moderation) — shared admin panel
- GAP-019 (monitoring) — log source
- kitehub-admin service

## Log

- **2026-05-14:** DONE — Wave 79 Bucket F-bis closure. ImpersonationService + ImpersonationAuditEntry + V48__create_impersonation_audit_log.sql + 30s TTL hard-limit + audit log ip+user_agent shipped via PR #1372. KitehubSubscriptionApplication @EntityScan extended +impersonation package.

- 2026-04-14 — Support journey uncovered
