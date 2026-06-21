---
audience: dev
domain: email
layer: use-cases
version: 1.0.0
last-updated: 2026-06-21
related-gaps: [GAP-657, GAP-659, GAP-664]
---

# Use Cases — Email Layer

**Scope:** Luồng gửi email transactional qua `kitehub-email` service — 5 critical templates + chính sách header + xử lý lỗi provider. Wave 98 Bucket B1 deliverability hardening.

**Sister layers:** [`rules.md`](rules.md) (Layer 1 — BR-EMAIL-001..007) · [`api-contract.md`](api-contract.md) (Layer 3 — `POST /api/platform/emails/send`).

**Actor chung:** Internal — service producer (server-to-server). Không có UI trực tiếp; người nhận (recipient) tương tác với email trong inbox của họ. Tất cả UC dưới đây gọi cùng một endpoint `POST /api/platform/emails/send`, khác nhau ở `templateName` + `variables`.

---

## UC-EMAIL-001 — Gửi email chào mừng (welcome)

**Actor:** Producer service (vd: flow onboarding sau khi user verify email).
**Tiền điều kiện:** Recipient đã verify email; có `recipientName` + `loginUrl`.
**Trigger:** User hoàn tất xác minh email lần đầu.

**Steps:**
1. Producer gọi `POST /api/platform/emails/send` với `templateName="welcome"` + `variables{ recipientName, loginUrl, docsUrl, unsubscribeUrl }`.
2. Server render template Thymeleaf `welcome` với tone `FORMAL_SAFE_DEFAULT` ("Kính gửi anh/chị {name}," — BR-EMAIL-004 Wave 98).
3. Server đính header `From` / `Reply-To` / `List-Unsubscribe` + `List-Unsubscribe-Post` (BR-EMAIL-002/003) + body multipart HTML + plain-text (BR-EMAIL-001).
4. Dispatch qua provider theo `email.provider` (BR-EMAIL-006).
5. Trả `200` + `{ messageId, status:"SENT", sentAt }`.

**Kết quả:** Recipient nhận email chào mừng có thể unsubscribe one-click.
**Errors:** `400` validation (thiếu `to`/`subject`/`templateName`, email sai format). Provider lỗi → `200` + `status:"FAILED"` (xem UC-EMAIL-006).
**Email/FE behavior:** Mail client strip HTML → render plain-text fallback (BR-EMAIL-001). Header `List-Unsubscribe` cho phép Gmail/Outlook hiện nút "Unsubscribe".

---

## UC-EMAIL-002 — Gửi lời mời Beta (beta-invite)

**Actor:** Producer (flow kích hoạt chương trình Beta cho center owner).
**Tiền điều kiện:** Có `orgName`, `inviteUrl` (token), `verificationCode`, `expiresAt`.
**Trigger:** Admin/hệ thống mời một trung tâm tham gia Beta.

**Steps:**
1. Producer gọi endpoint với `templateName="beta-invite"` + `variables{ orgName, inviteUrl, verificationCode, expiresAt, unsubscribeUrl }`.
2. Render tone `FORMAL_SAFE_DEFAULT` (đối tượng là chủ trung tâm — authority figure, tránh tone thân mật burn trust per BR-EMAIL-004).
3. Đính đủ header chuẩn (gồm `List-Unsubscribe` vì không phải password-reset — BR-EMAIL-003).
4. Trả `200` + `status:"SENT"`.

**Kết quả:** Owner nhận email mời Beta kèm mã xác minh + link accept có thời hạn (`expiresAt`).
**Errors:** `400` validation; `200`+`FAILED` khi provider lỗi.
**Email behavior:** `expiresAt` hiển thị giờ Việt Nam; link `inviteUrl` chứa token một lần.

---

## UC-EMAIL-003 — Gửi mã xác minh email (email-verification / OTP)

**Actor:** Producer (flow đăng ký / xác minh email).
**Tiền điều kiện:** Có `recipientName`, `verificationCode` (6 chữ số), `verifyUrl`, `expiresInMinutes` (15).
**Trigger:** User yêu cầu xác minh email.

**Steps:**
1. Gọi endpoint `templateName="email-verification"` + `variables{ recipientName, verificationCode, verifyUrl, expiresInMinutes:15 }`.
2. Render template + đính header chuẩn (gồm `List-Unsubscribe`).
3. Trả `200` + `status:"SENT"`.

**Kết quả:** User nhận OTP 6 số (hết hạn 15 phút) + link verify.
**Errors:** `400` validation; `200`+`FAILED` provider lỗi.
**Email behavior:** Mã OTP hiển thị nổi bật; cả HTML + plain-text đều chứa mã (BR-EMAIL-001) để client plain-mode vẫn đọc được.

---

## UC-EMAIL-004 — Gửi email đặt lại mật khẩu (password-reset — security mail)

**Actor:** Producer (flow quên mật khẩu).
**Tiền điều kiện:** Có `recipientName`, `resetUrl` (token), `expiresInMinutes` (30).
**Trigger:** User bấm "Quên mật khẩu".

