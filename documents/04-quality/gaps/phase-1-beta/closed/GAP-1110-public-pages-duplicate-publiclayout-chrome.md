# GAP-1110: (public) pages duplicate PublicLayout chrome (header/main/footer)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-10 (G2 browser-walk `/contact?plan=enterprise` — user-flagged trùng header)
**Affects:** `kitehub-frontend/src/app/(public)/**` — contact, waitlist, trang chủ (LandingShellSSR), beta-status, legal/{terms,privacy,cookies,data-rights}

## Problem

Các trang dưới route group `(public)` được bọc bởi `(public)/layout.tsx` → `PublicLayout` (đã render site `<header>` logo+nav, MỘT `<main>`, và `<Footer />` chung). Nhưng nhiều trang lại **tự render thêm `<header>`/`<main>`/`<footer>` riêng** → trùng header/footer (lộ ở SSR / no-JS / bot) + nested `<main>` (HTML không hợp lệ).

Phát hiện qua G2 browser-walk `/contact`: 2 header chồng (`KiteHub / Bảng giá / Đăng nhập / Dùng thử miễn phí` + `KiteHub / ← Xem bảng giá`). Sweep (per `cross-flow-bug-class-sweep.md`) tìm cùng class trên `/waitlist` + trang chủ (`LandingShellSSR` trùng cả header LẪN footer) + nested `<main>` ở beta-status + 4 trang legal.

Trang chủ đặc biệt nghiêm trọng: `LandingShellSSR` (SSR fallback cho bot/SEO/no-JS per GAP-459) render header + footer riêng → bot thấy header+footer đôi. `LandingClient` (bản client) không có chrome riêng → sau hydrate còn 1 header (đúng), nhưng initial HTML / no-JS bị đôi.

## Proposed Fix (đã làm)

Bỏ chrome trùng khỏi mọi trang `(public)`, chỉ giữ nội dung; `PublicLayout` cung cấp chrome duy nhất:
- contact, waitlist: bỏ `<header>` site-level + nested `<main>`, giữ back-link dạng nội dung
- LandingShellSSR: bỏ `<header>` + `<footer>` trùng + nested `<main>`, giữ hero/features/section testimonial-trust (parity với LandingClient)
- beta-status, legal/{terms,privacy,cookies,data-rights}: `<main>` → `<div>`

## Acceptance Criteria

- [x] Không còn `<main>` JSX trong trang `(public)` (chỉ PublicLayout giữ 1 `<main>`) — verified grep
- [x] Không còn site-level `<header>`/`<footer>` trùng trong trang `(public)` (header còn lại = tiêu đề mục hợp lệ)
- [x] FE production build PASS (`pnpm --filter kitehub-frontend build` exit 0, 90/90 static pages)
- [x] CI detector `scripts/check-public-page-duplicate-chrome.sh` ship (WARN-mode, job `public-page-duplicate-chrome` trong `quality-code.yml`)
- [x] Detector self-test PASS: post-fix 0 FAIL; counterfactual pre-fix (commit 0096c5dc) flag 6 `<main>` hit / 5 file

## Related

- Fixed in: commit `7e6cc613` (fix(public): remove duplicate header/main/footer in (public) pages)
- Detector: commit ship cùng PR #2279
- Bug class detector mandate: `.claude/rules/cross-flow-bug-class-sweep.md` §4.1 (statically-detectable → persistent CI detector)
- Discovery filing: `.claude/rules/discovery-to-gap-inline-filing.md`
- Sibling feature: GAP-1101 (KH sales-lead /contact — trang bị trùng đầu tiên phát hiện)

## Log

- **2026-06-10:** Filed + DONE cùng session. G2 walk `/contact` user-flag trùng header → sweep 8 file (`cross-flow-bug-class-sweep`) → fix (commit `7e6cc613`, +126/−283) → FE build PASS → ship CI detector WARN-mode (`check-public-page-duplicate-chrome.sh` + `quality-code.yml` job). Self-test: post-fix 0 FAIL, pre-fix counterfactual 6 `<main>` hit. Integrate vào PR #2279.
