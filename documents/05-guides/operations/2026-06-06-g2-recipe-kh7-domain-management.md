---
title: Công thức G2 — KH-7 Quản lý custom domain (add → verify → status → delete)
audience: dev
product: KiteHub (KH) — FE kitehub-frontend :3001, backend kitehub-* qua gateway :9000 (per kitehub-kiteclass-boundary.md §2)
created: 2026-06-06
scope: Bàn giao G2 thủ công cho luồng KH-7 (custom domain management) thuộc Chiến dịch Xác minh Luồng
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh7-domain-management.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh7-domain-management.md
  - documents/05-guides/operations/2026-06-04-g2-recipe-kh3-subscription.md
---

# Công thức G2 — KH-7 Quản lý custom domain

> **Trạng thái:** G1 ✅ PASS (tôi tự walk) — Wave flow-kh7. Đã fix inline 3 lỗi (FM-1 `@PreAuthorize` 4 endpoint + FM-2 regex multi-label VN-domain + FM-5 reserved denylist). Còn 2 gap đã biết: **GAP-1023** (P0 cross-tenant IDOR, PARTIAL) + **GAP-1024** (P1 verification state machine). G2 này để bạn xác nhận G1 không bỏ sót lỗi hiển thị / sai cảm nhận người dùng.

## 1. Mục tiêu + điều kiện đầu vào + thời lượng

**Mục tiêu:** Bạn (dev) đóng vai **Owner** đi hết vòng đời custom domain qua gateway thật: xem trạng thái ban đầu (NONE) → gắn domain → nhận TXT verify token → bấm verify → xoá domain. Xác nhận flow chạy end-to-end + các sad path (domain sai định dạng, domain hệ thống bị chặn, instance không tồn tại) hành xử đúng.

> ⚠️ **Trần verify = `PENDING_VERIFY` (KHÔNG đạt VERIFIED trên local — đây là kỳ vọng ĐÚNG, KHÔNG phải bug).** `DnsTxtLookupService` làm real DNS TXT lookup qua JNDI; domain test (`school.com`) không có TXT record `kitehub-verify={uuid}` thật trên DNS công cộng nên verify luôn trả `false` → state ở lại `PENDING_VERIFY`. Muốn đạt VERIFIED phải sở hữu domain thật + thêm đúng TXT record — ngoài phạm vi G2 local. Cứ catalog là PASS khi thấy `POST /verify` trả 200 + giữ `PENDING_VERIFY` (KHÔNG phải 500).

**Điều kiện đầu vào:**

- Stack Docker local đang chạy, các service `healthy`: `kite-gateway`, `kitehub-subscription`, `kite-postgres`. (FE `kitehub-frontend` tùy chọn — KH-7 hiện chưa có trang FE quản lý domain hoàn chỉnh, G2 này test **backend qua API**, đó là logic vừa walk G1.)
- Tài khoản Owner: `owner.test@test.vn` / `Test@1234`.
- Một instance tier **PREMIUM/ENTERPRISE** + có `subdomain` non-null (tier gate ở `DomainService` chặn instance FREE/TRIAL/BASIC). Wave flow-kh7 đã tạm set instance `22003e3c…` lên `PREMIUM` cho walk — lệnh §2 sẽ lấy đúng instanceId.

**Thời lượng:** ~10-15 phút (API walk qua gateway).

## 2. Thiết lập

```bash
GW=http://localhost:9000
```

| Mục | Lệnh / hành động |
|---|---|
| Kiểm tra gateway | `curl -sS $GW/actuator/health` → phải trả `{"status":"UP",...}` (Gateway thật ở port **9000**) |
| Lấy accessToken Owner | Đăng nhập lấy JWT (xem dưới) |
| Lấy instanceId PREMIUM + xác nhận subdomain non-null | `docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT id, subdomain, tier, custom_domain, domain_status FROM instances WHERE tier IN ('PREMIUM','ENTERPRISE') AND subdomain IS NOT NULL LIMIT 3;"` |

