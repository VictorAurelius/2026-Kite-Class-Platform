---
title: G2 Human Test Recipe — KH-5 Subscription lifecycle (downgrade / cancel / renew)
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff for KH-5 subscription lifecycle (Owner downgrade/cancel + renew BE-only)
product: KiteHub (KH) — FE kitehub-frontend port :3001 (per kitehub-kiteclass-boundary.md §2)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh5-subscription-lifecycle.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md
  - .claude/rules/kitehub-kiteclass-boundary.md
  - .claude/rules/g1-browser-walk-before-flip.md
---

# G2 Human Test Recipe — KH-5 Subscription lifecycle

> **Sản phẩm:** KiteHub (KH) — quản lý vòng đời subscription SaaS. FE = `kitehub-frontend` chạy port **`:3001`** (KHÔNG phải `:3000` của KiteClass). Backend = `kitehub-subscription` qua gateway `:9000`. (Per `kitehub-kiteclass-boundary.md` §2.)

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (dev) tự xác nhận Owner quản lý vòng đời subscription chạy thật trên local stack:

- **2 thao tác CÓ FE button → test bằng BROWSER thật trên `:3001`:**
  - **Downgrade (hạ gói)** — `/billing` → nút "Nâng cấp" → TierSelector chọn gói thấp hơn → ChangeConfirmation.
  - **Cancel (hủy đăng ký)** — `/settings` → tab "Nguy hiểm" → DangerZone nút "Hủy đăng ký".
- **1 thao tác BE-only → test bằng CURL qua gateway `:9000`:**
  - **Renew (gia hạn)** — KHÔNG có owner-facing FE (per `SubscriptionController.java:53` ghi rõ "operational/renewal-reminder view with no owner-facing FE"). Đây là vận hành nội bộ, test bằng `curl`.

Trọng tâm: (1) xác nhận FE tự gắn `Authorization: Bearer` + tự gọi đúng endpoint gateway (KHÔNG gắn header tay); (2) xác nhận 2 inline fix (FM-2 NPE→400, FM-5 downgrade corruption→400) PASS; (3) nhận diện 4 known-issue (GAP-1015..1018) KHÔNG báo nhầm thành bug mới.

**Prereq:**
- Local Docker stack UP: FE `kitehub-frontend :3001` + gateway `:9000` + `kitehub-subscription` + `kite-postgres` healthy. Check:
  - `docker ps | grep -E 'kitehub-frontend|kite-gateway|kitehub-subscription'` → `healthy`/`Up`.
  - Mở `http://localhost:3001` trên browser → trang load (KHÔNG `ERR_EMPTY_RESPONSE`; nếu lỗi xem §6 — stale docker-proxy GAP-1067 class).
- Code Wave flow-kh5 đã ship: 2 inline fix (FM-2 + FM-5) + rebuild `kitehub-subscription`.
- Seeded data: Owner `owner@skyedu.vn / SkyEdu@2026` (subdomain `sky-education`), subscription `3ea90672-44d3-4ee9-9859-fd312482b429` ở trạng thái `BASIC` / `ACTIVE`.

**Thời lượng:** ~12-18 phút (browser-walk 2 thao tác + curl supplement BE-only).

**Lưu ý quan trọng — thứ tự + state:** Browser-walk **thay đổi state thật** của subscription seeded. Walk theo thứ tự: **Bước 1 Downgrade** (sub vẫn `ACTIVE`, set `pending_tier=FREE`) → **Bước 2 Cancel** (sub → hủy cuối kỳ). Sau khi Cancel, §4 curl supplement tận dụng đúng state này (renew sub đã hủy → sad-path 400; double-cancel → idempotent). Renew happy-path (204) cần sub còn `ACTIVE` → chạy **trước** Bước 2 Cancel hoặc **restore** sub (xem §6 Troubleshooting). Cuối walk muốn re-test thì restore sub về `BASIC`/`ACTIVE`.

## 2. Setup

### 2.1 Browser + DevTools (cho Bước 1-2 browser-walk)

