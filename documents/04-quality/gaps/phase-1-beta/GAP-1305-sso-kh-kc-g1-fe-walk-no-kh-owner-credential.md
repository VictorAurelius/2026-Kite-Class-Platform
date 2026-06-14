# GAP-1305: SSO KH→KC không G1-FE-browser-walk được — thiếu KiteHub owner credential local

**Status:** 🟡 PARTIAL
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

## State-check finding (verified 2026-06-14, per `audit-to-gap-pipeline.md` §2.8)

Fix-time state-check trên live `kitehub` DB (stack UP) cho thấy gap premise **đã self-correct một phần + lộ root cause sâu hơn**:

- **Owner credential ĐÃ tồn tại** (không còn "no owner seeded"): 6+ OWNER users trong `users` table, totp tắt, login được. NHƯNG:
- **Non-determinism là blocker thực**: `resolveTenantIdForRole` (`AuthService.java:852`) emit JWT `tenantId` = instance đầu tiên (`List.findFirst()`, KHÔNG `ORDER BY`). Javadoc:838 ghi invariant "1 user → 1 tenant". `owner.test@test.vn` cố ý sở hữu **2 instance** (sky-test `22003e3c` BASIC cho flow KH-5/6/7 + skytest `aaaabbbb` FREE chứa KC data) → vi phạm invariant → `tenantId` claim non-deterministic (verified: lúc `aaaabbbb`, lúc `22003e3c` rỗng). SSO có thể land tenant rỗng.

**Fix shipped (this PR):** seed `kitehub/scripts/seed-kh-owner-sso.sql` — tạo owner SSO RIÊNG `sso.owner@skytest.test` sở hữu **duy nhất** `aaaabbbb` (single-instance → deterministic `tenantId=aaaabbbb`). Reassign KH `instances.owner_id` của aaaabbbb (KC data + G3 minted-token walk không phụ thuộc → an toàn). Side-benefit: `owner.test` còn 1 instance sky-test(22003e3c) → cũng deterministic cho flow paid-tier.

**Verified BE-contract (HTTP 200, deterministic):**
- `POST /api/auth/login` (`sso.owner@skytest.test`/`Test@1234`) → 200 + `tenantId=aaaabbbb` ✅
- `POST /api/v1/auth/sso/issue-code` → 200 + opaque code (không token-in-URL, per ADR-040) ✅
- Nút "Mở quản lý trường" render iff owner ≥1 non-deleted instance → satisfied (single-instance). ✅

## Acceptance Criteria

- [x] Có KH owner credential local (seed script HOẶC recipe step) login được `:3001`. — `sso.owner@skytest.test`/`Test@1234` via `seed-kh-owner-sso.sql`; login verified HTTP 200 + `tenantId=aaaabbbb` deterministic.
- [ ] G1-FE browser walk SSO: login KH → "Mở quản lý trường" → KC `:3000` no-relogin → role-home. Console clean + opaque code (không token-in-URL). — **PENDING human G2★** (FE browser no-relogin walk = GAP-1138 G2★; BE-contract unblock verified above; per `g1-browser-walk-before-flip.md` không claim browser-walk qua curl).
- [x] Recipe `2026-06-14-g2-recipe-sso-kh-kc.md` §2 cập nhật cách provision KH owner credential local. — §2.1 added (seed + credential table + verify commands + non-determinism caveat) + §1 prereq + §3 Bước 1.

## Related

- Discovered in: G1 FE browser walk session 2026-06-14 (`2026-06-14-g1-fe-browser-walk.md` §3.6)
- Blocks G1-FE/G2★ of: GAP-1138 (cross-product SSO KH→KC impl), STAFF-scope walk (cần session STAFF qua SSO — rbac-role-shell recipe §6)
- AC #2 follow-up: **GAP-1138 G2★** (FE no-relogin browser walk closes AC #2; GAP-1305 đã unblock + BE-verify)
- Sister boundary: `kitehub-kiteclass-boundary.md` §2 (KH `:3001` auth ≠ KC `:3000` auth)

## Log

- **2026-06-14:** PARTIAL — fix-time state-check (per `audit-to-gap-pipeline.md` §2.8) trên live DB: premise "no KH owner credential" self-corrected (owners đã tồn tại) nhưng lộ root cause sâu hơn = `resolveTenantIdForRole.findFirst()` non-deterministic cho owner đa-instance (owner.test sở hữu 2 → vi phạm invariant javadoc:838 "1 user→1 tenant"). Ship `seed-kh-owner-sso.sql` (dedicated single-instance owner `sso.owner@skytest.test` → deterministic `tenantId=aaaabbbb`; reassign aaaabbbb KH owner — KC data + G3 minted-token không ảnh hưởng) + recipe §2.1/§1/§3 update. AC #1 ✅ (login 200 + tenantId=aaaabbbb verified) + AC #3 ✅ (recipe). AC #2 (FE no-relogin browser walk) PENDING human G2★ = GAP-1138 G2★ (per `g1-browser-walk-before-flip.md` không claim browser-walk qua curl/BE-contract). completion ~85%.
