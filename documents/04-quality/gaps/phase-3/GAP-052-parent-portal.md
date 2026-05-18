# GAP-052: Parent Portal + Accounts

**Status:** 🟡 IN_PROGRESS (Wave 2 ships identity + invitation; Wave 5 completes it)
**Priority:** 🔴 P0 (K-12 persona blocker)
**Domain:** Product / Frontend / Backend
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P5 K-12 School, P9 International School

## Delivery Split

- **Wave 2 (GAP-052a — this PR):** identity + invitation + skeleton page.
  - Parent / ParentStudentLink / ParentInvitation entities + V42 migration
  - `ParentInvitationService` (invite, redeem, expire sweeper) and `ParentService`
    (self + linked children) with tenant isolation
  - Endpoints: `POST /api/v1/parent-invitations`, `POST /api/v1/parent-invitations/redeem/{token}`,
    `GET /api/v1/parent/me`, `GET /api/v1/parent/me/children`, and internal
    `GET /internal/parents/{id}` for gateway JWT enrichment
  - Feature flag `kiteclass.parent-portal.enabled` (default false) gates the
    surface until Wave 5 lands
  - Frontend skeletons: `(dashboard)/parent` and `(auth)/parent-invite/[token]`
  - Unit tests for both services + V42 assertion in `Wave02MigrationsTest`
- **Wave 5 (GAP-052b — follow-up):** dashboard widgets (attendance, grades,
  invoices), messaging, SMS/Zalo notifications, fee payment, push
  notifications, parent self-registration, bulk import.

## Problem

K-12 education CRITICAL cần parent engagement. Hiện tại KHÔNG có:
- Parent accounts linked to students
- Parent portal (view child's progress)
- Parent notifications
- Fee payment by parent

Ở Việt Nam, phụ huynh rất active trong việc theo dõi con. Không có parent portal = trường cấp 3 không thể dùng platform.

## Proposed Fix

### 1. Parent Entity + Linking

```java
@Entity
public class Parent {
  Long id;
  String email;
  String phoneNumber;
  String fullName;
  String relationship;  // FATHER, MOTHER, GUARDIAN
  @ManyToMany Set<Student> children;
}

// 1 parent có thể có nhiều con
// 1 student có thể có nhiều parents (cha + mẹ)
```

### 2. Parent Portal UI

`{tenant}/parent/` routes:
- Dashboard: list children
- Per child:
  - Schedule / timetable
  - Grades (current + historical)
  - Attendance records
  - Fee/invoice status
  - Behavior notes (hạnh kiểm)
  - Teacher communications
- Notifications:
  - Absence alerts
  - Low grade alerts
  - Fee reminders
  - Meeting invitations
- Actions:
  - Pay fees
  - Message teacher
  - Approve/sign documents
  - Request meetings

### 3. Authentication

- Parent signup via invitation email (từ school admin)
- Link to student via code or admin approval
- SSO với popular platforms (Zalo, Google)
- Multi-children support trong 1 account

### 4. Communication Channel

- In-app messaging (parent ↔ teacher)
- Email notifications
- SMS notifications (GAP-063)
- **Zalo integration** (critical in VN)
- Push notifications (PWA)

### 5. Privacy + Permissions

- Parent sees ONLY their children's data
- Tenant isolation + parent-child relationship filter
- Explicit consent management
- GDPR/VN data protection compliant

### 6. Integration với Existing Features

- Invoice → parent receives, parent pays
- Attendance → parent notified if child absent
- Grade entered → parent notified
- Class schedule change → parent notified

## Acceptance Criteria

- [ ] Parent entity + linking to students
- [ ] Parent signup via email invitation
- [ ] Parent portal (dashboard + per-child views)
- [ ] Notifications (email, SMS, Zalo, push)
- [ ] Fee payment by parent
- [ ] In-app messaging parent ↔ teacher
- [ ] Multi-children support
- [ ] Privacy enforcement (only see own children)
- [ ] Mobile-responsive (parent thường dùng phone)
- [ ] E2E test: invite parent → view child → pay fee → message teacher

## Dependencies

- GAP-051 (bulk import parents)
- GAP-063 (SMS/Zalo integration)
- GAP-021 (branding propagation — parent emails)

## Log

- 2026-04-14 — Persona review identified
