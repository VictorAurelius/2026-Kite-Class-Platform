# Session Handoff 2026-06-08 — KC-1 G2: Option B integrate + logout fix + design-first meta

**Ngày:** 2026-06-08
**Branch:** `fix/v87-attendance-status-normalize-kc5` (committed, CHƯA push — session sau mở PR → main + fix tất cả)
**Tiếp nối:** handoff `2026-06-08-kc1-g2-walk-fixes-meta.md` (9 bug + 5 meta-rule trước đó)

---

## 1. Việc đã làm session này

### 1.1 GAP-1074 Option B — integrate xong (PARTIAL 80%)
- Agent worktree `agent-af80300f278ac070e` hoàn tất → integrate 7 file FE vào branch:
  `jwt-storage.ts` (rewrite tenant-scoped localStorage) + test (24/24 PASS) + `auth-store.ts` + `useAuth.ts` + `api-client.ts` + `student-register-form.tsx` + `(dashboard)/branding/wizard/page.tsx` (merge thủ công: giữ EXEMPT marker triage + bỏ dead `localStorage.getItem('tenantId')` fallback).
- Pattern: `kc:<tenantId>:accessToken` (cross-tab persist) + `sessionStorage kc:currentTenant` (per-tab bind) + `kc:activeTenant` (fresh-tab pointer).
- Security review code thật: isolation đúng (getter trả token+tenantId cùng namespace; clearTokens chỉ xóa tenant hiện tại). `pnpm build` PASS. FE Docker rebuilt (canonical `kitehub/scripts/rebuild.sh kiteclass-frontend` — KHÔNG dùng `dev-rebuild.sh`, xem §3 gap).
- **CHƯA browser-walk** (bước 1-2 cross-tab persist) → GAP-1074 giữ PARTIAL.

### 1.2 GAP-1075 (MỚI, PARTIAL 40%) — logout 404 contract drift
- KC-1 G2 bước 5: `POST :9000/api/auth/logout` → 404 (AuthController không có logout endpoint). Logout side-effects gate sau `onSuccess` → token không xóa, kẹt.
- **FIX INLINE:** `auth.ts` logout → client-side only (bỏ BE call); `useAuth` `onSuccess`→`onSettled` (clear luôn chạy).
- **DEFER (P1):** BE `POST /api/auth/logout` + refresh-token revocation (Redis blacklist).
- Cross-flow sweep: `forgot-password` + `reset-password` cũng drift → đã tracked GAP-803.

### 1.3 META — rule mới `design-first-investigation-order.md` v1.0.0
- User directive: "check design (architecture) TRƯỚC → gap → documents → code CUỐI cùng".
- Trigger: tôi grep code trả lời câu hỏi tenant-resolution thay vì đọc architecture docs trước → suy diễn ngược, user push 2 lần.
- Enforcement parity: rule + rules-index.csv row + memory `feedback_design_first_investigation_order.md` + MEMORY.md + output-review-mandate §3 row.

### 1.4 Làm rõ kiến trúc tenant-by-domain (áp dụng rule mới — design-first)
- Design canonical: `documents/02-architecture/tenant-domain-landing-architecture.md` — domain-per-tenant, resolve theo Host.
- **GAP-811** (FE middleware host→tenant) = fix chuẩn đa-tenant; đã ship ở **kitehub-frontend** (:3001) PARTIAL.
- **State-check finding:** GAP-813 BE endpoint (`/api/v1/public/tenants/by-subdomain/{slug}`) giờ **LIVE** (probe trả UUID Sky) → blocker "live-walk deferred" của GAP-811 đã gỡ. Verify live: `curl -H "Host: sky-education.kitehub.local" http://localhost:3001/` → render Sky branding ✅.
- KC-1 dashboard (kiteclass :3000) multi-tenant-by-domain = **Phase 2**, chưa có middleware.

---

## 2. Next session — FIX TẤT CẢ (theo thứ tự)

1. **Mở PR** `fix/v87-attendance-status-normalize-kc5` → main (nhiều commit; CI có 2 detector WARN-mode từ session trước).
2. **GAP-1074 browser-walk** (bước 1-2 cross-tab persist, 1 tenant) → flip DONE nếu PASS. Recipe: `documents/05-guides/operations/2026-06-08-g2-recipe-kc1-session-isolation.md`. Credentials: A=`owner@skyedu.vn`/`SkyEdu@2026` (e8ff87e1), B=`walk.owner+bucketb@skyedu.vn`/`SkyEdu@2026` (ba8bfdce, đã reset pw). LƯU Ý: bước 3-5 cross-tenant isolation trên localhost là dev-fallback path, KHÔNG phải production domain-path.
3. **GAP-1075 DEFER part** — BE `POST /api/auth/logout` + refresh-token revocation (Redis blacklist) + tests.
4. **GAP-811** — chạy nốt 5 AC local-walk (recipe §recipe trong handoff §1.4 + GAP-811 §AC); GAP-813 LIVE nên walk được → bump PARTIAL→DONE nếu PASS. Có thể cần `/etc/hosts` cho browser walk.
5. **KC-1 G2 còn lại** (từ handoff trước): GAP-1071 root-fix move-shell→layout SPLIT; GAP-1072 logo browser preview confirm; GAP-1073 residual sweep kitehub-frontend; GAP-1068 seed link.
6. **Flip KC-1 G2** sau khi 1074 + các bug còn lại xong.

---

## 3. Gap phát hiện thêm (chưa file — session sau file)
- **dev-rebuild path broken:** `kiteclass/scripts/dev-rebuild.sh` dùng `docker-compose.dev.yml` (Dockerfile.dev) → `pnpm install` FAIL (`ERR_PNPM_WORKSPACE_PKG_NOT_FOUND @kite/shared-ui` — build context thiếu `packages/shared-ui`). Canonical `kitehub/scripts/rebuild.sh` (production Dockerfile, context=repo root) OK. → file gap fix dev-rebuild context HOẶC deprecate dev-rebuild.sh cho FE.

## 4. Stack state
- kiteclass-frontend (:3000): rebuilt với Option B + logout fix + 7 shell page. Healthy.
- kitehub-frontend (:3001): GAP-811 middleware live, GAP-813 endpoint live. Healthy.
- kiteclass-core / kite-gateway / postgres / redis / minio / mailhog: Up healthy.
- EC2 (AWS) stopped (local-only session).

## 5. Commits session này (chưa push)
- `feat(kiteclass-fe): GAP-1074 Option B tenant-scoped localStorage + GAP-1075 logout client-side`
- `meta(rules): design-first-investigation-order v1.0.0 (check design before code)`
- `docs(g2): KC-1 session-isolation recipe + GAP-811 local-test findings`
