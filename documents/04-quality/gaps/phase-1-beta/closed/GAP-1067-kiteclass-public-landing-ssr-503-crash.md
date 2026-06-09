# GAP-1067: ERR_EMPTY_RESPONSE trên :3000 — stale docker-proxy sau compose-up (KHÔNG phải landing SSR crash)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-08 (KC-1 G2 Bước 1 — user mở `http://localhost:3000/` rồi `/login` đều báo ERR_EMPTY_RESPONSE)
**Affects:** Local dev stack port-publish (WSL2 + Docker Desktop) cho frontend container `kiteclass-frontend` (:3000) + `kitehub-frontend` (:3001); `kiteclass/kiteclass-frontend/src/app/(public)` getLandingPage (residual non-fatal)

## Problem

**Chẩn đoán ban đầu SAI** (đã sửa): tưởng `/` crash do SSR `getLandingPage` 503. Thực tế:

1. **Root cause ERR_EMPTY_RESPONSE = stale docker-proxy port-forward.** Khi rebuild kiteclass-core (`docker compose up -d kiteclass-core` trong `rebuild.sh`) → compose reconcile network → làm hỏng port-publish host→container của **cả 2 frontend** (`:3000` + `:3001`). Bằng chứng:
   - Từ TRONG container: `node http.get localhost:3000/login` → 200 (16KB), `/` → 200 (125KB) — Next serve OK.
   - Từ host (WSL): mọi route `:3000` + `:3001` → HTTP 000; Java service `:9000/:8081/:8088` → reach OK.
   - **Fix: `docker restart kiteclass-frontend kitehub-frontend`** → tất cả route 200 ngay.
2. **Residual (non-fatal):** SSR `(public)/page.tsx` + `layout.tsx` gọi `getLandingPage(tenantId)` → backend 503 → log AxiosError NHƯNG page vẫn render fallback (200, 125KB). KHÔNG gây ERR_EMPTY_RESPONSE. Đáng xem 503 source (tenant chưa cấu hình landing) nhưng không blocking.

## Proposed Fix

(1) **Ops/devops:** `rebuild.sh` (hoặc post-rebuild) nên restart sibling frontend container HOẶC cảnh báo, vì compose-up 1 service làm stale frontend port-forward trong WSL2 — sẽ tái diễn mỗi lần fix-loop rebuild service trong campaign. (2) **Residual FE:** SSR landing graceful-degrade (đã render fallback OK) + điều tra getLandingPage 503 source (P3, tách nếu cần).

## Acceptance Criteria

- [x] Root cause ERR_EMPTY_RESPONSE xác định = stale docker-proxy (không phải SSR)
- [x] Workaround verified: restart frontend container → :3000/:3001 mọi route 200
- [ ] `rebuild.sh` tự restart/cảnh báo sibling frontend port-forward sau compose-up (ngăn tái diễn trong campaign)
- [ ] (P3 residual) getLandingPage 503 source điều tra hoặc tách gap riêng

## Related

- Discovered in: KC-1 G2 Bước 1 walk 2026-06-08 (Flow Verification Campaign)
- Trigger: rebuild kiteclass-core (GAP-1066 fix) → compose-up disrupt frontend port-forward
- Recurrence risk: cao — mọi service rebuild trong G2 fix-loop sẽ lặp lại; cần ops fix

## Log (cập nhật)

- **2026-06-09:** 🟢 DONE — KC-1 G2 human browser-walk PASS (W1 — trang :3000 load 200 (không ERR_EMPTY_RESPONSE)). Code fix đã ship (PARTIAL trước đó), G2 verify trên browser thật :3000 hoàn tất per `pre-handoff-self-test-completeness.md` §3 + `g1-browser-walk-before-flip.md`. CSV canonical -> DONE; moved closed/. 
