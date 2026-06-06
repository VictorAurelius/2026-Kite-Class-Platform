# Pre-Walk Persona Simulation — KH-7 Custom Domain / Domain Management

**Date:** 2026-06-06
**Flow:** KH-7 — Owner gắn custom domain → verify DNS TXT → status → remove
**Service:** kitehub-subscription (`DomainController` + `DomainService` + `DnsTxtLookupService`)
**Mandated by:** `.claude/rules/pre-walk-persona-simulation-mandate.md`
**Type:** Prediction only — KHÔNG fix gì. Walker dùng để act trước khi walk live.

---

## Tóm tắt 2 cờ bắt buộc

- **(i) Gateway route cho `/api/instances/**`:** ✅ **TỒN TẠI** — route `kitehub-instance-domain-verify` tại `application.yml:166-174` (`Path=/api/instances/**` → `http://kitehub-subscription:8080`). JWT được validate + inject `X-User-Id`/`X-User-Roles` qua `JwtAuthenticationGatewayFilter` (GlobalFilter, HS512). `/api/instances/**` KHÔNG nằm trong `isPublicPath` → cần JWT hợp lệ. **KHÔNG phải walk-blocker.** (Thiếu `Authorization` header → pass-through → subscription default-deny → 401.)
- **(ii) DNS verification có chạy được local không:** ❌ **KHÔNG đạt VERIFIED local.** `DnsTxtLookupService.verifyTxtRecord` làm real JNDI DNS lookup (`com.sun.jndi.dns.DnsContextFactory`). KHÔNG có dev-bypass ép VERIFIED. Domain test (vd `school.com`) không có TXT record khớp token → trả `false` → state ở lại `PENDING_VERIFY`. `mockMode=true` (default) cũng giữ PENDING (cả 2 nhánh mock/non-mock đều set PENDING khi không khớp). **Walk chỉ tới được add→PENDING_VERIFY; VERIFIED không reach được trừ khi walker sở hữu domain thật + thêm đúng TXT `kitehub-verify={uuid}` tại `_kitehub-verify.{domain}` hoặc apex.**

---

## Bảng failure modes (xếp theo confidence × walk-impact)

### 1. [P0, HIGH] IDOR + thiếu ownership/role binding — bất kỳ user nào quản lý domain của instance bất kỳ
- **(a) Where:** `DomainController.java` (toàn bộ, không có `@PreAuthorize` trên 4 method) + `DomainService.java:49,99,148,170` (`findInstanceOrThrow(instanceId)` chỉ load by id, KHÔNG verify caller sở hữu `{id}`).
- **(b) Symptom:** Owner A (JWT hợp lệ) gọi `POST/GET/DELETE /api/instances/{B's id}/domain` → 200, đọc/ghi/xóa domain của tenant B. Default `anyRequest().authenticated()` chỉ chặn anonymous; KHÔNG ràng buộc caller↔`{id}`, KHÔNG yêu cầu role OWNER → cả STAFF/USER cũng làm được. Đúng lớp P0 đã confirm ở KH-5 (GAP-1015) + KH-6 (GAP-1019).
- **(c) Pre-walk check:** `grep -n "PreAuthorize\|instanceId.*equals\|getCurrentUser\|X-User-Id" DomainController.java DomainService.java` → cả hai trống về ownership binding. Live: login Owner A, lấy instanceId của B từ DB (`psql -c "select id from instances limit 2"`), `curl -H "Authorization: Bearer <A_token>" /api/instances/{B_id}/domain` → kỳ vọng (sai) 200.

### 2. [P1, HIGH] Walk-blocker mềm — regex từ chối CHÍNH domain ví dụ `school.example.com`
- **(a) Where:** `DomainSetupRequest.java:22-25` regex `^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\.[a-zA-Z]{2,}$`.
- **(b) Symptom:** Regex chỉ khớp **đúng 2 label** (`label.tld`). `school.example.com` (2 dấu chấm) → 400. Cũng reject `localhost`, `truong.edu.vn`, `my.school.edu.vn` — tức đa số domain VN thực tế (3 label `*.edu.vn`) đều bị chặn. Happy-path documented tự fail.
- **(c) Pre-walk check:** `python3 -c "import re;print(bool(re.match(r'^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\.[a-zA-Z]{2,}$','school.example.com')))"` → `False`. Walker phải dùng domain 2-label (vd `school.com`) cho happy path, và catalog regex bug riêng.

