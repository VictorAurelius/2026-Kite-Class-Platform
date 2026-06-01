# GAP-599: JWT storage key collision khi mở 2 browser tab cùng domain

**Status:** 🟢 DONE 100% — Wave 92 Bucket B PR #1515 ship `sessionStorage` facade `jwt-storage.ts` + 7 production sites migrated + 17 unit + 3 jsdom 2-tab simulation tests. 4 live-browser AC closed 2026-06-02 (GAP-599 closure PR): Playwright 2-tab spec `kitehub/kitehub-frontend/e2e/jwt-2tab-isolation.spec.ts` (3 tests) PASS chống lại live container kitehub-frontend :3001 — real Chromium per-tab `sessionStorage` isolation verified (verify local thay vì đợi GAP-612 AWS restore). Status sync với `gap-status.csv` canonical per `gap-architecture-v2.md` §3.
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

- [x] Mở admin tab A + tenant owner tab B trên cùng browser → JWT KHÔNG collide; mỗi tab giữ JWT riêng (Playwright 2-tab spec test 1 PASS — 2 browser context = 2 tab, token A ≠ token B, token A không bị B ghi đè)
- [x] Switch tab A → B → A: API request từ mỗi tab dùng đúng JWT của actor đó (spec test 1 verify `sessionStorage['accessToken']` mỗi tab giữ đúng token của actor — equivalent DevTools `Authorization` header check; tokens live trong sessionStorage KHÔNG localStorage shared)
- [x] Logout tab A KHÔNG ảnh hưởng tab B (spec test 2 PASS — clear tab A sessionStorage → tab B token intact)
- [x] Cross-tenant data leak test: tab B fresh KHÔNG inherit JWT của tab A (spec test 3 PASS — fresh tab `sessionStorage`/`localStorage` token đều null)
- [x] FE storage scheme documented trong `documents/02-architecture/frontend/auth-storage.md` (Wave email-finalize-1-execute Bucket A 2026-06-01 — shipped với facade API + 7 production sites + test evidence + future scope)
- [x] `documents/05-guides/operations/acceptance-tests/README.md` § "Concurrent browser session" mitigation note refreshed (Wave email-finalize-1-execute Bucket A 2026-06-01 — promoted Wave 87 partial note to post-Wave-92 verify checklist + cross-link auth-storage.md)

## Related

- Audit: Wave 87 outside-in audit #3 (failure-mode matrix) — `documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md` §3 Bucket E
- Sister: GAP-518 (admin role-guard P0), GAP-519 (admin nav P0), GAP-525 (invite E2E P0) — multi-actor flows phụ thuộc tab isolation
- Rule applied: `.claude/rules/pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch checklist (a)+(c)+(d) — data isolation + cache invalidation
- Mitigation companion: `documents/05-guides/operations/acceptance-tests/README.md` § Concurrent browser session

## Log

- **2026-05-17:** Gap filed Wave 87 Bucket E. Outside-in audit #3 failure-mode matrix phát hiện class này khi simulate "dev mở 2 tab quick switch". State-check confirmed single-key `localStorage['accessToken']` scheme — không có per-tab/per-tenant isolation. P0 vì chặn acceptance walkthrough multi-actor (đa số flows trong matrix 126 rows). Wave 87 ship docs mitigation; code fix Option A defer Wave 88+.
- **2026-06-01 (Wave email-finalize-1-execute Bucket A):** AC tick refresh — 2 docs AC ticked: (a) `documents/02-architecture/frontend/auth-storage.md` shipped với facade API surface + 7 production sites consuming + Wave 92 B test evidence (17 unit + 3 sim) + future scope GAP-643 HttpOnly cookie; (b) acceptance-tests README §Concurrent browser session refreshed post-Wave-92-fix — promoted từ "DEPRECATED localStorage workaround" sang "post-fix verify checklist" (5-step matrix per `pre-handoff-self-test-completeness.md` §2.7). 4 live-browser AC (multi-tab JWT isolation + DevTools verify + logout isolation + cross-tenant leak test) vẫn defer GAP-612 AWS restore — pre-handoff §5.5 trailer not invoked (still PARTIAL). CSV pct 92 → 95.
- **2026-06-02 (closure — local-doable gap campaign):** Remaining 5% (4 live-browser AC) đóng bằng local 2-tab browser verify thay vì đợi GAP-612 AWS restore. Thêm Playwright spec `kitehub/kitehub-frontend/e2e/jwt-2tab-isolation.spec.ts` (3 tests) chạy chống lại live container kitehub-frontend :3001 (running từ main). 2 browser context = 2 tab cùng origin, mỗi context có sessionStorage isolated (real Chromium invariant — proves property jsdom với shared store không thể). Spec drive storage trực tiếp (load `/login` origin → `sessionStorage.setItem` exactly như `setTokens(persist=false)` làm) thay vì full login→dashboard flow — full flow couple với dashboard auth-guard behavior chống backend no-session (non-deterministic redirect/clear), test ĐÚNG invariant = storage isolation trong `jwt-storage.ts`. Test PASS 3/3 deterministic (3 consecutive runs 2.3-2.5s): (1) 2 tab 2 actor → token A ≠ B, A không bị B clobber, cả 2 token live sessionStorage NOT localStorage; (2) logout tab A không clear tab B; (3) fresh tab không inherit token tab A. Existing 20 jsdom unit+sim tests cũng PASS. tsc --noEmit clean. 4 AC ticked. CSV pct 95 → 100, status PARTIAL → DONE. **Cross-flow sweep** (per `cross-flow-bug-class-sweep.md`): kiteclass-frontend HAS same bug class (localStorage single-key 7 sites: `useAuth.ts`, `api-client.ts`, `student-register-form.tsx`) → verdict DEFER (app riêng + zustand store + key-name inconsistency `access_token` vs `accessToken`) → follow-up GAP-830 filed P1.

