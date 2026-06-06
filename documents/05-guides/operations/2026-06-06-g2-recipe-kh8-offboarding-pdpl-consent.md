---
title: G2 Human Test Recipe — KH-8 Off-boarding + data retention (PDPL) + consent
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff cho KH-8 (consent v1/v2 + DSAR + off-boarding/purge)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh8-offboarding-pdpl-consent.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh8-offboarding-pdpl-consent.md
---

# G2 Human Test Recipe — KH-8 Off-boarding + data retention (PDPL) + consent

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Verify 3 sub-flow PDPL: consent (ghi/đọc/thu hồi), DSAR (yêu cầu + tra trạng thái), off-boarding (list instances + purge). Xác nhận consent v2 SECURE (bind user).

**Prereq:**
- Stack UP: `kite-gateway` + `kitehub-subscription` (consent/DSAR) + `kitehub-admin`/`kitehub-platform` (instances) + `kite-postgres` healthy.
- Đã merge Wave flow-kh8 (PR #2199).

**Thời lượng:** ~12 phút.

## 2. Setup

```bash
G=http://localhost:9000
OT=$(curl -s -X POST $G/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r .accessToken)
VID="g2-visitor-$(date +%s)"   # visitorId duy nhất cho consent test
```

## 3. Các bước

### Bước 1 — Ghi consent (public, anonymous visitor)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/consent/record" -H 'Content-Type: application/json' \
  -d "{\"visitorId\":\"$VID\",\"consents\":{\"analytics\":true,\"marketing\":false}}" -w '\n[%{http_code}]\n' | head -c 200
```
**✅ Kỳ vọng (PASS):** HTTP **201** (consent v1 là public — anonymous visitor ghi được, không cần đăng nhập).
**⚠️ Sad path:** body sai shape → 400.

### Bước 2 — Đọc consent
**Hành động:**
```bash
curl -s "$G/api/v1/consent/$VID" -w '\n[%{http_code}]\n' | head -c 200
```
**✅ Kỳ vọng (PASS):** HTTP **200**, trả về consents đã ghi ở Bước 1.

### Bước 3 — Thu hồi consent
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/consent/$VID/revoke" -w '\n[%{http_code}]\n' | head -c 150
```
**✅ Kỳ vọng (PASS):** HTTP **200** (đã thu hồi).
**⚠️ Sad path (KNOWN-ISSUE):** consent v1 revoke không cần auth (theo visitorId) → ai biết visitorId cũng revoke được = GAP-1027 P2 (mitigated bằng UUID khó đoán; by-design anonymous). Đừng báo lại.

### Bước 4 — DSAR: tạo yêu cầu truy cập dữ liệu
**Hành động:**
```bash
TICKET=$(curl -s -X POST "$G/api/v1/dsar/request" -H "Authorization: Bearer $OT" -H 'Content-Type: application/json' \
  -d '{"requestType":"ACCESS","email":"owner.test@test.vn"}' | jq -r '.ticketId // .data.ticketId')
echo "ticket: $TICKET"
```
**✅ Kỳ vọng (PASS):** HTTP **201**, có `ticketId`.

### Bước 5 — DSAR: tra trạng thái
**Hành động:**
```bash
curl -s "$G/api/v1/dsar/$TICKET" -H "Authorization: Bearer $OT" -w '\n[%{http_code}]\n' | head -c 250
```
**✅ Kỳ vọng (PASS):** HTTP **200**, dữ liệu redacted (PDPL — không lộ field nhạy cảm).

### Bước 6 — Off-boarding: list instances
**Hành động:**
```bash
curl -s "$G/api/platform/instances" -H "Authorization: Bearer $OT" -w '\n[%{http_code}]\n' | jq -c '.data | length' 2>/dev/null
```
**✅ Kỳ vọng (PASS):** HTTP **200**, danh sách instances.
**⚠️ KNOWN-ISSUE:** owner.test thấy được **TẤT CẢ** instances (không chỉ của mình) = GAP-1025 P0 (InstanceController thiếu @PreAuthorize). **Đừng test DELETE purge** — purge any-instance hiện reachable bởi any-user (lỗ hổng đã biết). Đừng báo lại.

## 4. Sad path quick checks
- DSAR request thiếu `requestType` → 400.
- Consent record với visitorId rỗng → 400.
- Consent v2 (authenticated, bind X-User-Id) — SECURE: user A không đọc được consent của user B (đã verify ở G1, không có IDOR).

## 5. ⚠️ KNOWN-ISSUE (đã filed — KHÔNG cần báo lại)
| Gap | Mức | Mô tả |
|-----|-----|-------|
| GAP-1025 | 🔴 P0 | `InstanceController` ZERO @PreAuthorize → bất kỳ user nào enumerate tất cả instances + purge any instance. **Đừng test DELETE purge.** Wave security-1. |
| GAP-1026 | 🟠 P1 | Purge instance chưa deleted → 200 FAILED (không 409); retention warning exact-day-match. |
| GAP-1027 | 🟡 P2 | Consent v1 revoke theo visitorId không cần auth (anonymous by-design, UUID-mitigated). |

## 6. Báo kết quả
- ✅ **FULL PASS** (Bước 1-6 happy PASS, KNOWN-ISSUE như mô tả) → Claude flip KH-8 → ✅ G1+G2 (chờ G3).
- ⚠️ **MOSTLY PASS** (cosmetic ngoài KNOWN-ISSUE) → catalog gap.
- 🔴 **BLOCKING** (1 bước happy fail) → báo bước + output + HTTP code.
- ❓ **UNCLEAR** → gửi screenshot/error.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|-------------|-----------|
| Consent record 401/403 thay vì 201 | Consent v1 phải là public — nếu bị chặn, gateway whitelist drift; báo BLOCKING. |
| DSAR ticket null | Response shape khác — `curl ... | jq` xem field thực tế. |
| `/api/platform/instances` 404 | Routing — thử `/api/platform/admin/instances`. |

**G3 production-parity preview:** G3 verify PDPL flows trên prod-equivalent. **Bắt buộc** confirm GAP-1025 fix (InstanceController @PreAuthorize + tenant-scoped purge) trước khi expose production — đây là P0 data-protection.
