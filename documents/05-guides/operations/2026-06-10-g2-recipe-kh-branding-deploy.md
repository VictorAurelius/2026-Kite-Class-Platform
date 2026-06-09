---
title: G2 Human Test Recipe — KH AI Branding deploy (GAP-1105/1107/1108)
audience: dev
created: 2026-06-10
scope: Flow Verification Campaign G2 handoff — AI Branding deploy wizard Step 6 → SSE stream → post-deploy /branding card
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1105-branding-deploy-stream-fe-id-and-sse-disconnect.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1107-branding-provision-rollback-and-assets-parse.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1108-branding-post-deploy-empty-no-landing-link.md
---

# G2 Recipe — KH AI Branding deploy (GAP-1105/1107/1108)

> **Sản phẩm:** KiteHub (KH) — FE `kitehub-frontend` **`:3001`** (per `kitehub-kiteclass-boundary.md` §2). BE `kitehub-branding` + gateway `:9000`.

## 1. Mục tiêu

Walk wizard AI Branding Step 6 (Phê duyệt) → deploy → xác nhận:
- **GAP-1105**: SSE deploy-stream chạy sạch — progress chạy tới 100%, **KHÔNG** `STREAM_DISCONNECTED` / `Lỗi triển khai (UNKNOWN)`.
- **GAP-1108**: sau deploy 100% → redirect `/branding` hiện **deploy-success card** (state DEPLOYED + link "Xem landing" `frontendUrl` + success toast).
- **GAP-1107**: assets hiển thị (KHÔNG 0 assets); REGENERATE rollback-only không lock.

## 2. Prereq

- ⚠️ **Stack đã rebuild với code mới** (kitehub-branding + kitehub-frontend) — coordinator đã `rebuild.sh branding` + `rebuild.sh frontend` (kiểm tra `docker ps`: 2 service Up < 15 phút).
- Login Owner KH có 1 instance ở trạng thái cho phép deploy branding (đã chọn template). Dùng tài khoản Owner đã seed (vd `admin@kitehub.me` hoặc tài khoản test local của bạn).
- Thời lượng: ~10-15 phút.

## 3. Setup

- Browser + DevTools mở sẵn:
  - **Network tab** filter `deploy-stream` + `lifecycle` + `deploy-status` (xem SSE + poll).
  - **Console tab** (canh uncaught error / `STREAM_DISCONNECTED`).
- Terminal DB query (tùy chọn verify):
  ```bash
  docker exec kite-postgres psql -U kite -d kitehub -c \
    "SELECT id, status FROM branding_instance_state ORDER BY updated_at DESC LIMIT 3;"
  ```
- Verify stack mới:
  ```bash
  docker ps --format '{{.Names}} {{.Status}}' | grep -E 'kitehub-branding|kitehub-frontend'
  # kỳ vọng: Up < 15 phút (image vừa rebuild)
  ```

## 4. Các bước (browser-walk — `:3001`)

### Bước 1 — Đăng nhập + vào wizard
- **Hành động**: mở `http://localhost:3001/login` → đăng nhập Owner → vào `http://localhost:3001/branding/wizard`.
- **✅ Kỳ vọng**: wizard render, các step hiển thị; chọn template (nếu chưa) tới được Step 6 "Phê duyệt".
- **⚠️ Sad path**: redirect `/login` → chưa đăng nhập / token hết hạn → đăng nhập lại.
- **🔍 Verify**: Network — request `/api/platform/...` hoặc `/api/v1/branding/...` trả 2xx (FE tự gắn `Authorization: Bearer`).

### Bước 2 — Step 6 Phê duyệt → bấm Deploy
- **Hành động**: ở Step 6, bấm nút **Phê duyệt / Triển khai**.
- **✅ Kỳ vọng**:
  - Network: `GET /api/v1/branding/instances/{id}/deploy-stream?token=...` mở **EventSource 200** (tới gateway `:9000`, KHÔNG 404 trên `:3001`) — đây là fix GAP-1105 (absolute URL).
  - UI "Tiến trình triển khai" hiển thị progress + history các bước (KHÔNG kẹt, KHÔNG hiện jobId thay instanceId).