- Mở **Chrome/Edge** → `http://localhost:3001`.
- Mở **DevTools** (F12) → tab **Network** → filter `Fetch/XHR` → tick **Preserve log** (giữ request qua redirect).
- Mở tab **Console** song song → theo dõi uncaught error / failed-to-fetch.
- Đăng nhập: nếu chưa login, browser redirect `/login` → nhập `owner@skyedu.vn` / `SkyEdu@2026` → "Đăng nhập" → redirect vào trang chủ Owner.

### 2.2 Terminal (cho §4 curl supplement BE-only + verify DB)

```bash
GW=http://localhost:9000

# Đăng nhập Owner qua gateway → lấy accessToken (chỉ dùng cho §4 BE-only renew/IDOR)
TOKEN=$(curl -s -X POST "$GW/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner@skyedu.vn","password":"SkyEdu@2026"}' | jq -r '.accessToken')
echo "TOKEN length: ${#TOKEN}"   # > 100 nghĩa là login OK

# Subscription seeded (BASIC/ACTIVE)
SUB=3ea90672-44d3-4ee9-9859-fd312482b429   # owner@skyedu.vn / sky-education (BASIC/ACTIVE) — verified DB 2026-06-09
AUTH=(-H "Authorization: Bearer $TOKEN")
```

Base path subscription: `/api/platform/subscriptions`. Verify DB:
`docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT id, tier, pending_tier, status, auto_renew FROM subscriptions WHERE id='$SUB';"`

## 3. Browser-walk (thao tác CÓ FE) — `:3001`

### Bước 1 — Downgrade (hạ gói BASIC → FREE) — browser `:3001`

**Hành động:**
1. Vào `http://localhost:3001/billing` (hoặc menu "Hóa đơn & Thanh toán"). Trang hiển thị **CurrentPlanCard** (gói hiện tại = `BASIC`) + nút **"Nâng cấp"** ở góc phải header.
2. Click **"Nâng cấp"** → điều hướng `/billing/upgrade` → wizard **StepIndicator** bước 1 → **TierSelector**.
3. Trong TierSelector, chọn gói **THẤP hơn BASIC = `FREE`** → wizard sang bước 2.
4. **ChangeConfirmation** hiển thị tier mới `FREE` → click **xác nhận** (nút confirm trong panel).

**✅ Kỳ vọng (PASS):**
- DevTools **Network**: request `PATCH http://localhost:9000/api/platform/subscriptions/3ea90672-44d3-4ee9-9859-fd312482b429/downgrade` → **HTTP 200**, request body `{"newTier":"FREE"}`.
- **Request Headers** có `Authorization: Bearer eyJ...` do **FE tự gắn** (qua apiClient interceptor) — bạn KHÔNG nhập header tay.
- FE: toast **"Đã lên lịch hạ gói. Thay đổi sẽ có hiệu lực cuối kỳ thanh toán."** → redirect `/billing?success=downgrade` → toast **"Đã lên lịch hạ gói thành công!"**.
- Console clean (không uncaught error).

**⚠️ Sad path (FM-5 đã FIX inline — phải PASS):** Downgrade khi đang có pending upgrade payment → BE reject **HTTP 400** (guard `pendingPaymentId != null` tránh ghi đè pending upgrade làm hỏng tier). Để tái hiện: trước tiên thực hiện **upgrade** (BASIC → PREMIUM) tạo `pendingPaymentId`, rồi quay lại downgrade → kỳ vọng FE hiển thị lỗi (toast đỏ) + Network `PATCH /downgrade` → **400**, `pending_tier` GIỮ NGUYÊN gói upgrade (KHÔNG bị overwrite về FREE).

**🔍 Verify DB:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub \
  -c "SELECT tier, pending_tier, status FROM subscriptions WHERE id='3ea90672-44d3-4ee9-9859-fd312482b429';"
# Kỳ vọng: tier=BASIC, pending_tier=FREE, status=ACTIVE (downgrade áp dụng cuối chu kỳ)
```

### Bước 2 — Cancel (hủy đăng ký) — browser `:3001`

**Hành động:**
1. Vào `http://localhost:3001/settings` → click tab **"Nguy hiểm"** (icon ⚠️ AlertTriangle).
2. Trong **DangerZone**, card **"Hủy đăng ký"** → click nút **"Hủy đăng ký"** (màu đỏ destructive).
3. Dialog **"Xác nhận hủy đăng ký"** mở ra (list hệ quả: còn hiệu lực đến hết chu kỳ → tạm ngưng → giữ data 30 ngày) → click **"Xác nhận hủy"**.

