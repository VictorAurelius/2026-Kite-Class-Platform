---
id: GAP-812
title: Custom domain DNS TXT verify thật + SSL provisioning + state machine hoàn thiện
status: PARTIAL
priority: P2
phase: phase-1-beta
domain: Backend
created: 2026-05-29
---

# GAP-812 — Custom domain DNS verify + SSL provisioning completion

## Problem

Luồng custom domain (tenant gắn domain riêng vào instance KiteClass, vd `lop.skyedu.vn`)
đã có khung đầy đủ ở `kitehub-subscription` nhưng **không hoàn chỉnh end-to-end** — domain
không bao giờ verify được + không có HTTPS cho domain custom:

1. **DNS TXT verify là stub**: `DomainService.checkDnsTxtRecord()`
   (`kitehub/kitehub-subscription/.../service/DomainService.java:193-210`) chỉ gọi
   `InetAddress.getAllByName(domain)` (resolve A record), KHÔNG đọc TXT record, rồi
   **luôn `return false`** (comment trong code: "TXT verification not yet fully
   implemented"). Hệ quả: nhánh `verifyCustomDomain()` luôn rơi vào else → status giữ
   nguyên `PENDING_VERIFY` mãi mãi, không bao giờ chuyển `VERIFIED`.

2. **mockMode mặc định true**: `DomainVerificationConfig.mockMode = true`
   (`config/DomainVerificationConfig.java:28`) → kể cả khi `checkDnsTxtRecord` trả về
   false, code chỉ log "Mock mode: DNS not resolvable, keeping PENDING" → tenant không
   bao giờ thấy domain active dù đã gắn TXT record đúng.

3. **Không có SSL/cert provisioning**: Không tìm thấy bất kỳ logic ACM / Let's Encrypt /
   Cloudflare cert nào cho custom domain. Domain dù VERIFIED cũng KHÔNG có chứng chỉ TLS →
   browser báo cảnh báo bảo mật → không dùng được trong production.

4. **State machine chưa hoàn chỉnh**: `FAILED` được khai báo trong enum
   `Instance.DomainStatus` nhưng KHÔNG có đường dẫn nào set `FAILED` (comment "Timeout
   logic would be implemented in a scheduled job" — chưa có job). Không có retry/re-verify
   path từ `FAILED` → `PENDING_VERIFY`, không có polling/webhook tự động.

**Phạm vi ảnh hưởng**: chỉ tier PREMIUM/ENTERPRISE (`Instance.canUseCustomDomain()` →
`PricingTier.allowsCustomDomain()`). KHÔNG chặn Phase 1 BETA (BETA chủ yếu dùng subdomain
`{tenant}.kiteclass.com` qua gateway `findByCustomDomain` — backup URL luôn hoạt động). Vì
vậy priority P2: là tính năng tier cao, không phải blocker.

## Root Cause

1. PR gốc dựng khung state machine + controller + DTO + entity field đầy đủ nhưng để DNS
   TXT lookup ở dạng stub (`return false`) vì Java built-in `InetAddress` không đọc được
   TXT record — cần thư viện DNS (dnsjava) hoặc JNDI `InitialDirContext`.
2. SSL provisioning cho domain do tenant tự chọn đòi hỏi tích hợp ngoài (ACM / Cloudflare
   for SaaS / Let's Encrypt) — phụ thuộc quyết định hạ tầng (ADR), không nằm trong scope
   PR khung ban đầu.
3. `FAILED` + timeout + retry để lại cho "scheduled job" chưa được tạo.

## Proposed Fix (thiết kế)

### Phần A — DNS TXT verification thật

Thay thế `checkDnsTxtRecord()` bằng TXT record lookup thật. Dùng **JNDI
`InitialDirContext`** (`com.sun.jndi.dns.DnsContextFactory`) — có sẵn trong JDK, KHÔNG cần
thêm dependency. (Dnsjava là phương án thay thế nếu cần resolver tùy biến / DNS-over-HTTPS,
nhưng JNDI đủ cho v1.)

```java
private boolean checkDnsTxtRecord(String domain, String expectedToken) {
    // Convention: TXT record đặt tại _kitehub-verify.{domain}, fallback apex {domain}
    String[] candidates = { "_kitehub-verify." + domain, domain };
    for (String host : candidates) {
        for (String txt : lookupTxtRecords(host)) {
            // expectedToken = "kitehub-verify={uuid}" — so khớp chính xác (strip quotes)
            if (txt.contains(expectedToken)) return true;
        }
    }
    return false;
}

private List<String> lookupTxtRecords(String host) {
    var env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    env.put(Context.PROVIDER_URL, "dns:");
    try {
        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(host, new String[]{"TXT"});
        Attribute txt = attrs.get("TXT");
        // ... trả về list giá trị TXT (mỗi giá trị strip dấu " bao quanh)
    } catch (NameNotFoundException e) {
        return List.of(); // record chưa tồn tại — verify chưa thành công, không phải lỗi
    }
}
```

Điểm chính:
- Token format giữ nguyên `kitehub-verify={uuid}` (đã có sẵn trong `initiateCustomDomain`).
- Convention TXT record `_kitehub-verify.{domain}` (chuẩn ngành cho domain verification,
  tránh đụng SPF/DKIM ở apex) + fallback apex để linh hoạt.
- `checkDnsTxtRecord` trả `false` (không throw) khi record chưa có → verify chưa thành
  công, để state machine xử lý timeout/retry (Phần C).
- Thêm config `kitehub.domain.verification.dns-timeout-ms` (mặc định 5000) + log rõ ràng
  số TXT record tìm thấy để debug.

### Phần B — SSL/cert provisioning (khuyến nghị: **Cloudflare for SaaS**)

Phân tích 3 phương án theo stack hiện tại (AWS `ap-southeast-1` + Cloudflare DNS/proxy per
ADR-025/ADR-031):

| Phương án | Cơ chế | Ưu | Nhược | Phù hợp |
|---|---|---|---|---|
| **Cloudflare for SaaS (Custom Hostnames)** | Tenant trỏ CNAME về Cloudflare; CF tự cấp + gia hạn cert (SSL on behalf of) | API gọn (1 POST tạo custom_hostname → CF lo cert + renew + edge TLS); domain verify + cert chung 1 luồng (TXT/CNAME pre-validation); $0 tự gia hạn; khớp Cloudflare đã dùng | Phụ thuộc Cloudflare; có phí theo custom hostname ở scale lớn | ⭐ **Khuyến nghị** |
| **AWS ACM + ALB SNI** | Import/request cert vào ACM, gắn cert lên ALB qua SNI | Cùng AWS; cert miễn phí | ALB giới hạn ~25 cert/listener (cần SNI multiplexing thủ công); ACM DNS validation cho domain KHÔNG do mình quản lý DNS rất rắc rối; gia hạn phải re-validate | Trung bình — vướng giới hạn cert + validation |
| **Let's Encrypt autocert (Caddy/cert-manager)** | Tự host ACME client cấp cert | $0; full control | Phải tự vận hành ACME + lưu/renew cert + reverse proxy đầu vào; gánh nặng ops cao cho solo-dev | Thấp — ops overhead lớn |

**Khuyến nghị: Cloudflare for SaaS** vì (1) dự án đã dùng Cloudflare proxy → tận dụng hạ
tầng sẵn; (2) gộp luôn domain-verify + cert-issue + auto-renew vào 1 luồng API, giảm phần
state machine phải tự lo cert; (3) không vướng giới hạn cert của ALB; (4) phù hợp solo-dev
(ít ops). Thiết kế:

- Thêm `CloudflareCustomHostnameClient` (Adapter, per `design-patterns.md` §2 — vendor
  isolation, domain type trả về không leak CF type).
- Khi `verifyCustomDomain` thành công TXT → gọi CF API tạo `custom_hostname` (CNAME-based
  pre-validation) → CF cấp cert. Thêm `domainStatus = CERT_PROVISIONING` (enum mới) giữa
  `VERIFIED` và domain thật sự live.
- API key Cloudflare lưu AWS Secrets Manager + terraform IaC declaration (per
  `local-fix-production-parity-check.md` — env var mới phải có prod surface parity).
- Backup URL `{subdomain}.kiteclass.com` luôn hoạt động song song khi cert đang provision.

### Phần C — State machine hoàn thiện

- **Timeout → FAILED**: scheduled job (`@Scheduled`) quét instance `PENDING_VERIFY` quá
  `timeoutHours` (mặc định 48h) → set `FAILED` + `migrationFailureReason`-style lý do.
- **FAILED → re-verify**: cho phép `initiateCustomDomain` / một endpoint `re-verify` reset
  `FAILED` → `PENDING_VERIFY` (re-generate token, tenant gắn lại TXT).
- **Polling tự động**: scheduled job quét `PENDING_VERIFY` định kỳ (vd mỗi 10 phút) gọi
  `checkDnsTxtRecord` → tự verify mà không cần tenant bấm "Verify" thủ công (giữ luôn cả
  endpoint manual verify hiện có).
- **CERT_PROVISIONING → VERIFIED**: poll Cloudflare custom_hostname status → khi cert
  `active` thì flip sang trạng thái live cuối.
- Cập nhật enum `Instance.DomainStatus`: `NONE → PENDING_VERIFY → CERT_PROVISIONING →
  VERIFIED` + `FAILED` (từ bất kỳ bước nào, có đường re-verify về `PENDING_VERIFY`).
- Tắt `mockMode` ở production profile (`application-production.yml`: `mockMode=false`),
  giữ `true` cho dev/test.

## Acceptance Criteria

- [ ] `checkDnsTxtRecord` đọc TXT record thật qua JNDI tại `_kitehub-verify.{domain}` +
      fallback apex, khớp token `kitehub-verify={uuid}` → trả `true` khi đúng.
- [ ] Testcontainers / integration test: gắn TXT record mock (hoặc test với domain thật có
      TXT) → `verifyCustomDomain` flip `PENDING_VERIFY → VERIFIED` (không còn kẹt PENDING).
- [ ] `mockMode=false` ở `application-production.yml`; `true` ở dev/test profile.
- [ ] ADR ghi quyết định SSL provisioning (chốt Cloudflare for SaaS) + lý do so 3 phương án.
- [ ] `CloudflareCustomHostnameClient` (Adapter) tạo custom_hostname + poll cert status;
      Cloudflare API key lưu Secrets Manager + terraform IaC declaration (prod parity).
- [ ] Enum `DomainStatus` thêm `CERT_PROVISIONING`; state machine có timeout→FAILED +
      FAILED→re-verify + polling job tự verify.
- [ ] RST walk (per `feature-ship-runtime-walk-mandate`): tenant PREMIUM gắn domain → thêm
      TXT record → verify → cert provision → browse `https://{domain}` có HTTPS hợp lệ +
      route đúng tenant. Verify 3 lớp (DB status + curl HTTPS cert + browser) trước DONE.
- [ ] Backup URL `{subdomain}.kiteclass.com` vẫn hoạt động khi cert đang provision.
- [ ] BE `mvn test` PASS; sweep callers `checkDnsTxtRecord` / `DomainStatus` enum (prod +
      test) per `api-contract-change-caller-sweep` khi đổi enum.

## Related

- Entity: `kitehub/kitehub-platform/.../domain/entity/Instance.java` (DomainStatus enum +
  custom domain fields).
- Service: `kitehub/kitehub-subscription/.../service/DomainService.java` (stub
  `checkDnsTxtRecord` line 193-210).
- Config: `kitehub/kitehub-subscription/.../config/DomainVerificationConfig.java`
  (`mockMode=true` mặc định).
- Controller: `kitehub/kitehub-subscription/.../controller/DomainController.java` (4
  endpoint đầy đủ).
- Gateway: `findByCustomDomain(host)` đã wired — route theo custom domain khi VERIFIED.
- ADR-025 (AWS Singapore Free Tier) + ADR-031 (FE self-host AWS EC2) — context hạ tầng.

## Log

- **2026-05-29:** Gap created. Xác nhận hiện trạng qua đọc DomainService/DomainController/
  DomainVerificationConfig/Instance: DNS TXT verify là stub (`return false` luôn) + mockMode
  mặc định true → verify kẹt PENDING; không có SSL provisioning; state machine thiếu
  FAILED/timeout/retry/polling. Thiết kế 3 phần: (A) DNS TXT thật qua JNDI InitialDirContext
  tại `_kitehub-verify.{domain}`; (B) SSL — khuyến nghị Cloudflare for SaaS (tận dụng CF
  proxy sẵn có, gộp verify+cert+renew, tránh giới hạn ACM/ALB); (C) state machine +
  CERT_PROVISIONING + scheduled timeout/polling job. P2 vì custom domain là tier
  PREMIUM/ENTERPRISE, backup subdomain luôn hoạt động → không chặn Phase 1 BETA.

## Outside-in findings (3-agent audit 2026-05-29)

Điều chỉnh design trước khi lock (per `outside-in-coverage-trigger.md`):

- **Benchmark (đổi approach):** ngành dùng **CNAME (subdomain) + A record (apex) cho routing**; TXT/Delegated-DCV chỉ để verify OWNERSHIP (tách khỏi routing). **Delegated DCV** (CNAME `_acme-challenge` đặt 1 lần → auto-renew mãi) > manual TXT rotate. Cloudflare for SaaS confirm fit.
- **Persona (P0):** **apex domain** — CNAME bất hợp lệ trên root (vendor VN không hỗ trợ ALIAS) → PHẢI hỗ trợ A-record HOẶC `_kitehub-verify.{domain}` subdomain TXT. Thiếu **hướng dẫn DNS per-vendor VN** (Mat Bao/PA Vietnam/Nhân Hòa) = P0 friction.
- **Persona/Benchmark (P1):** status real-time + nút "Kiểm tra lại"; **SSL-pending fallback page** (serve qua backup subdomain HTTPS trong lúc chờ cert — KHÔNG để user gặp `ERR_CERT`); status badge Pending→Active + webhook notify.
- **Failure-mode (P1):** CAA record + DNSSEC chặn cấp cert (bài học Shopify — cần cảnh báo chủ động); user xoá TXT post-verified → scheduled re-verify; cert renew fail → health-check + alert + auto-fallback.
- **Meta:** 3 doc kiến trúc (`domain-management.md`, `ssl-automation.md`, brief) mâu thuẫn verify method → reconcile 1 nguồn TRƯỚC khi build UI (cross-ref GAP-813).

## Update — Wave tenant-domain-1 Bucket D (2026-06-01) — Status PARTIAL

### Shipped trong Bucket D PR

**Phần A — DNS TXT verify thật (DONE):**
- `DnsTxtLookupService.java` (NEW) — JNDI `InitialDirContext` với `com.sun.jndi.dns.DnsContextFactory`, lookup `_kitehub-verify.{domain}` (preferred) + apex fallback, timeout 5s, returns `false` (never throws) on NameNotFound/fail
- `DomainService.checkDnsTxtRecord()` rewritten — delegate sang `DnsTxtLookupService.verifyTxtRecord()`; xóa stub `InetAddress.getAllByName()` + `return false`
- `DomainServiceTest` updated với `@Mock DnsTxtLookupService` injection + 2 test mới (`verifyCustomDomain_dnsLookupMiss_returnsPending` + `verifyCustomDomain_dnsTxtMatch_returnsVerified`)
- `DnsTxtLookupServiceTest` (NEW) — 6 unit tests (match-at-subdomain / match-at-apex / no-records / token-mismatch / blank-domain / blank-token) qua subclass override để tránh real DNS trong CI
- Total: 18/18 tests PASS (`./mvnw -pl kitehub-subscription test -Dtest='DomainServiceTest,DnsTxtLookupServiceTest'`)

**Phần B v1 — ACM cert scaffold (DEFERRED apply per `release-deploy-standard.md` §9):**
- `infrastructure/terraform-aws/acm-tenant-domains.tf` (NEW) — `aws_acm_certificate.tenant_domain` resource với `for_each` over `var.tenant_custom_domains` (DNS validation), outputs `tenant_acm_cert_arns` + `tenant_acm_validation_records` (CNAME instructions cho tenant DNS setup)
- ASCII-only descriptions per `aws-sg-description-ascii.md`
- `terraform validate` PASS (existing warnings pre-existing, not from new file)
- Apply DEFERRED — terraform run-time decision per dev trigger; current default `tenant_custom_domains = []` (no-op nếu chạy apply)

**Phần C v1.1 — DomainStatus extended:**
- `Instance.DomainStatus` enum thêm `CERT_PROVISIONING` value giữa `PENDING_VERIFY` và `VERIFIED` — state machine sẵn sàng cho v1.1 SSL provisioning automation
- v1.0 hiện tại verify→VERIFIED trực tiếp (Cloudflare for SaaS / ACM automation chưa wire); enum value future-proof

**Doc layer (3-layer per CLAUDE.md mandate):**
- `documents/01-business/kitehub/custom-domain/rules.md` (NEW) — BR-DOMAIN-001..012 + state machine ASCII diagram + config keys
- `documents/01-business/kitehub/custom-domain/use-cases.md` (NEW) — UC-DOMAIN-001..004 (Owner đăng ký + gỡ + re-verify + scheduled polling future)
- `documents/01-business/kitehub/custom-domain/api-contract.md` (NEW) — 4 endpoints (initiate/verify/delete/get) + DomainStatus enum + error codes
- `documents/05-guides/operations/custom-domain-verify-runbook.md` (NEW) — Owner-facing + 5 DNS provider VN guide (Mat Bao / PA Vietnam / Nhân Hòa / Cloudflare / Namecheap) + troubleshooting (CAA / propagate / cert error) + ops team queries

### Deferred to follow-up GAP-816

- **Phần B v2 SSL provisioning automation:** Lambda subscriber outbox event `domain.verified` → trigger ACM cert request / Cloudflare Custom Hostname API; live apply `acm-tenant-domains.tf`
- **Phần C scheduler:** `@Scheduled` polling job `DomainVerificationScheduler` (timeout 48h → FAILED; periodic DNS re-check mỗi 10 phút auto-verify mà không cần tenant trigger manual)
- **Phần B Cloudflare for SaaS integration:** `CloudflareCustomHostnameClient` adapter + secret in Secrets Manager + terraform IaC declaration
- **CERT_PROVISIONING → VERIFIED polling cert status:** Lambda subscribe ACM/Cloudflare cert.status → flip enum when cert active
- **SSL-pending fallback page UI:** FE banner "cert đang cấp..." khi user truy cập custom domain trong CERT_PROVISIONING state

→ Status flip OPEN → PARTIAL (completion ~40%) per `gap-done-discipline.md` §3 PARTIAL exit ramp. Cargo cult avoided — no false DONE flip.

## Log

- **2026-06-01 (Wave tenant-domain-1 Bucket D):** Status flip OPEN → PARTIAL ~40% completion. Phần A DNS TXT verify thật DONE (JNDI implementation); Phần B v1 terraform ACM scaffold DONE (apply deferred per `release-deploy-standard.md` §9); Phần C enum extended (CERT_PROVISIONING) + state machine doc. 3-layer biz docs + runbook + 18/18 tests PASS. Phần B SSL automation + Phần C scheduler deferred → GAP-816. Per `gap-done-discipline.md` §3 PARTIAL exit ramp + follow-up gap filed.