**Đăng nhập + bắt token vào biến shell:**

```bash
TOKEN=$(curl -sS -X POST $GW/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r '.accessToken')
echo "TOKEN length: ${#TOKEN}"   # > 0 nghĩa là login OK

# Lấy instanceId PREMIUM của Owner (thay nếu DB của bạn khác)
IID=$(docker exec kite-postgres psql -U kitehub -d kitehub -t -A -c \
  "SELECT id FROM instances WHERE tier IN ('PREMIUM','ENTERPRISE') AND subdomain IS NOT NULL LIMIT 1;")
echo "Instance: $IID"
```

> Nếu không có instance PREMIUM nào: `docker exec kite-postgres psql -U kitehub -d kitehub -c "UPDATE instances SET tier='PREMIUM' WHERE id='<id>';"` rồi lấy lại `$IID`. (Nhớ trả về tier cũ sau khi walk xong — Wave flow-kh7 restore instance về FREE + clear domain.)

Công cụ: terminal + `curl` + `jq` + `docker exec` psql.

## 3. Các bước

### Bước 1 — Xem trạng thái domain ban đầu (baseline NONE)

**Hành động:**
```bash
curl -sS "$GW/api/instances/$IID/domain" \
  -H "Authorization: Bearer $TOKEN" -w "\nHTTP=%{http_code}\n" | jq
```

**✅ Kỳ vọng (PASS):** HTTP **200**. Payload `status="NONE"` (chưa gắn domain) + `backupUrl` dạng `https://<subdomain>.kitehub.me`.

**⚠️ Sad path:**
- HTTP 401 → thiếu/sai `Authorization` header (gateway default-deny `/api/instances/**`).
- `backupUrl` = `https://null.kitehub.me` → instance thiếu `subdomain` (cosmetic, pre-walk mục 8). Chọn instance khác có subdomain.

**🔍 Verify:** `status` = `NONE` xác nhận chưa có domain pending — baseline sạch để walk.

---

### Bước 2 — Gắn custom domain (add → PENDING_VERIFY)

**Hành động:**
```bash
curl -sS -X POST "$GW/api/instances/$IID/domain" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customDomain":"school.com"}' -w "\nHTTP=%{http_code}\n" | jq
```

**✅ Kỳ vọng (PASS):** HTTP **200**. Payload:
- `status="PENDING_VERIFY"`
- `verifyToken` dạng `kitehub-verify={uuid}` (ví dụ `kitehub-verify=3f2a...`)
- Hướng dẫn TXT record để thêm vào DNS (host `_kitehub-verify.school.com` hoặc apex, value = `verifyToken`)

**⚠️ Sad path:**
- HTTP **400** "Custom domain is only available for PREMIUM and ENTERPRISE tiers" → instance không phải PREMIUM/ENTERPRISE (tier gate). Quay lại §2 set tier.
- HTTP **400** regex reject → bạn dùng domain khác `school.com`. Happy path PHẢI dùng domain 2-label `school.com` (KH-7 G1 đã fix FM-2 cho cả 3-label, nhưng `school.com` là đường chuẩn nhất).

**🔍 Verify DB:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT custom_domain, domain_status, domain_verify_token FROM instances WHERE id='$IID';"
```
→ `custom_domain=school.com`, `domain_status=PENDING_VERIFY`, `domain_verify_token` không null.

---

### Bước 3 — Bấm verify (giữ PENDING_VERIFY — trần local, KHÔNG phải lỗi)

**Hành động:**
```bash
curl -sS -X POST "$GW/api/instances/$IID/domain/verify" \
  -H "Authorization: Bearer $TOKEN" -w "\nHTTP=%{http_code}\n" | jq
