---
title: G2 Human Test Recipe — KH-5 Subscription lifecycle (downgrade / cancel / renew)
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff for KH-5 subscription lifecycle (Owner downgrade/cancel/renew)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh5-subscription-lifecycle.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md
---

# G2 Human Test Recipe — KH-5 Subscription lifecycle

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (dev) tự xác nhận flow Owner quản lý vòng đời subscription chạy thật trên local Docker stack — đăng nhập danh tính `OWNER` → renew (gia hạn) → downgrade (hạ gói) → cancel (hủy) qua gateway `:9000`, với happy path + sad path hoạt động đúng. Trọng tâm: xác nhận 2 inline fix (FM-2 NPE→400, FM-5 downgrade corruption→400) đã PASS, và nhận diện 4 known-issue (GAP-1015..1018) KHÔNG báo nhầm thành bug mới.

**Prereq:**
- Local Docker stack UP: gateway `:9000` + `kitehub-subscription` + `kite-postgres` healthy. Check: `docker ps | grep -E 'kite-gateway|kitehub-subscription'` → `healthy`.
- Code Wave flow-kh5 đã ship: 2 inline fix (FM-2 + FM-5) + rebuild `kitehub-subscription`.
- Seeded data: Owner `owner.test@test.vn / Test@1234` (tenant `22003e3c…`), subscription `81cf38cd…` ở trạng thái `BASIC` / `ACTIVE`.

**Thời lượng:** ~10-15 phút (API walk qua gateway). Toàn bộ test bằng `curl` — không cần UI.

**Lưu ý quan trọng:** Các bước happy path **thay đổi state thật** của subscription seeded (renew → downgrade → cancel). Walk theo đúng thứ tự Bước 3→5; sau khi cancel ở Bước 5 thì sub vào `CANCELLED` (đúng để test sad path "renew sub đã CANCELLED"). Sau walk, nếu muốn re-test, restore sub về `BASIC`/`ACTIVE` (xem §6 Troubleshooting).

## 2. Setup

```bash
GW=http://localhost:9000

# Đăng nhập Owner → lấy accessToken
TOKEN=$(curl -s -X POST "$GW/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r '.accessToken')
echo "TOKEN length: ${#TOKEN}"   # > 100 nghĩa là login OK

# Subscription seeded (BASIC/ACTIVE)
SUB=81cf38cd-0000-0000-0000-000000000000   # thay bằng id thật nếu khác
AUTH=(-H "Authorization: Bearer $TOKEN")
```

Base path subscription: `/api/platform/subscriptions`. Tools: terminal + `curl` + `jq`.

> Nếu chưa biết `SUB` id thật: query nhanh DB
> `docker exec kite-postgres psql -U kite -d kitehub -c "SELECT id, tier, status FROM subscriptions WHERE status='ACTIVE' LIMIT 3;"`

## 3. Các bước

### Bước 0 — Đọc state ban đầu
**Hành động:** `curl -s "$GW/api/platform/subscriptions/$SUB" "${AUTH[@]}" | jq '{tier,status,expiresAt,version}'`
**✅ Kỳ vọng (PASS):** HTTP 200, `tier: "BASIC"`, `status: "ACTIVE"`. Ghi nhớ `expiresAt` + `version` để so sánh sau renew.
**⚠️ Sad path:** Bỏ header `Authorization` → 401 `AUTH_REQUIRED`.

