# GAP-035: Wizard Team Collaboration (Multi-user Edit)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (enterprise feature)
**Domain:** Frontend / Backend
**Detected:** 2026-04-14 (simulation: Owner × Configuration × C2 UX)

## Problem

Enterprise tenant có team (marketing manager, designer, owner) cần **collaborate on branding**:
- Manager upload logo
- Designer choose template + colors
- Owner approve final

Hiện tại wizard là single-user only → phải screenshot share, không realtime.

## Proposed Fix

### 1. Shared draft

```java
@Entity
public class BrandingDraftCollaborator {
  Long id;
  String draftId;
  String userId;
  Role role;  // OWNER, EDITOR, VIEWER
  Timestamp invitedAt;
}
```

### 2. Share link

Owner tạo wizard draft → invite:
```
[Share wizard] → generate link: /branding/wizard?draft=abc123
Recipients: team@example.com
Permission: EDITOR | VIEWER
Expiry: 7 days
```

### 3. Realtime sync (optional)

Lightweight CRDT or polling:
- Every 5s sync state
- Show "John is editing Step 3" indicator
- Last-write-wins cho simple fields
- Lock for preview generation (expensive)

### 4. Comments / feedback

```tsx
<FormField label="Tone">
  <RadioGroup ... />
  <CommentsThread fieldId="tone">
    - Designer: "Đề nghị chọn professional"
    - Owner: "OK, approve"
  </CommentsThread>
</FormField>
```

### 5. Approval workflow

Enterprise có approval gate:
```
EDITOR completes wizard → REQUEST APPROVAL
  ↓
OWNER review → [Approve] [Request changes]
  ↓ approved
Deploy
```

## Acceptance Criteria

- [ ] Share wizard via link (with permission)
- [ ] 3 roles: OWNER, EDITOR, VIEWER
- [ ] Polling sync every 5s (basic) — later upgrade WebSocket
- [ ] Comments per field
- [ ] Approval workflow cho Enterprise
- [ ] Audit: who changed what
- [ ] E2E test: 2 users simultaneously editing

## Dependencies

- GAP-013 (wizard UX) — base to extend
- GAP-020 (state persistence) — shared draft state

## Log

- 2026-04-14 — Enterprise collaboration scenario