```

**✅ Kỳ vọng (PASS):** HTTP **200**. `status` vẫn **`PENDING_VERIFY`** — vì DNS TXT lookup không tìm thấy token khớp trên DNS công cộng cho `school.com`. **Đây là kết quả ĐÚNG, KHÔNG phải bug** (xem cảnh báo đầu file + pre-walk mục 3).

**⚠️ Sad path (đây mới là lỗi thật nếu gặp):**
- HTTP **500** → regression — DNS lookup throw exception không được nuốt đúng. Báo lại.
- Verify mất tới ~10 giây rồi mới trả → DNS egress chậm trong container (pre-walk mục 7). Vẫn PASS nếu cuối cùng trả 200 + PENDING.

**🔍 Verify:** Không có `CERT_PROVISIONING`, không có cert side-effect, không bao giờ đạt VERIFIED local — đó là **GAP-1024** (P1, state machine chưa đầy đủ), đã catalog, KHÔNG block G2.

---

### Bước 4 — Xoá domain (DELETE → 204, về NONE)

**Hành động:**
```bash
curl -sS -X DELETE "$GW/api/instances/$IID/domain" \
  -H "Authorization: Bearer $TOKEN" -w "\nHTTP=%{http_code}\n"
```

**✅ Kỳ vọng (PASS):** HTTP **204** (No Content, body rỗng).

**🔍 Verify DB:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT custom_domain, domain_status FROM instances WHERE id='$IID';"
```
→ `custom_domain=null` (hoặc rỗng), `domain_status=NONE`. Lặp `DELETE` lần 2 → vẫn **204** (idempotent).

---

### Bước 5 — Xác nhận 3 fix inline G1 còn sống

**5a — FM-1 role gate (chặn non-OWNER):** Gọi domain với JWT của user KHÔNG có role OWNER/PLATFORM_ADMIN/ADMIN (ví dụ một STAFF/USER token) → kỳ vọng **403**. (FM-1 thêm `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")` cả 4 endpoint.)

**5b — FM-2 regex multi-label (VN-domain `*.edu.vn`):**
```bash
curl -sS -X POST "$GW/api/instances/$IID/domain" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customDomain":"truong.edu.vn"}' -w "\nHTTP=%{http_code}\n"
```
→ kỳ vọng **200** PENDING_VERIFY (regex cũ reject 3-label, FM-2 đã sửa). Nhớ DELETE lại sau test.

**5c — FM-5 reserved denylist:**
```bash
curl -sS -X POST "$GW/api/instances/$IID/domain" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customDomain":"kitehub.me"}' -w "\nHTTP=%{http_code}\n"
```
→ kỳ vọng **400** (chặn claim domain hệ thống). Thử thêm `app.kitehub.me` → cũng **400**.

## 4. Sad path quét nhanh

| Case | Lệnh / input | Kỳ vọng |
|---|---|---|
| Instance không tồn tại | `GET /api/instances/00000000-0000-0000-0000-000000000000/domain` | **404** (KHÔNG 500) |
| Thiếu `Authorization` | bỏ header `-H "Authorization: ..."` | **401** |
| Domain hệ thống `kitehub.me` | POST body `{"customDomain":"kitehub.me"}` | **400** (FM-5) |
| Domain 3-label VN `*.edu.vn` | POST `{"customDomain":"truong.edu.vn"}` | **200** PENDING (FM-2) |
| Non-OWNER role | gọi với STAFF/USER JWT | **403** (FM-1) |
| `POST /verify` khi chưa có domain | DELETE trước rồi verify | **400** "No domain verification pending" |
| ⚠️ **Cross-tenant** (Owner A → instance Owner B) | gọi domain của instance tenant khác bằng token Owner A | **(SAI) 200** — đây là **GAP-1023 P0 IDOR** còn mở. FM-1 mới chặn theo *role*, CHƯA chặn cross-tenant *ownership*. Nếu bạn confirm được lỗ này → ghi nhận, KHÔNG cần fix vòng G2 (đã có gap). |