### 3. [P1, HIGH] VERIFIED không reach được local — không có dev-bypass
- **(a) Where:** `DomainService.java:99-140` + `DnsTxtLookupService.java:57-76`.
- **(b) Symptom:** `POST /domain/verify` làm real DNS TXT lookup; domain test không có TXT khớp → `verified=false` → set lại `PENDING_VERIFY`. Không có config ép VERIFIED. Walk dừng ở PENDING. `FAILED` cũng không bao giờ set (timeout job chưa implement — comment `DomainService.java:127`).
- **(c) Pre-walk check:** `grep -n "isMockMode\|VERIFIED\|return true" DomainService.java DnsTxtLookupService.java DomainVerificationConfig.java` → xác nhận không có bypass. Live: add domain → `POST /verify` → kỳ vọng status vẫn `PENDING_VERIFY` (đây là kết quả đúng, không phải bug; chỉ là trần của walk).

### 4. [P1, HIGH] State machine không đầy đủ — `CERT_PROVISIONING` không bao giờ set; không có cert side-effect
- **(a) Where:** `Instance.java:81-83` enum `{NONE, PENDING_VERIFY, CERT_PROVISIONING, VERIFIED, FAILED}` + javadoc nói `NONE→PENDING_VERIFY→CERT_PROVISIONING→VERIFIED`; nhưng `DomainService.verifyCustomDomain` nhảy thẳng PENDING→VERIFIED (`:119`), bỏ qua `CERT_PROVISIONING`.
- **(b) Symptom:** Không có bước cấp cert (ACM/Let's Encrypt), không gọi gateway route registration, không gọi Cloudflare API. VERIFIED ≠ domain thật sự phục vụ traffic. Drift doc↔code.
- **(c) Pre-walk check:** `grep -n "CERT_PROVISIONING\|FAILED\|acm\|certificate\|cloudflare" DomainService.java` → 0 hit (ngoài enum). Xác nhận side-effect vắng mặt.

### 5. [P1, HIGH] Không chặn reserved domain — claim được `kitehub.me` / `kiteclass.com`
- **(a) Where:** `DomainService.initiateCustomDomain` (`:49-87`) — chỉ check tier + uniqueness, không có reserved/denylist.
- **(b) Symptom:** `POST /api/instances/{id}/domain` body `{"customDomain":"kitehub.me"}` → 200 (regex khớp apex 2-label). Tenant claim được domain hệ thống → abuse/phishing risk.
- **(c) Pre-walk check:** `grep -ni "reserved\|denylist\|blocklist\|kitehub.me\|kiteclass" DomainService.java` → 0 hit. Live: thử add `kitehub.me` → kỳ vọng (sai) 200 PENDING.

### 6. [P1, HIGH — pre-walk seed] Tier gate chặn instance không phải PREMIUM/ENTERPRISE
- **(a) Where:** `DomainService.java:55-60` `instance.canUseCustomDomain()` → `tier.allowsCustomDomain()`.
- **(b) Symptom:** Nếu seed instance là TRIAL/FREE/BASIC → bước 1 trả **400** "Custom domain is only available for PREMIUM and ENTERPRISE tiers". Cả flow chặn ngay từ add.
- **(c) Pre-walk check:** `psql -c "select id, tier, subdomain, custom_domain, domain_status from instances;"` → đảm bảo có ít nhất 1 instance tier PREMIUM/ENTERPRISE để walk. Nếu không, `UPDATE instances SET tier='PREMIUM' WHERE id=...` trước khi walk.

### 7. [P2, MEDIUM] DNS lookup latency / egress trong Docker container
- **(a) Where:** `DnsTxtLookupService.java:88-128` — 2 candidate host × timeout 5000ms × retries 1.
- **(b) Symptom:** Container không có DNS egress (port 53) hoặc domain timeout → `POST /verify` có thể mất tới ~10s rồi trả PENDING (exception nuốt, `:116`). Walk chậm, không hang vĩnh viễn.
- **(c) Pre-walk check:** `docker exec kitehub-subscription sh -c "nslookup -type=txt google.com"` (hoặc `getent hosts`) → xác nhận container resolve được DNS. Nếu không, verify sẽ luôn timeout→PENDING (kết quả vẫn đúng, chỉ chậm).

### 8. [P2, MEDIUM] `getSubdomain()` null → backupUrl `https://null.kiteclass.com`
- **(a) Where:** `DomainService.buildResponse` (`:207`) `"https://" + instance.getSubdomain() + ".kiteclass.com"`.
- **(b) Symptom:** Seed instance không có subdomain → response field `backupUrl` = `https://null.kiteclass.com` (không NPE, nhưng xấu/sai). Cosmetic.
- **(c) Pre-walk check:** `psql -c "select id, subdomain from instances where tier in ('PREMIUM','ENTERPRISE');"` → đảm bảo subdomain non-null.

### 9. [P2, MEDIUM] `verify` trên domain đã VERIFIED → 400 (không idempotent)
- **(a) Where:** `DomainService.java:104-110` — nếu status != `PENDING_VERIFY` throw `IllegalArgumentException` → 400.
- **(b) Symptom:** Sau khi (giả định) đạt VERIFIED, gọi lại `POST /verify` → 400 "No domain verification pending". Re-verify một domain đã verified không idempotent.
- **(c) Pre-walk check:** Sad-path step — sau verify (dù PENDING), gọi `POST /verify` lần 2 khi domain ở trạng thái khác PENDING → quan sát 400.

### 10. [P2, MEDIUM] Re-initiate cùng domain → token MỚI ghi đè, TXT cũ vô hiệu
- **(a) Where:** `DomainService.java:73-81` — mỗi `initiate` sinh `kitehub-verify={UUID.randomUUID()}` mới + overwrite.
- **(b) Symptom:** User add domain → nhận token1 → add lại cùng domain → token2 (token1 chết). Nếu user đã thêm TXT token1, verify sẽ fail. UX gap (không cảnh báo "đã có pending").
- **(c) Pre-walk check:** `POST /domain` 2 lần liên tiếp cùng domain → so `verifyToken` 2 response khác nhau.

### 11. [P3, MEDIUM] Validation invalid instanceId / no-domain / delete-when-none đã map đúng (không 500)
- **(a) Where:** `GlobalExceptionHandler.java` — `EntityNotFoundException→404` (`:43-50`), `IllegalArgumentException→400` (`:79-86`), `MethodArgumentNotValid→400` (`:100-115`).
- **(b) Symptom (kỳ vọng ĐÚNG):** instanceId không tồn tại → **404**; verify khi chưa set domain → **400**; delete khi NONE → 204 (idempotent, set NONE/null lần nữa). Đây là baseline để phân biệt: nếu thấy **500** ở các case này = regression mới.
- **(c) Pre-walk check:** `curl /api/instances/00000000-0000-0000-0000-000000000000/domain` → kỳ vọng 404 (không 500). `DELETE` trên instance NONE → kỳ vọng 204.

### 12. [P3, LOW] Schema không drift — đã align
- **(a) Where:** `V12__add_custom_domain.sql` (`domain_verify_token VARCHAR(255)`, `domain_verified_at TIMESTAMP`, `domain_status VARCHAR(50) DEFAULT 'NONE'`) + V1 `custom_domain VARCHAR(255)` vs `Instance.java:90-112` (`@Enumerated(EnumType.STRING)` trên `domainStatus`, length khớp).
- **(b) Symptom (kỳ vọng ĐÚNG):** Không có drift — enum STRING, độ dài cột đủ (`PENDING_VERIFY`=13 < 50; token `kitehub-verify=`+UUID = 50 < 255). Khác KC-5/6/7 — flow này schema OK.
- **(c) Pre-walk check:** `docker exec kite-postgres psql -U <u> -d kitehub_subscription -c "\d instances"` → xác nhận 4 cột domain tồn tại trên schema Flyway thật (không chỉ entity).

---

## Hướng dẫn nhanh cho walker

1. **Seed đúng:** đảm bảo instance tier `PREMIUM`/`ENTERPRISE` + subdomain non-null (mục 6, 8).
2. **Dùng domain 2-label** (vd `school.com`) cho happy path — KHÔNG dùng `school.example.com` (mục 2).
3. **Kỳ vọng trần PENDING_VERIFY** — không reach VERIFIED local (mục 3). Catalog, đừng coi là walk-fail.
4. **Confirm IDOR P0 sống** (mục 1) bằng cross-instance curl — đây là finding quan trọng nhất.
5. **Sad paths:** invalid instanceId→404, verify-no-pending→400, reserved domain→(sai)200 (mục 5, 11).

**Đầu ra dự đoán:** 1 P0 (IDOR) + 4 P1 (regex / VERIFIED-ceiling / state-machine / reserved-domain) + 1 P1 seed-gate + 5 P2/P3. Gateway route OK (không walk-blocker); DNS không đạt VERIFIED local.