**✅ Kỳ vọng (PASS):**
- DevTools **Network**: request `DELETE http://localhost:9000/api/platform/subscriptions/3ea90672-44d3-4ee9-9859-fd312482b429` → **HTTP 204** No Content.
  - Lưu ý: FE cancel **KHÔNG** gửi `?immediate=true` → đây là **hủy cuối kỳ** (đúng theo copy DangerZone "còn hiệu lực đến hết chu kỳ thanh toán"), KHÁC với `curl ?immediate=true` hủy ngay.
- **Request Headers** có `Authorization: Bearer` do FE tự gắn (KHÔNG tay).
- FE: redirect `/billing?success=cancelled`.
- Console clean.

**⚠️ Sad path:** Nếu nút "Hủy đăng ký" click mà KHÔNG có request Network → `instance.subscriptionId` rỗng (DangerZone guard `if (instance?.subscriptionId)`) → seeding sai, xem §6.

**🔍 Verify DB:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub \
  -c "SELECT status, auto_renew, expires_at FROM subscriptions WHERE id='3ea90672-44d3-4ee9-9859-fd312482b429';"
# Kỳ vọng: auto_renew=false + status phản ánh đã hủy (CANCELLED hoặc cancel-scheduled cuối kỳ);
# vì hủy cuối kỳ (no immediate), expires_at có thể giữ tới cuối chu kỳ — autoRenew=false là tín hiệu chắc chắn.
```

## 4. Curl supplement (BE-only operational) — `:9000`

> **Renew = BE-only operational, no owner-facing FE** (per `SubscriptionController.java:53` — "operational/renewal-reminder view with no owner-facing FE"; `endpoints.ts` KHÔNG có renew). Phần này test bằng `curl` qua gateway `:9000` với `$TOKEN` từ §2.2. Chạy **sau** Bước 2 Cancel để tận dụng state CANCELLED cho sad-path; renew happy-path cần restore sub trước.

### 4a — Renew happy-path (gia hạn) — cần sub ACTIVE
```bash
# Restore sub về BASIC/ACTIVE trước (vì Bước 2 đã hủy):
docker exec kite-postgres psql -U kitehub -d kitehub \
  -c "UPDATE subscriptions SET tier='BASIC', status='ACTIVE', pending_tier=NULL, auto_renew=true WHERE id='$SUB';"

# Đọc state trước renew
curl -s "$GW/api/platform/subscriptions/$SUB" "${AUTH[@]}" | jq '{tier,status,expiresAt,version}'

# Renew
curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/subscriptions/$SUB/renew" "${AUTH[@]}"
```
**✅ Kỳ vọng:** HTTP **204** No Content. Verify `expiresAt` tăng **+1 tháng**, `version` +1.

**🔍 Verify (known-issue GAP-1016):** Renew KHÔNG tạo payment record (free renew):
`docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT count(*) FROM payments WHERE subscription_id='$SUB';"` — count KHÔNG tăng sau renew. **GAP-1016 P1 đã biết** — đừng re-flag.

### 4b — Renew sad-path FM-2 (đã FIX inline — phải PASS)
Renew một subscription `PENDING` (chưa confirm payment, `expiresAt = null`) → HTTP **400** (KHÔNG phải 500). Guard PENDING/null → IllegalArgumentException. Nếu có sub PENDING:
```bash
curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/subscriptions/<PENDING_SUB>/renew" "${AUTH[@]}"
# Kỳ vọng: ... [400], message rõ ràng — KHÔNG 500 "An unexpected error occurred"
```

### 4c — Renew sad-path: sub đã CANCELLED
Sau khi Cancel (Bước 2) mà CHƯA restore, gọi renew:
```bash
curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/subscriptions/$SUB/renew" "${AUTH[@]}"
```
**✅ Kỳ vọng:** HTTP **400** + message rõ ràng (không cho renew sub đã hủy).