## 5. Báo kết quả G2

Sau khi xong (hoặc dừng vì blocker nặng), trả về **một trong 4 trạng thái**:

| Trạng thái | Khi nào dùng | Cần gửi kèm |
|---|---|---|
| ✅ **FULL PASS** | Bước 1-4 happy path PASS (verify giữ PENDING_VERIFY = đúng) + ít nhất 5a/5b/5c + sad path 404/401 PASS | Xác nhận ngắn "Đi hết được" |
| ⚠️ **MOSTLY PASS có cosmetic** | Happy path xong nhưng có gap nhỏ (vd `backupUrl` null, message lỗi chưa chuẩn tiếng Việt, số/format hiển thị lạ) | Liệt kê 1 dòng mỗi cosmetic; tôi vá vòng sau |
| 🔴 **BLOCKING** | Có bước fail nặng: verify trả **500**, gateway 404/503 dai dẳng, DB sai trạng thái, add domain hợp lệ bị 400 oan | Chụp output curl + DB query + bước số mấy. Tôi gom catalog + vá batch |
| ❓ **UNCLEAR** | Không chắc PASS/FAIL, hoặc confirm được GAP-1023 cross-tenant | Ping kèm output + giải thích ngắn |

> Lưu ý: verify giữ `PENDING_VERIFY` là **PASS** (trần local), KHÔNG phải BLOCKING. GAP-1023 (cross-tenant) + GAP-1024 (state machine) là **gap đã biết** — nếu gặp lại, ghi nhận chứ đừng coi là lỗi mới.

Khi nhận báo cáo, tôi flip campaign §4 dòng KH-7: `🔄 walk-pass-pending-human` → `✅ G1+G2 chờ G3 production parity` (nếu PASS) hoặc thêm vòng vá (nếu BLOCKING).

## 6. Khắc phục nhanh + xem trước G3

**Khắc phục nhanh:**

| Hiện tượng | Cách thử trước |
|---|---|
| Gateway 503 / timeout | `docker restart kite-gateway` + đợi 30-60 giây, retry |
| Login trả token rỗng (`${#TOKEN}` = 0) | Kiểm tra `owner.test@test.vn` / `Test@1234` còn đúng; gọi `/api/auth/login` xem raw response |
| POST domain trả 400 tier | Instance chưa PREMIUM/ENTERPRISE — `UPDATE instances SET tier='PREMIUM' WHERE id='$IID';` |
| `backupUrl=https://null.kitehub.me` | Instance thiếu `subdomain` — chọn instance khác (cosmetic, pre-walk mục 8) |
| Verify trả 500 (không phải 200) | Regression DNS lookup — báo lại ngay, đây là blocker thật |
| Domain hợp lệ bị 400 | So regex `DomainSetupRequest` (FM-2 multi-label) — báo lại nếu reject domain hợp lệ |

**Xem trước G3 (production parity, vòng sau):** G2 chạy trên stack local Docker; verify dừng ở `PENDING_VERIFY` vì DNS local. G3 sẽ chạy trên môi trường production-equivalent (AWS EC2 stack đang stopped) + cần verify thêm:

- **Đạt VERIFIED thật:** sở hữu domain thật + thêm TXT record `kitehub-verify={uuid}` đúng → `POST /verify` trả VERIFIED.
- **Cert side-effect + state machine đầy đủ** (`CERT_PROVISIONING` → `VERIFIED`, có cấp cert ACM/Let's Encrypt + đăng ký gateway route) — **GAP-1024**.
- **Cross-tenant ownership binding** (gateway propagate tenant-identity → chặn Owner A thao tác instance Owner B) — **GAP-1023** (sister của GAP-1015/GAP-1019; đề xuất 1 fix wave chung cho cả 3).
- Domain verify gọi DNS công cộng thật + serve traffic qua domain custom.

Hiện ngoài phạm vi G2; ghi nhận để chuẩn bị wave sau.
