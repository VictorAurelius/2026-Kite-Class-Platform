---
title: G2 Human Test Recipe — KC-8 Parent portal
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign G2 handoff for KC-8 parent portal (child grade/attendance/fees/conduct facets)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-05-flow-kc8-parent-portal.md
  - documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc8-parent-portal.md
---

# G2 Human Test Recipe — KC-8 Parent portal

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (dev) tự xác nhận flow phụ huynh xem dữ liệu con chạy thật trên local stack — đăng nhập danh tính PARENT → xem danh sách con → cấp consent → đọc học phí/điểm danh/học bạ/hạnh kiểm, với IDOR + consent gate hoạt động đúng.

**Prereq:**
- Local Docker stack UP (kiteclass-core + kite-postgres healthy). Check: `docker ps | grep kiteclass-core` → `healthy`.
- Fixtures sẵn (dev DB `kiteclass_shared`): parent 1 (`parent1@test.com`, ref-id 1) → child 1; parent 2 → child 2; cùng instance `aaaabbbb-…0001`; child 1 có 2 invoices.
- Fixes Wave flow-kc8 đã ship: GAP-1006 (fees 500 fix) + 2 `@PreAuthorize` (notifications/payment).

**Thời lượng:** ~10-15 phút (API walk). FE UI optional (xem §4 lưu ý mock).

**Lưu ý FE quan trọng:** FE parent portal hiện CHỈ wire thật trang **transcript** + `/me`. Trang attendance/grades/billing render **mock data** (FE-wiring defer Phase 1.5). G2 này test **backend qua API** (canonical) — đó là logic vừa walk G1. Nếu test FE UI, chỉ transcript là dữ liệu thật.

## 2. Setup

```bash
PORT=8088   # docker port kiteclass-core 8080
TENANT=aaaabbbb-0000-0000-0000-000000000001
# Headers parent 1 (gateway inject khi production; G1/G2 local curl trực tiếp core)
P1=(-H "X-Tenant-Id: $TENANT" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" \
    -H "X-User-Reference-Id: 1" -H "X-User-Roles: PARENT")
```

Tools: terminal + curl. (Optional: browser `http://localhost:3000/parent` cho FE transcript — parent portal là **KiteClass** tenant app `kiteclass-frontend` `:3000` `(dashboard)/parent`, KHÔNG phải KiteHub `:3001`.)

## 3. Các bước

### Bước 1 — Xem danh sách con
**Hành động:** `curl -s "http://localhost:$PORT/api/v1/parent/me/children" "${P1[@]}" | jq`
**✅ Kỳ vọng (PASS):** HTTP 200, `data` = 1 child `{"studentId":1,"studentName":"Nguyen Van S1","linkType":"PRIMARY"}`.
**⚠️ Sad path:** Bỏ header `X-User-Reference-Id` → 401 `AUTH_REQUIRED`.

