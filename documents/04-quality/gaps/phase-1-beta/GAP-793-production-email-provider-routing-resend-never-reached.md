---
id: GAP-793
title: Production email-provider routing — Resend nhánh send không bao giờ tới
status: PARTIAL
priority: P0
category: Backend
phase: phase-1-beta
completion_pct: 80
created: 2026-05-28
last_verified: 2026-05-28
---

# GAP-793 — Production email-provider routing: Resend không bao giờ được gọi (P0)

## Problem

Production chạy `EMAIL_PROVIDER=resend` (xác nhận tại `kitehub/docker-compose.production.yml:157`,
`documents/02-architecture/env-vars-registry.md`, và `infrastructure/terraform-cloudflare/dns.tf`
tạo SPF + DKIM cho Resend). Nhưng đường gửi email **không bao giờ tới Resend** → mọi email
giao dịch (welcome, trial, beta-invite, staff-invite, DSAR...) trong production hoặc bị reject
hoặc rơi vào spam, vì DNS trỏ về Resend trong khi service thực tế lại gửi qua AWS SES (DKIM SES
chưa được thiết lập trong prod).

Đây là defect P0 ảnh hưởng toàn bộ pipeline email giao dịch khi `email.provider=resend`.

## Root Cause

Hai lỗi wiring cộng lại khiến `ResendEmailService` thành bean chết:

1. **Thiếu nhánh `resend` trong send path.** `SESEmailService.sendEmail()` (~dòng 184) định tuyến:
   `smtp`→SMTP (MailHog), `mock`/mockMode→log, **còn lại→AWS SES**. KHÔNG có nhánh `resend` →
   `provider=resend` rơi xuống nhánh else (SES).

2. **Consumer inject bean cụ thể, không inject interface.** Cả `EmailController` (dòng 34/53) lẫn
   `EmailEventListener` mới (dòng 66/69/123) inject `SESEmailService` cụ thể, không bao giờ inject
   `NotificationChannel`/`EmailSender`. `ResendEmailService` tồn tại + `@ConditionalOnProperty(email.provider=resend)`
   + `implements NotificationChannel` — nhưng vì không ai inject interface nên Resend không bao giờ được gọi.

Đây là lỗi có sẵn từ trước (`EmailController` đã có); `EmailEventListener` (PR #1936) kế thừa lại.
Local hoạt động bình thường vì local dùng `smtp`→MailHog.

### Lý do lịch sử — kiến trúc trôi (architecture drift)

ADR-025 chốt AWS SES là provider email Phase 1 BETA. Sau đó Stream A (deliverability hardening)
adopt Resend (GAP-657 Wave 98) + cấu hình DNS Resend (SPF/DKIM) + đặt `EMAIL_PROVIDER=resend` trong
production. Nhưng lớp send chưa bao giờ được nối lại theo quyết định Resend — quyết định Stream A
(Resend) trôi khỏi mã thực thi (vẫn route SES). Đây chính là nguyên nhân gốc theo
`local-fix-production-parity-check.md` + `outside-in-coverage-trigger.md` §2.1 (architecture-decision
keyword "integration/processor/provider").

## Proposed Fix

Chọn cơ chế Spring idiomatic: **`@Primary EmailProviderRouter implements NotificationChannel, EmailSender`**.

- Interface mới `EmailSender` nâng 2 method producer-facing (`sendTemplatedEmail(EmailRequest)`,
  `sendEmail(to, subject, html)`) lên hợp đồng chung. Cả `SESEmailService` và `ResendEmailService`
  implement nó.
- `EmailProviderRouter` đọc `email.provider`, delegate sang `ResendEmailService` khi `resend`,
  ngược lại sang `SESEmailService` (giữ nguyên branching smtp/ses/mock nội bộ).
- Consumer (`EmailController`, `EmailEventListener`) inject `EmailSender` → nhận `@Primary` router,
  KHÔNG còn inject `SESEmailService` cụ thể.
- `ResendEmailService` được bổ sung `EmailTemplateRenderer` + `BrandingClient` + `sendTemplatedEmail`
  để render template + branding khi `provider=resend` (mirror SES).

### Bean-ambiguity reasoning

- `SESEmailService` luôn là bean (handle smtp/ses/mock) — không đặt conditional để tránh phải khai báo
  4 điều kiện loại trừ + tránh phá wiring `smtp` local.
- `ResendEmailService` giữ `@ConditionalOnProperty(email.provider=resend)` — chỉ là bean cho đúng 1
  giá trị — và router resolve qua `ObjectProvider<ResendEmailService>` nên khi vắng (provider ∈
  {ses, smtp, mock}) là null-safe, không gây lỗi startup.
- Khi `resend` active: 3 bean implement `NotificationChannel` (SES + Resend + router) → `@Primary`
  router là target inject duy nhất, không `NoUniqueBeanDefinitionException`.
- Router giữ `SESEmailService` cụ thể + `ObjectProvider<ResendEmailService>` (KHÔNG inject generic
  `NotificationChannel`) → không self-injection recursion.

## Acceptance Criteria

- [x] `email.provider=resend` → general `NotificationChannel` resolve về router, router delegate sang `ResendEmailService`
- [x] `email.provider` ∈ {ses, smtp, mock} → router delegate sang `SESEmailService`
- [x] Cả `EmailController` và `EmailEventListener` inject `EmailSender` (không inject `SESEmailService` cụ thể)
- [x] Không bean ambiguity startup cho mọi provider value (resend/ses/smtp/mock) — verify qua context test
- [x] `ClassRescheduledEmailService` (consumer chuyên biệt, conditional riêng) KHÔNG bị đổi wiring
- [x] Compile clean dưới strict-warnings
- [x] `@SpringBootTest` context test: resend→ResendEmailService, ses→SESEmailService (88 tests PASS)
- [ ] **Resend live-send verify** — gửi thật qua Resend HTTP API trong production-equivalent → BLOCKED bởi GAP-612 (AWS account suspended); defer tới khi prod/AWS restore
- [ ] Coordinator walk local MailHog (`provider=smtp`) xác minh routing end-to-end

## Why PARTIAL (không DONE)

Theo `feature-ship-runtime-walk-mandate.md` + `pre-handoff-self-test-completeness.md` §2.3:
runtime resend live-send không thể verify cho tới khi AWS/prod restore (GAP-612). Bean routing đã
được verify bằng context test; nhưng email Resend gửi thật cần endpoint production. Gap giữ PARTIAL
~80% (code + IaC surface + test xong; live-send verify defer GAP-612).

## Log

- **2026-05-28:** Gap filed. Defect P0 phát hiện khi review PR #1936 (EmailEventListener kế thừa lỗi
  inject SES cụ thể). Fix: `EmailSender` interface + `@Primary EmailProviderRouter` + nối Resend vào
  cả 2 consumer. 88 tests PASS (gồm `EmailProviderRoutingTest` resend/ses context test). PR SUPERSEDE
  #1936. Live-send verify defer GAP-612 (AWS suspended). Root cause = ADR-025(SES) vs Stream-A(Resend)
  architecture drift.
