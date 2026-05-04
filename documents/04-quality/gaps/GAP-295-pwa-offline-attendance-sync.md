# GAP-295: PWA + offline attendance sync

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (kiteclass-frontend) + Service Worker
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (mobile in basement / poor signal area), eventually all field-mobile personas

## Problem

P1 AC-EDGE-004: "App work usable offline cho mark attendance (mobile in basement / poor signal area) → sync khi có internet."

State-check `kiteclass-frontend/src` for `offline|service.?worker|sw\.js|serviceWorker` → **0 matches**. No PWA manifest, no service worker, no offline-capable storage.

Real solo tutor pain point: classroom in basement office (signal weak), park/coffee shop (intermittent), home (Wi-Fi drop) — current app fails to mark attendance offline → teacher writes on paper → re-types later → data quality drift.

## Root Cause

PWA / offline-first architecture not in v1 scope. Next.js does not auto-generate service workers; requires explicit setup (`next-pwa` package or custom).

## Proposed Fix

1. Add `next-pwa` plugin to `kiteclass-frontend/next.config.ts`.
2. Define service worker caching strategy:
   - App shell — cache-first
   - API GET (read-only) — stale-while-revalidate
   - API mutations (POST/PUT) — queued via IndexedDB (e.g., `Workbox.BackgroundSync`)
3. Implement attendance offline write: tap status → write to IndexedDB queue → optimistic UI ("✓ saved offline").
4. On network restore: replay queue to backend (`POST /api/v1/attendance/bulk`) with idempotency keys.
5. Conflict resolution: if same session attendance marked online by another device, last-write-wins with audit log entry.
6. PWA manifest + iOS/Android install prompts.
7. E2E test: airplane-mode mark → restore network → verify sync.

## Acceptance Criteria

- [ ] `next-pwa` integrated; service worker registered
- [ ] PWA manifest + install affordance
- [ ] Attendance offline-write via IndexedDB queue
- [ ] Background sync on network restore (Workbox BackgroundSync)
- [ ] Optimistic UI + conflict resolution
- [ ] E2E airplane-mode test
- [ ] AC-EDGE-004 PASS

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §5
- AC: AC-EDGE-004
- Sibling: GAP-290 (mobile UX) — share mobile-first investment

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check: 0 matches for service worker / PWA in kiteclass-frontend.