### 4d — Idempotent double-cancel
Gọi lại `DELETE` (hủy ngay) một lần nữa sau Bước 2:
```bash
curl -s -w " [%{http_code}]" -X DELETE "$GW/api/platform/subscriptions/$SUB?immediate=true" "${AUTH[@]}"
```
**✅ Kỳ vọng:** HTTP **204** idempotent no-op (không lỗi, không đổi state).

### 4e — IDOR cross-tenant (KNOWN-ISSUE GAP-1015 — chỉ quan sát)
Dùng (hoặc tạo) subscription `$SUB_B` của tenant KHÁC, rồi dùng `$TOKEN` của Owner A gọi:
```bash
curl -s -w " [%{http_code}]" -X DELETE "$GW/api/platform/subscriptions/$SUB_B?immediate=true" "${AUTH[@]}"
```
**⚠️ KNOWN-ISSUE GAP-1015 (P0 IDOR):** Hiện trả HTTP **204** + cancel được sub của tenant khác (KHÔNG có ownership check). **Lỗ hổng P0 đã biết, đang chờ fix** (cần gateway forward `tenantId` + ownership guard; cùng class KC-7 GAP-1005 / KC-8 GAP-1007). **KHÔNG re-flag**. Nếu thấy **403** thì tốt (đã fix sớm) → báo lại.

## 5. Sad path quick checks

| Case | Layer | Expected |
|---|---|---|
| Downgrade qua FE → Network `Authorization` không do bạn gắn | Browser | FE tự inject `Bearer` (KHÔNG tay) |
| Downgrade khi có pending upgrade payment | Browser/BE | **400** (FM-5 fixed — pending_tier giữ nguyên) |
| Nút "Hủy đăng ký" không phát request Network | Browser | `instance.subscriptionId` rỗng → seeding sai (§6) |
| Cancel qua FE (no immediate) | Browser | **204**, hủy cuối kỳ, `auto_renew=false` |
| Renew sub `PENDING` (expiresAt null) | curl BE-only | **400** (FM-2 fixed — KHÔNG 500) |
| Renew sub đã `CANCELLED` | curl BE-only | **400** message rõ ràng |
| Double-cancel | curl BE-only | **204** idempotent no-op |
| Cancel sub của tenant khác | curl BE-only | **204 (KNOWN GAP-1015 P0 IDOR)** — chờ fix, đừng re-flag |
| Renew KHÔNG tạo payment | curl BE-only | **KNOWN GAP-1016 P1** free-renew |
| Cancel KHÔNG suspend instance | curl BE-only | **KNOWN GAP-1017 P1** |
| Renew gói ANNUALLY chỉ +1 tháng / double-renew cộng dồn | curl BE-only | **KNOWN GAP-1018 P2** hardening cluster |

## 6. Báo kết quả

Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → 2 browser-walk (downgrade + cancel) OK + FE tự gắn header + 2 inline fix (FM-2/FM-5) PASS + renew BE-only OK + 4 known-issue đúng mô tả → Claude flip campaign KH-5 → ✅ G1+G2 chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic, vd toast wording / layout) → catalog gap polish.
- 🔴 **BLOCKING** → browser button không phát request / Network 4xx-5xx lạ / FM-2 vẫn 500 / FM-5 vẫn corrupt tier / console error → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot DevTools Network + HTTP code.

## 7. G1/G2 browser-walk evidence (per g1-browser-walk-before-flip.md §3)

Điền khi walk 2 thao tác FE trên `:3001` (downgrade + cancel). Mỗi dòng phải qua **browser thật**, KHÔNG curl gắn header tay.

### 7.1 Downgrade (`:3001/billing` → "Nâng cấp" → TierSelector → ChangeConfirmation)

| # | Evidence | Tiêu chí PASS | Quan sát |
|---|---|---|---|
| (a) | FE entry point thật | Mở `http://localhost:3001/billing` trên browser (KHÔNG curl), trang render CurrentPlanCard | ⬜ |
| (b) | Console clean | DevTools Console không uncaught error / failed-to-fetch trên happy path | ⬜ |
| (c) | Network 2xx | `PATCH :9000/api/platform/subscriptions/{id}/downgrade` → **200** | ⬜ |
| (d) | FE-injected header | Request Headers có `Authorization: Bearer ...` do FE tự gắn (KHÔNG nhập tay) | ⬜ |
| (e) | FE route resolves | `/billing/upgrade` render wizard (TierSelector → ChangeConfirmation), redirect `/billing?success=downgrade` | ⬜ |
| (f) | ≥1 sad path qua browser | FM-5: downgrade khi có pending upgrade → FE hiển thị lỗi + Network **400** (KHÔNG silent) | ⬜ |

