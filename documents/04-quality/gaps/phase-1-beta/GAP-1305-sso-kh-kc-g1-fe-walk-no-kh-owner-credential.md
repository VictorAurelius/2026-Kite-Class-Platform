# GAP-1305: SSO KH→KC không G1-FE-browser-walk được — thiếu KiteHub owner credential local

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps (test-infra) / Frontend
**Found:** 2026-06-14 (G1 FE browser walk RBAC+LMS — `documents/04-quality/audits/rst-html/2026-06-14-g1-fe-browser-walk.md`)
**Affects:** GAP-1138 (cross-product SSO KH→KC) — block flip ngay cả ở tầng G1-FE; recipe `2026-06-14-g2-recipe-sso-kh-kc.md`

## Problem

Trong G1 FE browser walk 2026-06-14, luồng SSO KH→KC (Flow KC-2/GAP-1138) KHÔNG drive được qua browser thật vì:

- SSO bắt đầu bằng login **KiteHub `:3001`** → click nút "Mở quản lý trường" (`OpenSchoolManagementButton`) → redirect KC `:3000/sso/callback?code=...`.
- KiteHub login dùng **kitehub-subscription auth** (`/api/auth/login`), TÁCH BIỆT với KiteClass tenant-auth (`/api/v1/tenant-auth/login`).
- **Không có KiteHub owner credential nào seeded local** → `:3001/dashboard` đá thẳng `/login` (KH), nút "Mở quản lý trường" không render → walk dừng ngay bước 1.

Evidence: `sso-kh-dashboard.png` (KH `:3001/dashboard` → `/login`); core/gateway logs xác nhận không có KH session.

Đây là **test-infra/credential gap**, KHÔNG khẳng định SSO BE bị lỗi (BE-contract walk 2026-06-14 đã verify issue→exchange→replay-401→CSRF-415 qua minted JWT). Vấn đề: tầng browser-real (G1-FE) + human G2★ cần KH owner account để walk luồng UI thật "Mở quản lý trường → KC no-relogin".

## Proposed Fix

Seed 1 KiteHub owner credential local (kitehub-subscription auth) gắn với tenant skytest (`aaaabbbb-...`) — tương tự pattern `kitehub/scripts/seed-toan10a1-demo.sql` đã seed KC creds. Hoặc document recipe lấy/ tạo KH owner account cho local SSO walk trong `2026-06-14-g2-recipe-sso-kh-kc.md` §2 Setup. Sau đó re-walk SSO G1-FE + bàn giao G2★.

## Acceptance Criteria

- [ ] Có KH owner credential local (seed script HOẶC recipe step) login được `:3001`.
- [ ] G1-FE browser walk SSO: login KH → "Mở quản lý trường" → KC `:3000` no-relogin → role-home. Console clean + opaque code (không token-in-URL).
- [ ] Recipe `2026-06-14-g2-recipe-sso-kh-kc.md` §2 cập nhật cách provision KH owner credential local.

## Related

- Discovered in: G1 FE browser walk session 2026-06-14 (`2026-06-14-g1-fe-browser-walk.md` §3.6)
- Blocks G1-FE/G2★ of: GAP-1138 (cross-product SSO KH→KC impl), STAFF-scope walk (cần session STAFF qua SSO — rbac-role-shell recipe §6)
- Sister boundary: `kitehub-kiteclass-boundary.md` §2 (KH `:3001` auth ≠ KC `:3000` auth)
