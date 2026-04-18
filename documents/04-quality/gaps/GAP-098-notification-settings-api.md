# GAP-098: Notification Settings API Not Implemented

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** KiteHub Frontend / API
**Found:** 2026-04-18 (TODO audit post Wave 4)
**Affects:** Customer settings page — notification preferences UI exists but no persistence

## Problem

`kitehub/kitehub-frontend/src/app/(customer)/settings/components/InstanceTab.tsx:57` has `// TODO: Implement notification settings API call`. UI allows toggling email/SMS/push preferences but changes are lost on page reload — no backend persistence.

## Evidence

```tsx
const handleNotificationChange = (channel: string, enabled: boolean) => {
  // TODO: Implement notification settings API call
  setNotificationPrefs(prev => ({ ...prev, [channel]: enabled }));
};
```

## Root Cause

Feature was scaffolded but backend never wired up. No endpoint, no DB column, no DTO.

## Proposed Fix

1. Add `notification_preferences` JSONB column to `instances` table (migration)
2. Backend endpoint `PATCH /api/v1/instances/{id}/notifications` accepting `{email, sms, push}` booleans
3. Frontend `useNotificationPreferences` hook with optimistic update
4. Wire to existing email dispatch (GAP-096 EmailAdminService already has toggles — reuse)

## Acceptance Criteria

- [ ] Migration adds `notification_preferences` column
- [ ] PATCH endpoint persists changes
- [ ] Reload preserves user selections
- [ ] Email dispatcher respects preferences (skip send if channel disabled)
- [ ] Unit tests + integration test

## Dependencies

- Reuses GAP-096 email admin toggle pattern
- Small scope — 1-2 days

## Related

- `.claude/skills/quality/business-logic-audit/SKILL.md` pattern for testing
