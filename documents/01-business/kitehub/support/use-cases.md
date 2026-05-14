# Support Tickets — Use Cases

**Domain:** Support inquiry / ticket submission
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

> **Bucket 0 stub status:** use-cases dưới stub form. Bucket F (GAP-540) sẽ enrich theo final FE/BE implementation.

---

## UC-SUPPORT-001 — User submit support ticket qua footer link

**Actor:** Mọi user (authenticated tenant member HOẶC anonymous public visitor).
**Trigger:** User click link "Liên hệ hỗ trợ" trong footer / dashboard nav → modal hoặc page `/support` mở → fill form → submit.
**Endpoint:** `POST /api/v1/support-tickets`

### Happy path

1. User click "Liên hệ hỗ trợ" → modal/page mở với:
   - Subject input (placeholder: "Tóm tắt vấn đề bạn đang gặp")
   - Body textarea (placeholder: "Mô tả chi tiết — bước thực hiện, kết quả mong đợi, kết quả thực tế")
   - Email input (required, placeholder: "Email để chúng tôi phản hồi")
   - Category dropdown (Auth Issue / Billing / Bug / Feature Request / Data Issue / Khác)
   - Priority dropdown (LOW / NORMAL / HIGH / URGENT) — default NORMAL
   - Hidden honeypot input
   - Privacy notice link: "Email được lưu để phản hồi yêu cầu — xem Chính sách Bảo mật"
   - Nút "Gửi yêu cầu" (disabled until subject + body + email validation pass)
2. FE call `POST /api/v1/support-tickets` (Authorization header IF authenticated).
3. BE validate: subject 5-200, body 10-5000, email RFC valid, category/priority in enum, honeypot empty.
4. BE attach `userId` + `tenantId` từ JWT nếu có.
5. BE generate `ticketNumber` format `KH-YYYY-NNNNN` (sequence từ DB sequence/serial).
6. BE persist `SupportTicket` row với `status=OPEN`.
7. BE emit `support.ticket.created` outbox event (subscriber: confirmation email + admin notification).
8. BE return `201 Created` với `{ id, ticketNumber, subject, category, priority, status, createdAt }`.
9. FE replace form với success screen: "Đã nhận yêu cầu hỗ trợ! Mã ticket: **KH-2026-00042**. Chúng tôi sẽ phản hồi qua email trong 24h."
10. (Async) Bucket E confirmation email gửi tới `email` field — chứa ticket number + summary.

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 3 | `subject` invalid | 400 | `SUPPORT_INVALID_SUBJECT` | Inline error subject field |
| 3 | `body` invalid | 400 | `SUPPORT_INVALID_BODY` | Inline error body + char counter |
| 3 | `email` missing/invalid | 400 | `SUPPORT_INVALID_EMAIL` | Inline error email field |
| 3 | `category`/`priority` invalid | 400 | `SUPPORT_INVALID_*` | (rare; FE dropdown gates) toast generic error |
| 3 | `honeypot` filled | 400 | `SUPPORT_HONEYPOT_FILLED` | (silent bot trap) |
| (gw) | Rate limit | 429 | `RATE_LIMITED` | Toast "Quá nhiều yêu cầu — vui lòng thử lại sau"; disable submit 60s |
| (server) | Unexpected 500 | 500 | `INTERNAL_ERROR` | Toast "Lỗi hệ thống; vui lòng thử lại hoặc gửi email trực tiếp tới support@kitehub.me" |

### FE behavior notes

- Submit button MUST be `disabled` cho đến khi: subject ≥5, body ≥10, email valid RFC.
- Sau success, hiển thị ticket number prominently (user copy/paste để reference).
- Modal sau success có CTA "Đóng" + "Gửi yêu cầu khác" (reset form).
- Footer link "Liên hệ hỗ trợ" hiển thị trên mọi page (Bucket F footer responsibility).
- Page route `/support` (nếu page-based thay vì modal) cũng public — KHÔNG redirect khi unauthenticated.
