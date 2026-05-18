---
audience: dev
domain: email
layer: business-rules
version: 1.0.0
last-updated: 2026-05-18
related-gaps: [GAP-657, GAP-659, GAP-543]
---

# Business Rules — Email Layer

**Scope:** Cấu hình, tone register, header policy, plain-text fallback policy cho `kitehub-email` service. Wave 98 Bucket B1 deliverability hardening.

**Source-of-truth:** rule này canonical cho email content + send behavior; code references `kitehub-email` Java service + Thymeleaf templates.

---

## BR-EMAIL-001 — Plain-text fallback mandatory cho 5 critical templates

Mỗi email transactional thuộc 5 critical types BẮT BUỘC ship BOTH HTML + plain-text body (multipart/alternative). Mail client strip HTML (Gmail Promotions, Outlook plain mode) render plain-text fallback.

**5 critical templates:**

| Template stem | Use case |
|---|---|
| `welcome` | Onboarding sau verify email |
| `beta-invite` | Beta program activation |
| `email-verification` | 6-digit OTP send |
| `password-reset` | Đặt lại mật khẩu |
| `invite-staff` | Owner mời Manager/Teacher join trung tâm |

**Verify:** mỗi template HTML có sibling `.txt` cùng folder `kitehub/kitehub-email/src/main/resources/templates/emails/`.

**Wave 98 trạng thái:** 5/5 templates có siblings.

---

## BR-EMAIL-002 — Reply-To header bắt buộc

Mọi email gửi đi PHẢI có header `Reply-To: support@kitehub.me` (configurable qua `aws.ses.reply-to-email`). Tenant reply route về support inbox, KHÔNG về no-reply sender. Cải thiện trust + giảm support ticket ngầm.

**Config key:** `aws.ses.reply-to-email` (default `support@kitehub.me`).

---

## BR-EMAIL-003 — List-Unsubscribe header bắt buộc cho non-critical email

Email transactional **trừ password-reset** PHẢI có header `List-Unsubscribe: <mailto:unsubscribe@kitehub.me>, <https://kitehub.me/unsubscribe?token={token}>` + `List-Unsubscribe-Post: List-Unsubscribe=One-Click`.

**Exception:** `password-reset` SKIP unsubscribe header vì là essential security mail (user không thể opt-out khi đang reset password).

**Config keys:**
- `aws.ses.unsubscribe-mailto` (default `unsubscribe@kitehub.me`)
- `aws.ses.unsubscribe-url-template` (default `https://kitehub.me/unsubscribe?token={token}`)

---

## BR-EMAIL-004 — Tone register theo recipient role (Wave 98 simplification)

`Tone` enum (com.kitehub.email.api.Tone) resolve từ recipient role:

| Role string (case-insensitive) | Tone |
|---|---|
| `PLATFORM_ADMIN`, `CENTER_OWNER`, `P2_CENTER_OWNER` | FORMAL_AUTHORITY |
| `CENTER_MANAGER`, `P3_CENTER_MANAGER` | SEMI_FORMAL_PEER |
| `TEACHER`, `P1_SOLO_TEACHER`, `SOLO_TEACHER` | INFORMAL_FRIEND |
| Anonymous / unknown / null | FORMAL_SAFE_DEFAULT |

**Wave 98 default:** ALL templates render dùng **FORMAL_SAFE_DEFAULT** salutation ("Kính gửi anh/chị {name}," + closing "Trân trọng, Đội ngũ KiteHub") để tránh trust-burning khi gửi tone informal cho authority figure (chị Hằng P2 Owner, 45 tuổi).

**Wave 99+ scope:** Per-tone variant templates (`welcome.formal.html` / `welcome.informal.html` / etc.) — see GAP-659 §Step 2 TODO marker trong `EmailTemplateRenderer.resolveTemplatePath()`.

**Vietnamese business email convention source:** Misa eInvoice templates + Talkpal VN formal email guide + external benchmark B-NEW-3 (audit 2026-05-18 Wave 98 Cluster B).

---

## BR-EMAIL-005 — Sender identity

`From: KiteHub <no-reply@kitehub.me>` (configurable qua `aws.ses.from-email` + `aws.ses.from-name`).

**KHÔNG được phép:**
- Personal email (`vannkite@outlook.com`) làm sender
- Different domain (e.g., `notifications@example.com`) — vi phạm DKIM / SPF / DMARC alignment

---

## BR-EMAIL-006 — Provider routing

`email.provider` config key:

| Value | Implementation | Use case |
|---|---|---|
| `ses` | `SESEmailService` (AWS SDK v2 SesClient) | Production default |
| `resend` | `ResendEmailService` (HTTPS API) | ADR-025 Stream A alternative |
| `smtp` | `SESEmailService.sendViaSMTP()` (JavaMailSender) | Local dev (MailHog) |
| `mock` | `SESEmailService` mock branch | Test |

**Activation:** dùng `@ConditionalOnProperty(name = "email.provider")` để chỉ load active provider bean — tránh wire 2 providers cùng lúc.

---

## BR-EMAIL-007 — Scheduler observability

Mọi scheduled email job (e.g., day-7 feedback survey, trial-expiration warnings) PHẢI:
1. Emit metric `email.scheduler.{job_name}.sent_count` mỗi run
2. Emit metric `email.scheduler.{job_name}.failure_count` mỗi run
3. CloudWatch alarm `email-scheduler-silent-fail` fire khi `sent_count == 0` qua 2 consecutive periods (catches silent broken cron)

**Wave 98 trạng thái:** alarm name reserved trong Terraform; concrete metric wiring DEFERRED — tracked via GAP-657 §Step 5 follow-up.

---

## Related

- **GAP-657** — Email layer hardening (deliverability — this rule §BR-EMAIL-001/002/003)
- **GAP-659** — Persona-tone split (this rule §BR-EMAIL-004)
- **GAP-543** — Email content audit (parent — content review)
- **ADR-025** — Vendor selection (Stream A = Resend)
- **`postgres-specific-type-testcontainers.md`** — testing standards
- **`audit-service-isolation.md`** — applies to email audit log services