### Bước 1 — Renew (gia hạn) — happy path
**Hành động:** `curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/subscriptions/$SUB/renew" "${AUTH[@]}"`
**✅ Kỳ vọng (PASS):** HTTP **204** No Content. Verify side effect:
```bash
curl -s "$GW/api/platform/subscriptions/$SUB" "${AUTH[@]}" | jq '{expiresAt,version}'
```
→ `expiresAt` tăng **+1 tháng** so với Bước 0, `version` tăng +1.
**⚠️ Sad path (đã FIX inline — phải PASS):** Renew một subscription `PENDING` (chưa confirm payment, `expiresAt = null`) → HTTP **400** (KHÔNG phải 500). Đây là **FM-2 đã fix** (guard PENDING/null → IllegalArgumentException). Nếu có sub PENDING:
```bash
curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/subscriptions/<PENDING_SUB>/renew" "${AUTH[@]}"
# Kỳ vọng: ... [400], message rõ ràng — KHÔNG 500 "An unexpected error occurred"
```
**🔍 Verify (known-issue GAP-1016):** Renew KHÔNG tạo payment record (free renew). `docker exec kite-postgres psql -U kite -d kitehub -c "SELECT count(*) FROM payments WHERE subscription_id='$SUB';"` — count KHÔNG tăng sau renew. **Đây là GAP-1016 P1 đã biết** — không phải bug mới, đừng re-flag.

### Bước 2 — Downgrade (hạ gói) — happy path
**Hành động:**
```bash
curl -s -w " [%{http_code}]" -X PATCH "$GW/api/platform/subscriptions/$SUB/downgrade" "${AUTH[@]}" \
  -H 'Content-Type: application/json' -d '{"newTier":"FREE"}'
```
**✅ Kỳ vọng (PASS):** HTTP **200**. Verify: `curl -s "$GW/api/platform/subscriptions/$SUB" "${AUTH[@]}" | jq '{tier,pendingTier,status}'` → `pendingTier: "FREE"` đã set, `status` vẫn `ACTIVE` (downgrade áp dụng cuối chu kỳ).
**⚠️ Sad path (đã FIX inline — phải PASS):** Downgrade khi đang có pending upgrade payment → HTTP **400** (reject). Đây là **FM-5 đã fix** (guard `pendingPaymentId != null` → 400, tránh ghi đè pending upgrade làm hỏng tier). Để tái hiện: trước tiên upgrade tạo `pendingPaymentId`, rồi downgrade → kỳ vọng `[400]` + `pending_tier` giữ nguyên gói upgrade (KHÔNG bị overwrite về FREE).

### Bước 3 — Cancel (hủy) — happy path
**Hành động:** `curl -s -w " [%{http_code}]" -X DELETE "$GW/api/platform/subscriptions/$SUB?immediate=true" "${AUTH[@]}"`
**✅ Kỳ vọng (PASS):** HTTP **204**. Verify: `curl -s "$GW/api/platform/subscriptions/$SUB" "${AUTH[@]}" | jq '{status,expiresAt,autoRenew}'` → `status: "CANCELLED"`, `expiresAt ≈ now`, `autoRenew: false`.
**🔍 Verify (known-issue GAP-1017):** Cancel KHÔNG suspend instance. `docker exec kite-postgres psql -U kite -d kitehub -c "SELECT status FROM instances WHERE id=(SELECT instance_id FROM subscriptions WHERE id='$SUB');"` → instance vẫn `ACTIVE`. **Đây là GAP-1017 P1 đã biết** — đừng re-flag.

### Bước 4 — Sad path: idempotent double-cancel
**Hành động:** Gọi lại `DELETE` ở Bước 3 một lần nữa.
**✅ Kỳ vọng (PASS):** HTTP **204** idempotent no-op (không lỗi, không đổi state).

### Bước 5 — Sad path: renew sub đã CANCELLED
**Hành động:** `curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/subscriptions/$SUB/renew" "${AUTH[@]}"`
**✅ Kỳ vọng (PASS):** HTTP **400** + message rõ ràng (không cho renew sub đã hủy).

