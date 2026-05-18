# GAP-072: Scheduled Rebrand + Academic-Year-Tied Branding Refresh

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** AI Branding / Scheduling / Product
**Detected:** 2026-04-14 (simulation-gap-finder on Wave 3 scope)
**Matrix cell:** Owner × Evolution × C10 Evolution

## Problem

Tenant không thể schedule branding refresh cho events biết trước:

- **New academic year** (Sep 5): trường muốn banner/hero đổi sang "Chào năm học 2026-2027" trước ngày khai giảng mà không thức khuya click rebrand
- **Tết**: seasonal branding cho Feb
- **Summer camp**: seasonal center landing pages
- **School anniversary**: thay banner 1 tuần trước ngày kỷ niệm

Hiện tại rebrand chỉ manual trigger. Admin phải nhớ + click đúng ngày.

## Evidence

- `InstanceLifecycleService.rebrand()` synchronous trigger
- Không có `ScheduledRebrand` entity
- AcademicYear entity (Wave 2 GAP-053) có startDate nhưng không hook vào branding pipeline
- Holiday table có dates nhưng không trigger branding event

## Proposed Fix

### 1. ScheduledRebrand entity

```java
@Entity
class ScheduledRebrand {
  Long id;
  Long instanceId;
  LocalDateTime scheduledAt;
  String preset;                // Optional preset (academic-year / tet / summer / custom)
  Map<String, String> overrides; // Wizard field overrides
  ScheduledRebrandStatus status; // PENDING / EXECUTED / CANCELLED / FAILED
  Long createdBy;
  String reason;                 // Audit
}
```

### 2. Trigger variants

- **One-off**: scheduledAt datetime
- **Recurring**: e.g., every Sep 1 tied to AcademicYear.startDate
- **Event-driven**: subscribe to `academic-year.started`, `holiday.upcoming`, `subscription.anniversary`

### 3. Scheduler

Spring `@Scheduled(fixedRate=60_000)` job scans `ScheduledRebrand WHERE status=PENDING AND scheduledAt <= now()` → enqueue rebrand via existing saga (Wave 3 GAP-008 workflow).

### 4. Preset library for common schedules

```
academic-year:
  trigger: AcademicYear.startDate - 7 days
  changes: hero banner → "Năm học {year}", landing CTA → "Ghi danh học năm mới"

tet-greeting:
  trigger: lunar Feb 1 - 14 days
  changes: palette → red/gold, banner → "Chúc mừng năm mới {year}"

summer-camp:
  trigger: Jun 1
  changes: landing → summer camp promo, hero → outdoor imagery
```

### 5. UI

Admin dashboard `/branding/schedule`:
- Timeline view (calendar)
- Create scheduled rebrand form
- Preview preset changes
- Cancel upcoming
- History of executed schedules

## Acceptance Criteria

- [ ] `scheduled_rebrands` table + CRUD API + migration
- [ ] `@Scheduled` dispatcher job (1-minute cadence)
- [ ] 3 seed presets: academic-year, tet-greeting, summer-camp
- [ ] Integration: AcademicYear.startDate creates default academic-year schedule
- [ ] UI schedule list + timeline + cancel
- [ ] Notification 24h before execution (email + in-app)
- [ ] Dry-run preview endpoint (shows changes without committing)
- [ ] E2E: create schedule → mock clock forward → rebrand executes

## Dependencies

- Wave 2 GAP-053 (academic year) — DONE, provides dates
- Wave 3 GAP-008 (agent workflow) — executor
- Wave 3 Outbox (3.1) — event publishing

## Target Wave

**Wave 8 Admin & Support** (Sprint 6+) — ops-oriented feature.

Does NOT block Wave 3.

## Log

- 2026-04-14 — Detected via simulation-gap-finder (evolution stage, temporal triggering missing)
