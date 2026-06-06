---
title: G2 Human Test Recipe — KC-11 Notification + document generation
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff cho KC-11 (document gen PDF/XLSX/DOCX + reports + Zalo stub)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kc11-notification-document-gen.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc11-notification-document-gen.md
---

# G2 Human Test Recipe — KC-11 Notification + document generation

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Verify document generation (PDF/XLSX/DOCX preview + download) + reports (revenue/attendance, ADMIN-only) + role bridge + Zalo OA stub graceful. Xác nhận security gate (TEACHER→reports 403, document tenant-bound).

**Prereq:**
- Stack UP: `kite-gateway` + `kiteclass-core` + `kite-postgres` healthy.
- Đã merge Wave flow-kc11 (PR #2203).

**Thời lượng:** ~10-12 phút.

## 2. Setup

```bash
G=http://localhost:9000
export JWT_SECRET=$(docker exec kite-gateway sh -c 'printf %s "$JWT_SECRET"')
mint(){ python3 -c "import hmac,hashlib,base64,json,time,os;s=os.environ['JWT_SECRET'].encode();b=lambda x:base64.urlsafe_b64encode(x).rstrip(b'=');h=b(json.dumps({'alg':'HS512','typ':'JWT'},separators=(',',':')).encode());n=int(time.time());p=b(json.dumps({'sub':'$1','email':'$2','role':'$3','type':'access','tenantId':'aaaabbbb-0000-0000-0000-000000000001','iat':n,'exp':n+3600},separators=(',',':')).encode());sig=b(hmac.new(s,h+b'.'+p,hashlib.sha512).digest());print((h+b'.'+p+b'.'+sig).decode())"; }
OT=$(curl -s -X POST $G/api/auth/login -H 'Content-Type: application/json' -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r .accessToken)
AT=$(mint "11111111-aaaa-0000-0000-000000000001" "admin.kc@test.vn" "ADMIN")
TT=$(mint "22222222-bbbb-0000-0000-000000000002" "teacher.kc@test.vn" "TEACHER")
```

## 3. Các bước

### Bước 1 — Document PDF preview (invoice)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/documents/pdf/preview" -H "Authorization: Bearer $OT" -H 'Content-Type: application/json' \
  -d '{"templateId":"invoice","data":{"invoiceNumber":"INV-1","items":[{"name":"Hoc phi","amount":500000}]}}' \
  -w '\n[HTTP %{http_code} bytes=%{size_download}]\n' -o /tmp/g2-kc11.pdf
file /tmp/g2-kc11.pdf
```
**✅ Kỳ vọng (PASS):** HTTP **200**, `bytes` > 100000, `file` báo "PDF document".
**⚠️ Sad path:** format không hợp lệ (`/api/v1/documents/exe/download`) → 400; body rỗng → 400 (không phải 500).

### Bước 2 — Document XLSX download (attendance)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/documents/xlsx/download" -H "Authorization: Bearer $OT" -H 'Content-Type: application/json' \
  -d '{"templateId":"attendance","data":{"rows":[]}}' -w '\n[HTTP %{http_code} bytes=%{size_download}]\n' -o /tmp/g2-kc11.xlsx
```
**✅ Kỳ vọng (PASS):** HTTP **200**, file XLSX (~3-4KB).

### Bước 3 — Reports (revenue) — ADMIN-only
**Hành động:**
```bash
curl -s "$G/api/v1/reports/revenue" -H "Authorization: Bearer $AT" | jq '.data.totalRevenue'
# Inverse-authz: TEACHER và OWNER KHÔNG được vào reports
curl -s "$G/api/v1/reports/revenue" -H "Authorization: Bearer $TT" -w '[TEACHER %{http_code}]\n' -o /dev/null
curl -s "$G/api/v1/reports/revenue" -H "Authorization: Bearer $OT" -w '[OWNER %{http_code}]\n' -o /dev/null
```
**✅ Kỳ vọng (PASS):** ADMIN → 200 (`totalRevenue` số); TEACHER → **403**; OWNER → **403** (reports `@PreAuthorize ADMIN` only).

### Bước 4 — Role bridge: TEACHER được vào documents
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/documents/pdf/preview" -H "Authorization: Bearer $TT" -H 'Content-Type: application/json' \
  -d '{"templateId":"invoice","data":{"items":[]}}' -w '[TEACHER documents %{http_code}]\n' -o /dev/null
```
**✅ Kỳ vọng (PASS):** TEACHER → **200** (documents cho phép ADMIN/OWNER/TEACHER → role bridge hoạt động).

### Bước 5 — Zalo OA stub (graceful)
**Hành động:** Zalo OA là stub (không gửi thật). Trigger gián tiếp qua parent invite (nếu có flow) hoặc check log:
```bash
docker logs kiteclass-core --since 2m 2>&1 | grep -i "would send Zalo OA" | tail -2
```
**✅ Kỳ vọng (PASS):** Nếu có log "would send Zalo OA: ..." → stub graceful (không crash). Nếu không có log (chưa trigger) → OK, stub không tự fire.

## 4. Sad path quick checks
- `POST /documents/pdf/preview` với `templateId` không tồn tại → lỗi rõ ràng (không 500 crash).
- Reports thiếu tenant context → (ℹ️ xem KNOWN-ISSUE GAP-1039).
- XLSX với data lạ → vẫn 200 (template kéo data từ DB, không từ caller — formula injection NEGATIVE).

## 5. ⚠️ KNOWN-ISSUE (đã filed — KHÔNG cần báo lại)
| Gap | Mức | Mô tả |
|-----|-----|-------|
| GAP-1039 | 🟠 P1 | Reports revenue/attendance **cross-tenant aggregate leak** khi thiếu `X-Tenant-Id` (repos thiếu instance_id predicate). Qua gateway thì header luôn có nên scoped; lỗ hổng ở defense-in-depth. Wave security-1. |
| GAP-1040 | 🟠 P1 | Document gen **SSRF** — caller inject `data.logoUrl` → OpenHTMLtoPDF fetch URL server-side. **Đừng test gửi logoUrl tới internal host.** Wave security-1. |

## 6. Báo kết quả
- ✅ **FULL PASS** → Claude flip KC-11 → ✅ G1+G2 (chờ G3).
- ⚠️ **MOSTLY PASS** (cosmetic ngoài KNOWN-ISSUE) → catalog gap.
- 🔴 **BLOCKING** (Bước 1/2 document gen fail) → báo bước + output.
- ❓ **UNCLEAR** → gửi screenshot/error.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|-------------|-----------|
| PDF bytes = 0 / HTTP 500 | Template/data sai shape — dùng `templateId:"invoice"` + data đơn giản. |
| TEACHER → reports 200 (lẽ ra 403) | Role bridge drift — báo BLOCKING (security regression). |
| ADMIN → reports 403 | ADMIN token role-literal sai — verify token `role:ADMIN`. |

**G3 production-parity preview:** G3 verify document gen + reports trên prod-equivalent. Reports G3 phải confirm GAP-1039 fix (instance_id predicate). Document gen G3 phải confirm GAP-1040 SSRF fix (allowlist host cho logoUrl) trước khi expose production.
