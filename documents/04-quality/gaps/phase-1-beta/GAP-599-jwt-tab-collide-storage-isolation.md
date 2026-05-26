# GAP-599: JWT storage key collision khi mở 2 browser tab cùng domain

**Status:** 🟡 PARTIAL 85% — Wave 92 Bucket B PR #1515 — `sessionStorage` facade `jwt-storage.ts` ship + 7 production sites migrated + 17 unit tests + 3 two-tab simulation tests PASS (jsdom isolated). Live multi-tab browser UX verify pending GAP-612 AWS restore per `pre-handoff-self-test-completeness.md` §2.7. Status sync with `gap-status.csv` canonical per `gap-architecture-v2.md` §3 (file Status field was stale `🔵 OPEN`; updated via Wave audit-stale-sweep-1 2026-05-26).
**Priority:** 🔴 P0
**Domain:** Frontend
**Phase:** phase-1-beta
**Found:** 2026-05-17 (Wave 87 outside-in audit #3 — failure-mode matrix)
**Affects:** Mọi multi-actor walkthrough (admin↔owner, owner↔teacher, owner↔parent) → flaky self-test + flaky production dev sessions

## Problem

Khi developer (hoặc tester) mở 2 tab trên cùng domain `kitehub.me` cùng lúc để chạy multi-actor walkthrough:

1. Tab A: đăng nhập với `admin@kitehub.me` (role `PLATFORM_ADMIN`) → FE lưu JWT vào `localStorage['accessToken']`
2. Tab B: đăng nhập với P2 tenant owner `hang@sky-education.vn` → FE ghi đè `localStorage['accessToken']` của Tab A

Hệ quả: Tab A submit form sau khi Tab B login → request mang JWT của Tab B → backend trả `403 Forbidden` (P2 không có quyền admin) hoặc tệ hơn — cross-tenant data leak nếu role overlap.

Mọi walkthrough multi-actor (đa số acceptance test flows trong `phase-1-beta-acceptance-self-test.csv`) đều flaky theo cách này khi dev tự nhiên mở 2 tab thay vì 2 browser profile.

## Root Cause

FE auth storage scheme dùng **single key `accessToken` trong `localStorage`** — shared across mọi tab cùng origin (browser invariant). Không có:

- Per-tab isolation (`sessionStorage` thay `localStorage`)
- Per-tenant key namespacing (`accessToken:tenant-{slug}` hoặc `accessToken:user-{email-hash}`)
- Tab-ID prefix (`accessToken:{tabId}` với `tabId` sinh tại tab init)

**State-check evidence** (2026-05-17):

```
$ grep -rnE "localStorage.*[Tt]oken|accessToken" kitehub/kitehub-frontend/src 2>/dev/null
kitehub/kitehub-frontend/src/components/layout/AdminLayout.tsx:29:    localStorage.removeItem('accessToken');
kitehub/kitehub-frontend/src/components/layout/AdminLayout.tsx:30:    localStorage.removeItem('refreshToken');
kitehub/kitehub-frontend/src/components/layout/DashboardLayout.tsx:14:    localStorage.removeItem('accessToken');
kitehub/kitehub-frontend/src/components/layout/DashboardLayout.tsx:15:    localStorage.removeItem('refreshToken');
kitehub/kitehub-frontend/src/lib/api/client.ts:15:      const accessToken = localStorage.getItem('accessToken');
```

→ `localStorage` single-key scheme confirmed. Zero hits cho `sessionStorage` token / tenant-scoped key.

## Proposed Fix

**Option A — sessionStorage thay localStorage** (preferred, low blast radius):
- Migrate `localStorage.setItem('accessToken', ...)` → `sessionStorage.setItem('accessToken', ...)` trong `lib/api/client.ts` + login flow + logout cleanup
- `sessionStorage` per-tab native (browser invariant) → JWT collision không thể xảy ra giữa tabs
- Trade-off: tab close → re-login required (không persist như localStorage). Acceptable cho Phase 1 BETA (cohort nhỏ, dev/tester scope).

**Option B — scoped key per tenant** (heavier refactor):
- Key pattern `accessToken:{tenant-slug}` hoặc `accessToken:{user-email-hash}`
- API client phải pick correct key per request (cần URL/header context để biết tenant active)
- Phức tạp hơn, defer Phase 1.5+ nếu Option A đủ.

**Option C — Wave 87 mitigation (docs-only, ship same-PR)**:
- README acceptance-tests note: "Dùng 2 browser profiles riêng (Chrome profile khác nhau hoặc Chrome + Firefox) cho multi-actor walkthrough — KHÔNG mở 2 tab cùng domain"
- Wave 87 Bucket E ship mitigation; code fix defer Wave 88+.

## Acceptance Criteria

- [ ] Mở admin tab A + tenant owner tab B trên cùng browser → JWT KHÔNG collide; mỗi tab giữ JWT riêng
- [ ] Switch tab A → B → A: API request từ mỗi tab dùng đúng JWT của actor đó (kiểm verify via DevTools Network tab → `Authorization` header)
- [ ] Logout tab A KHÔNG ảnh hưởng tab B (mỗi tab logout độc lập)
- [ ] Cross-tenant data leak test: tab A xem dashboard admin, tab B xem dashboard owner → KHÔNG có request nào trả data sai tenant context
- [ ] FE storage scheme documented trong `documents/02-architecture/frontend/auth-storage.md` (hoặc tương đương)
- [ ] `documents/05-guides/operations/acceptance-tests/README.md` § "Concurrent browser session" mitigation note shipped (Wave 87 Bucket E — partial)

## Related

- Audit: Wave 87 outside-in audit #3 (failure-mode matrix) — `documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md` §3 Bucket E
- Sister: GAP-518 (admin role-guard P0), GAP-519 (admin nav P0), GAP-525 (invite E2E P0) — multi-actor flows phụ thuộc tab isolation
- Rule applied: `.claude/rules/pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch checklist (a)+(c)+(d) — data isolation + cache invalidation
- Mitigation companion: `documents/05-guides/operations/acceptance-tests/README.md` § Concurrent browser session

## Log

- **2026-05-17:** Gap filed Wave 87 Bucket E. Outside-in audit #3 failure-mode matrix phát hiện class này khi simulate "dev mở 2 tab quick switch". State-check confirmed single-key `localStorage['accessToken']` scheme — không có per-tab/per-tenant isolation. P0 vì chặn acceptance walkthrough multi-actor (đa số flows trong matrix 126 rows). Wave 87 ship docs mitigation; code fix Option A defer Wave 88+.