### Bước 2 — Đọc khi CHƯA cấp consent (PDPL gate)
**Hành động:** `curl -s -w " [%{http_code}]" "http://localhost:$PORT/api/v1/parent/children/1/attendance?from=2026-01-01&to=2026-12-31" "${P1[@]}"`
**✅ Kỳ vọng (PASS):** HTTP **403** `PARENT_CONSENT_REQUIRED` — phụ huynh chưa cấp quyền xem field này.
**🔍 Verify:** Đây là first-login behavior đúng (FM#6 pre-walk) — FE phải hiển thị CTA "cấp consent", không phải báo lỗi chung.

### Bước 3 — Cấp consent
**Hành động:**
```bash
curl -s -X PUT "http://localhost:$PORT/api/v1/parent/consent?childId=1" "${P1[@]}" \
  -H "Content-Type: application/json" \
  -d '{"updates":{"attendance":true,"fees":true,"transcript":true,"conduct":true,"notifications":true}}' | jq
```
**✅ Kỳ vọng (PASS):** HTTP 200, `data.fields` tất cả `true`, `version` tăng.

### Bước 4 — Đọc dữ liệu con sau consent
**Hành động:**
```bash
curl -s "http://localhost:$PORT/api/v1/parent/children/1/fees?from=2026-01-01&to=2026-12-31" "${P1[@]}" | jq '.data.content'
curl -s "http://localhost:$PORT/api/v1/parent/children/1/attendance?from=2026-01-01&to=2026-12-31" "${P1[@]}" | jq '.data.totalElements'
curl -s "http://localhost:$PORT/api/v1/parent/children/1/transcript" "${P1[@]}" | jq '.data'
```
**✅ Kỳ vọng (PASS):**
- fees → 200, 2 invoices (INV-TEST-001/002). **KHÔNG 500** (GAP-1006 fixed).
- attendance → 200, `totalElements: 0` (chưa seed data → empty page hợp lệ).
- transcript → 200, `[]` (chưa seed).
**⚠️ Sad path:** Thiếu `from`/`to` cho fees/attendance → 400 `PARAM_MISSING` (không phải 500 — fix Wave flow-kc8).

### Bước 5 — IDOR: thử xem con của phụ huynh KHÁC
**Hành động:** `curl -s -w " [%{http_code}]" "http://localhost:$PORT/api/v1/parent/children/2/fees?from=2026-01-01&to=2026-12-31" "${P1[@]}"`
**✅ Kỳ vọng (PASS):** HTTP **403** `ACCESS_DENIED` — parent 1 KHÔNG link với child 2.
**🔍 Verify:** Lặp với `/children/2/notifications` + POST `/children/2/payments` (body `{"invoiceId":1,"amount":100000,"paymentMethod":"BANK_TRANSFER"}` + header `Idempotency-Key: x`) → cả 2 đều **403** (2 controller này vừa được thêm `@PreAuthorize` Wave flow-kc8).

## 4. Sad path quick checks

| Case | Expected |
|---|---|
| Bỏ `X-User-Reference-Id` | 401 `AUTH_REQUIRED` |
| `childId=abc` | 400 `PARAM_TYPE_MISMATCH` |
| Thiếu `from`/`to` | 400 `PARAM_MISSING` (không 500) |
| `from` > `to` | 400 `BAD_REQUEST` |
| Đọc con chưa consent | 403 `PARENT_CONSENT_REQUIRED` |
| Đọc con của parent khác | 403 `ACCESS_DENIED` |

## 5. Báo kết quả

Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → Claude flip campaign KC-8 → ✅ G1+G2 chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic, vd FE mock UX) → catalog gap polish.
- 🔴 **BLOCKING** → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm output/screenshot.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| HTTP 000 | Container restarting — `docker inspect kiteclass-core --format '{{.State.Health.Status}}'` chờ `healthy` |
| fees 500 | Code GAP-1006 chưa nạp — `bash kitehub/scripts/rebuild.sh kiteclass-core` |
| Mọi facet 403 dù đã consent | Check `version` consent ≥ `kite.parent.consent.required-version` (default 1) |
| FE attendance/fees hiển thị data lạ | Đó là mock fixture (FE-wiring defer) — test qua API là canonical |

**G3 production-parity — DEFERRED Phase 2 (đã verify 2026-06-05):** Walk qua gateway thật (mint JWT → :9000) bị chặn vì **parent LOGIN là Phase 2 by design** — `TokenService.resolveTenantIdForRole` chỉ issue tenantId cho OWNER (PARENT/STUDENT chưa), gateway KHÔNG inject `X-User-Reference-Id`, `TokenService` không issue `referenceId` claim. Tracked: **GAP-725** (parent/teacher/student auth path Phase 2) + **GAP-798b** (reference_id producer side — blocked on login-wiring, deliberately not built per trust-pass anti-pattern). Consumer-side authz (`@authz.hasAccessToChild`) đã ship + verified G1. G3 gateway-parity unblock khi GAP-725/798b land Phase 2. **G2 hiện test direct-core API (header inject) là cách đúng cho Phase 1** — đó là logic production-ready của BE facets.

**Known defers (không block G2):** GAP-1007 (role-collision IDOR defense-in-depth P2), GAP-1008 (payment consent asymmetry P3), FE wiring attendance/fees/billing (Phase 1.5), notifications facet stub empty (GAP-063b), payment VietQR stub (Wave 106).


---

## 🔄 Re-walk update 2026-06-16 (UNBLOCKED — parent seed shipped)

- **Verdict:** ✅ parent portal walkable sau khi seed (GAP-1457 DONE).
- **Seed:** chạy `kiteclass/kiteclass-core/scripts/dev-seed-parent-kc8.sql` vào `kiteclass_shared` (dev-only, idempotent). Tạo parent Trần Thị Hồng + link student 167 (Phạm Thị Mai) + consent 5 facets, tenant TRIAL `sky-education-074901`.
- **Credential parent:** `hong.tran+074901@gmail.com / Parent@123` (login qua `/api/v1/tenant-auth/login` → JWT referenceId → gateway inject X-User-Reference-Id).
- **Access:** nip.io `sky-education-074901.127.0.0.1.nip.io:3000` → redirect `/parent`.
- **Verified:** login 200 → /me Trần Thị Hồng → /me/children [Phạm Thị Mai] → fees facet real invoice 3.5M (consent gate) → IDOR cross-child 403. 0 console errors.
- **STALE note:** wave-plan §11 "parent Phase-2-gated" SAI — auth-1 (GAP-725/1122) đã ship parent login.
- **Còn lại (GAP-1458 PARTIAL Phase 1.5):** FE facet pages (attendance/billing/grades + hero 92