**Steps:**
1. Gọi endpoint `templateName="password-reset"` + `variables{ recipientName, resetUrl, expiresInMinutes:30 }`.
2. Render template; **đặc biệt: KHÔNG đính header `List-Unsubscribe`** — đây là essential security mail, user không được opt-out khi đang reset (BR-EMAIL-003 exception).
3. Vẫn giữ `From` / `Reply-To` + body multipart (BR-EMAIL-001/002/005).
4. Trả `200` + `status:"SENT"`.

**Kết quả:** User nhận link reset mật khẩu (hết hạn 30 phút), không có nút unsubscribe.
**Errors:** `400` validation; `200`+`FAILED` provider lỗi.
**Email behavior:** Đây là loại email duy nhất trong 5 critical templates bị loại trừ khỏi `List-Unsubscribe` (BR-EMAIL-003) — verify bằng test header policy.

---

## UC-EMAIL-005 — Gửi lời mời nhân sự (invite-staff)

**Actor:** Producer (Owner mời Manager/Teacher vào trung tâm).
**Tiền điều kiện:** Có `recipientName`, `ownerName`, `tenantName`, `role`, `inviteUrl` (token), `expiresAt` ("7 ngày kể từ thời điểm gửi").
**Trigger:** Owner mời nhân sự qua UI quản lý trung tâm.

**Steps:**
1. Gọi endpoint với `templateName="invite-staff"` + `variables{ recipientName, ownerName, tenantName, role, inviteUrl, expiresAt }` + (tùy chọn) `instanceId`/`tenantId` để fetch branding.
2. Render template với context tenant (tên trung tâm + người mời) + tone `FORMAL_SAFE_DEFAULT`.
3. Đính header chuẩn (gồm `List-Unsubscribe`).
4. Trả `200` + `status:"SENT"`.

**Kết quả:** Nhân sự nhận email mời có ngữ cảnh tenant (ai mời, vào trung tâm nào, vai trò gì) + link accept hết hạn sau 7 ngày.
**Errors:** `400` validation; `200`+`FAILED` provider lỗi.
**Email behavior:** Nếu truyền `instanceId`/`tenantId` → branding của tenant được fetch + áp vào email (logo/màu).

---

## UC-EMAIL-006 — Xử lý lỗi provider (envelope 200 + status=FAILED)

**Actor:** Producer (bất kỳ UC nào ở trên khi provider gặp sự cố).
**Tiền điều kiện:** Request hợp lệ nhưng provider (SES/Resend) trả lỗi (rate limit, HTTP error).
**Trigger:** Provider dispatch thất bại.

**Steps:**
1. Producer gọi endpoint như bình thường.
2. Provider lỗi → service KHÔNG ném 500; trả `200` + `{ messageId:null, status:"FAILED", errorMessage:"..." }`.
3. Producer đọc `status` để quyết định retry / log; retry bền vững đi qua outbox (theo BR-EMAIL-006 + outbox dispatcher).

**Kết quả:** Producer biết email thất bại (không bị "im lặng nuốt lỗi") nhưng caller không vỡ luồng vì exception.
**Errors:** Provider down kéo dài → contract đích là `503 EMAIL_503_PROVIDER_DOWN` (api-contract.md §Error codes), nhưng `EmailController` hiện trả `200`+`FAILED` envelope — refactor về 503 tracked qua **GAP-572**.
**Email behavior:** N/A (server-to-server). Khi chạy `email.provider=mock`/`smtp` (dev/MailHog) → `status:"MOCK"`, không gửi thật.

---

## Tham chiếu chéo

- Tone register hiện tại: Wave 98 ALL templates render `FORMAL_SAFE_DEFAULT`; per-tone variant (`*.formal.html` / `*.informal.html`) deferred Wave 99+ (BR-EMAIL-004, GAP-659 §Step 2).
- Scheduler-driven email (day-7 survey, trial warnings) phải emit metric `email.scheduler.{job}.sent_count` + alarm silent-fail (BR-EMAIL-007) — flow async, không liệt kê UC riêng ở đây (out-of-scope Wave 98 concrete wiring per GAP-657 §Step 5).

## Related

- [`rules.md`](rules.md) — BR-EMAIL-001..007
- [`api-contract.md`](api-contract.md) — `POST /api/platform/emails/send` + 5 template schemas + header policy
- GAP-657 (deliverability) · GAP-659 (tone) · GAP-664 (3-layer backfill — this file)
- `EmailType.java` — enum canonical cho `templateName`

## Log

- **2026-06-21** — use-cases.md created (GAP-664 — 3-layer completeness backfill; email đã có rules.md + api-contract.md, thiếu use-cases.md). 6 UC-EMAIL grounded trong api-contract `POST /api/platform/emails/send` + 5 critical templates (welcome / beta-invite / email-verification / password-reset / invite-staff) + BR-EMAIL-001..007. UC-EMAIL-004 ground exception password-reset không có `List-Unsubscribe`; UC-EMAIL-006 ground envelope 200+FAILED (GAP-572 refactor về 503).