### Bước 6 — IDOR cross-tenant (KNOWN-ISSUE GAP-1015 — chỉ quan sát)
**Hành động:** Tạo (hoặc dùng) subscription `$SUB_B` của tenant KHÁC, rồi dùng `$TOKEN` của Owner A gọi:
```bash
curl -s -w " [%{http_code}]" -X DELETE "$GW/api/platform/subscriptions/$SUB_B?immediate=true" "${AUTH[@]}"
```
**⚠️ KNOWN-ISSUE GAP-1015 (P0 IDOR):** Hiện trả HTTP **204** + cancel được sub của tenant khác (KHÔNG có ownership check). **Đây là lỗ hổng P0 đã biết, đang chờ fix** (cần gateway forward `tenantId` + ownership guard; cùng class KC-7 GAP-1005 / KC-8 GAP-1007). **KHÔNG re-flag** — chỉ xác nhận hành vi để theo dõi. Nếu bạn thấy **403** thì tốt (đã được fix sớm) → báo lại.

## 4. Sad path quick checks

| Case | Expected |
|---|---|
| Bỏ `Authorization` header | 401 `AUTH_REQUIRED` |
| Renew sub `PENDING` (expiresAt null) | **400** (FM-2 fixed — KHÔNG 500) |
| Downgrade khi có pending upgrade payment | **400** (FM-5 fixed — pending_tier giữ nguyên) |
| Double-cancel | 204 idempotent no-op |
| Renew sub đã `CANCELLED` | 400 message rõ ràng |
| Cancel sub của tenant khác | **204 (KNOWN GAP-1015 P0 IDOR)** — chờ fix, đừng re-flag |
| Renew KHÔNG tạo payment | **KNOWN GAP-1016 P1** free-renew |
| Cancel KHÔNG suspend instance | **KNOWN GAP-1017 P1** |
| Renew gói ANNUALLY chỉ +1 tháng / double-renew cộng dồn | **KNOWN GAP-1018 P2** hardening cluster |

## 5. Báo kết quả

Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → happy path 3 endpoint OK + 2 inline fix (FM-2/FM-5) PASS + 4 known-issue đúng như mô tả → Claude flip campaign KH-5 → ✅ G1+G2 chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic, vd message wording chưa đẹp) → catalog gap polish.
- 🔴 **BLOCKING** → happy path lỗi HTTP lạ / FM-2 vẫn 500 / FM-5 vẫn corrupt tier → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm output `curl` + HTTP code.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| HTTP 000 | Container restarting — `docker inspect kitehub-subscription --format '{{.State.Health.Status}}'` chờ `healthy` |
| Login 401 / TOKEN rỗng | Sai credential hoặc gateway down — `docker logs kite-gateway --tail 30`; xác nhận `owner.test@test.vn` seeded |
| Renew PENDING vẫn trả **500** | Code FM-2 fix chưa nạp — `bash kitehub/scripts/rebuild.sh kitehub-subscription` |
| Downgrade ghi đè pending upgrade (tier sai) | Code FM-5 fix chưa nạp — rebuild `kitehub-subscription` |
| Tất cả 3 endpoint trả **403** | JWT mang role claim `TENANT_OWNER` thay vì `OWNER` (authority-bridge, KC-7 class) — decode JWT: `echo $TOKEN \| cut -d. -f2 \| base64 -d 2>/dev/null \| jq .role`; nếu `TENANT_OWNER` → báo lại (gap ẩn) |
| Cần re-test từ đầu | Restore sub: `docker exec kite-postgres psql -U kite -d kitehub -c "UPDATE subscriptions SET tier='BASIC', status='ACTIVE', pending_tier=NULL, auto_renew=true WHERE id='$SUB';"` |

**G3 production-parity — preview (DEFERRED):** Walk qua gateway production thật (mint JWT prod → ALB HTTPS) sẽ kiểm 3 điều G2 local chưa cover: (a) **GAP-1015 IDOR phải đã fix** — gateway production forward `tenantId` + ownership guard thật (P0 blocker, ưu tiên fix trước G3); (b) revenue path — manual renew tạo payment thật (GAP-1016) thay vì free; (c) instance suspend khi cancel (GAP-1017) trên infra thật. G3 mở khi 4 gap GAP-1015..1018 được triage + ít nhất GAP-1015 P0 land. **G2 hiện test direct qua local gateway là cách đúng cho Phase 1** — xác nhận logic lifecycle BE happy/sad + 2 inline fix.
