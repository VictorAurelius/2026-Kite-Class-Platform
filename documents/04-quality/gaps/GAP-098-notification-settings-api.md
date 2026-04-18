# GAP-098: Notification Settings API Not Implemented

**Status:** 🟢 DONE (2026-04-18, PR #354)
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

- [x] Migration V18 adds `email_notifications` + `trial_reminders` columns (2 boolean columns instead of JSONB)
- [x] PATCH endpoint persists changes (reuses existing `PATCH /api/platform/instances/{id}` với optional Boolean fields)
- [x] Reload preserves user selections (Frontend loads from `instance.emailNotifications` / `instance.trialReminders`)
- [ ] Email dispatcher respects preferences (skip send if channel disabled) — **deferred**: hook into EmailAdminService trong follow-up PR since email dispatch is in different service
- [x] Unit tests: 2 new tests trong InstanceServiceTest (persist + null-preserves-existing), 262 total pass

## Dependencies

- Reuses GAP-096 email admin toggle pattern
- Small scope — 1-2 days

## Related

- `.claude/skills/quality/business-logic-audit/SKILL.md` pattern for testing