### 7.2 Cancel (`:3001/settings` → tab "Nguy hiểm" → DangerZone)

| # | Evidence | Tiêu chí PASS | Quan sát |
|---|---|---|---|
| (a) | FE entry point thật | Mở `http://localhost:3001/settings` → tab "Nguy hiểm" render DangerZone | ⬜ |
| (b) | Console clean | DevTools Console không uncaught error trên flow hủy | ⬜ |
| (c) | Network 2xx | `DELETE :9000/api/platform/subscriptions/{id}` → **204** | ⬜ |
| (d) | FE-injected header | Request Headers có `Authorization: Bearer ...` do FE tự gắn | ⬜ |
| (e) | FE route resolves | Dialog "Xác nhận hủy đăng ký" render, sau confirm redirect `/billing?success=cancelled` | ⬜ |
| (f) | ≥1 sad path qua browser | Nút click mà không phát request → quan sát guard `instance.subscriptionId` (báo nếu rỗng) | ⬜ |

> Nếu BẤT KỲ (a)-(e) FAIL → flow KHÔNG đạt G1/G2 PASS; catalog blocker + fix loop.

## 8. Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| `http://localhost:3001` trả `ERR_EMPTY_RESPONSE` | Stale docker-proxy port-forward (GAP-1067 class) — `bash kitehub/scripts/rebuild.sh kitehub-frontend` hoặc restart compose; chờ `kitehub-frontend` healthy |
| Login `:3001` redirect loop / 401 | Sai credential hoặc gateway down — `docker logs kite-gateway --tail 30`; xác nhận `owner@skyedu.vn` seeded |
| Nhầm port `:3000` | `:3000` = KiteClass (kiteclass-frontend), KHÔNG phải KH-5. KH-5 = `:3001` (per `kitehub-kiteclass-boundary.md` §2) |
| Network `PATCH /downgrade` không xuất hiện | TierSelector chưa cho chọn FREE, hoặc chọn nhầm gói ≥ BASIC (→ thành upgrade) — chọn đúng gói THẤP hơn |
| Nút "Hủy đăng ký" không phát DELETE | `instance.subscriptionId` rỗng — verify: `docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT id, subscription_id FROM instances WHERE id=(SELECT instance_id FROM subscriptions WHERE id='$SUB');"` |
| Renew PENDING vẫn trả **500** | Code FM-2 fix chưa nạp — `bash kitehub/scripts/rebuild.sh kitehub-subscription` |
| Downgrade ghi đè pending upgrade (tier sai) | Code FM-5 fix chưa nạp — rebuild `kitehub-subscription` |
| curl 3 endpoint trả **403** | JWT mang role `TENANT_OWNER` thay vì `OWNER` (authority-bridge, KC-7 class) — `echo $TOKEN \| cut -d. -f2 \| base64 -d 2>/dev/null \| jq .role`; nếu `TENANT_OWNER` → báo lại (gap ẩn) |
| Cần re-test từ đầu | Restore sub: `docker exec kite-postgres psql -U kitehub -d kitehub -c "UPDATE subscriptions SET tier='BASIC', status='ACTIVE', pending_tier=NULL, auto_renew=true WHERE id='$SUB';"` |

**G3 production-parity — preview (DEFERRED):** Walk qua gateway production thật (mint JWT prod → ALB HTTPS) kiểm 3 điều G2 local chưa cover: (a) **GAP-1015 IDOR phải đã fix** — gateway production forward `tenantId` + ownership guard thật (P0 blocker, ưu tiên fix trước G3); (b) revenue path — manual renew tạo payment thật (GAP-1016) thay vì free; (c) instance suspend khi cancel (GAP-1017) trên infra thật. G3 mở khi 4 gap GAP-1015..1018 được triage + ít nhất GAP-1015 P0 land. **G2 hiện test browser-walk FE `:3001` cho downgrade/cancel + curl BE-only cho renew là cách đúng cho Phase 1.**
