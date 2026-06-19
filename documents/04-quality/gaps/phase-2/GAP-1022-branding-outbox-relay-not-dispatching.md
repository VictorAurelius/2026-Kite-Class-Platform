# GAP-1022: Branding outbox relay không dispatch (rows tích lũy dispatched_at=null)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-06 (KH-6 AI Branding wizard G1 walk)
**Affects:** kitehub-branding outbox relay/dispatcher

## Problem

KH-6 G1 walk: sau khi tạo branding job, outbox rows (`branding.job.queued` + `branding.lifecycle.transition`) được persist NHƯNG `dispatched_at` giữ NULL — outbox relay/dispatcher không mark dispatched. Job vẫn process thành công (QUEUED→COMPLETED ~5s) **vì fast-path direct publish** (`rabbitTemplate.convertAndSend` trong `BrandingEventEmitter`) deliver message tới consumer ngay; outbox chỉ là reliability net.

Hệ quả: outbox rows tích lũy undispatched mãi → table phình + nếu fast-path fail (broker down lúc emit) thì message mất luôn (relay không retry). Reliability net không hoạt động đúng — chỉ fast-path che.

## Root Cause

Relay/dispatcher poll `branding_outbox WHERE dispatched_at IS NULL` không chạy (scheduler không enable / không wired / @Scheduled missing). Cần verify dispatcher bean active.

## Proposed Fix

1. Verify/wire branding outbox relay (scheduled poll undispatched rows → publish → set dispatched_at). Mirror pattern subscription MigrationOutbox relay nếu có.
2. Verify khi fast-path fail, relay retry deliver từ outbox.

## Acceptance Criteria

- [ ] Sau emit, outbox row `dispatched_at` set trong N giây (relay chạy)
- [ ] Fast-path fail (broker down) → relay retry deliver khi broker up
- [ ] IT verify relay dispatch + retry

## Related

- Discovered in: KH-6 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md` (FM-5)
- Related: `design-patterns.md` §3.5.1 Outbox pattern