- **⚠️ Sad path (canh kỹ — đây là bug cũ)**:
  - `STREAM_DISCONNECTED` ngay khi connect → fix GAP-1105 chưa ăn (kiểm tra Network: URL deploy-stream phải là `:9000` không phải `:3001`).
  - **CORS error** (không phải 404): EventSource giờ cross-origin `:3001`→`:9000` với `withCredentials` → nếu lỗi CORS, báo lại (cần fix gateway CORS allow-credentials cho `:3001` route deploy-stream).
- **🔍 Verify**: Network tab — event `progress` đến liên tục, không có event `error` với data rỗng gây UNKNOWN.

### Bước 3 — Deploy chạy tới 100% + complete
- **Hành động**: chờ progress chạy.
- **✅ Kỳ vọng**: progress đạt **100%**, có **toast "Triển khai thành công…"** (fire-once), rồi redirect `/branding`.
- **⚠️ Sad path**: "Lỗi triển khai (UNKNOWN)" sau khi xong → bug GAP-1105 #4 (named-error suppression chưa ăn) — báo lại.
- **🔍 Verify**: Console không có uncaught error sau khi stream đóng sạch.

### Bước 4 — Post-deploy `/branding` card (GAP-1108)
- **Hành động**: ở trang `/branding` (sau redirect).
- **✅ Kỳ vọng**:
  - **Deploy-success card** ở đầu trang: "Trang web của bạn đã sẵn sàng 🎉" + nút/link **"Xem landing"** (`<a target=_blank href={frontendUrl}>`, vd `toan-master.kiteclass.vn`) + tóm tắt template + ngày.
  - **Assets** hiển thị (≥1, KHÔNG "0 assets") — fix GAP-1107 #2 (BrandingAsset[] shape).
- **⚠️ Sad path**: trang `/branding` rỗng / không có card / assets 0 → GAP-1108 chưa ăn → báo lại.
- **🔍 Verify**:
  ```bash
  # deploy-status endpoint trả DEPLOYED + frontendUrl
  curl -s "http://localhost:9000/api/v1/branding/instances/{id}/deploy-status" \
    -H "Authorization: Bearer <token>" | jq '{state, deployed, frontendUrl, brandingVersion}'
  ```

### Bước 5 — (tùy chọn) REGENERATE rollback (GAP-1107 #1, best-effort)
- **Hành động**: thử REGENERATE branding vài lần (≈5).
- **✅ Kỳ vọng**: KHÔNG rơi vào `REGENERATING→FAILED rollback-only` lock; `recordMarker` chạy `REQUIRES_NEW` (txn isolation) không poison caller.
- **⚠️ Sad path**: intermittent rollback-only → đây là phần **best-effort còn pending** (repro non-deterministic) — nếu gặp, **báo lại số lần / tần suất** để coordinator có repro.

## 5. Sad path checks tổng hợp
- EventSource URL: phải `:9000` (gateway), không `:3001` (Next.js 404).
- Stream đóng sạch → KHÔNG được hiện UNKNOWN.
- Deploy 100% → `/branding` PHẢI có card + landing link + assets > 0.

## 6. Báo kết quả (báo lại 1 trong 4)
- ✅ **FULL PASS** → coordinator flip GAP-1105 DONE + GAP-1107/1108 DONE (phần code), chờ G3.
- ⚠️ **MOSTLY PASS** (vd REGENERATE rollback thỉnh thoảng) → catalog GAP-1107 #1 repro.
- 🔴 **BLOCKING** (STREAM_DISCONNECTED / UNKNOWN / CORS / card rỗng / assets 0) → catalog + fix loop + re-walk.
- ❓ **UNCLEAR** → screenshot Network + Console + ping.

## 7. Troubleshooting + G3 preview
| Triệu chứng | Quick fix |
|---|---|
| deploy-stream 404 trên `:3001` | image kitehub-frontend chưa rebuild — `docker ps` check Up time |
| CORS error EventSource | gateway CORS allow-credentials cho `:3001` deploy-stream route (báo coordinator) |
| `/branding` card rỗng | image kitehub-branding chưa rebuild (deploy-status endpoint thiếu) |
| assets 0 | parseAssetsJson / MockProvisioning chưa có code mới — rebuild branding |

**G3 preview**: sau G2 PASS, production-parity walk qua gateway `:9000` với JWT mint (Host-based, HS512) — coordinator chạy, không cần bạn.
