# Feedback Submission — Use Cases

**Domain:** In-app feedback widget submission
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

> **Bucket 0 stub status:** use-cases dưới stub form. Bucket F (GAP-542) sẽ enrich theo final FE/BE implementation.

---

## UC-FEEDBACK-001 — User submit feedback từ widget (corner button)

**Actor:** Mọi user (authenticated tenant member HOẶC anonymous public visitor).
**Trigger:** User click nút feedback widget góc dưới phải màn hình → modal mở → fill rating + comment → click "Gửi".
**Endpoint:** `POST /api/v1/feedback`

### Happy path

1. User click widget icon → modal hiện với:
   - 5 stars/emoji selector (1-5)
   - Textarea comment (placeholder: "Cho chúng tôi biết bạn nghĩ gì...")
   - Optional email field (placeholder: "Email (tuỳ chọn — để liên hệ lại)")
   - Category dropdown (Bug / Trải nghiệm / Yêu cầu tính năng / Khác)
   - Hidden honeypot input
   - Nút "Gửi feedback" (disabled until rating + comment ≥5 chars)
2. FE auto-populate `pageUrl` = `window.location.href`.
3. FE call `POST /api/v1/feedback` với JSON body (Authorization header IF authenticated).
4. BE validate: rating in [1..5], comment 5-2000 chars trim, email RFC valid nếu cung cấp, honeypot empty, category in enum.
5. BE attach `userId` + `tenantId` từ JWT nếu có; null nếu anonymous.
6. BE persist `FeedbackSubmission` row với `status=RECEIVED`.
7. BE emit `feedback.received` outbox event.
8. BE return `201 Created` với `{ id, rating, category, createdAt, status }`.
9. FE replace modal content với success message ("Cảm ơn bạn! Phản hồi đã được ghi nhận.") + auto-close sau 3s.

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 4 | `rating` invalid | 400 | `FEEDBACK_INVALID_RATING` | Inline error trên star selector |
| 4 | `comment` blank / <5 / >2000 | 400 | `FEEDBACK_INVALID_COMMENT` | Inline error textarea; show char counter |
| 4 | `email` cung cấp invalid | 400 | `FEEDBACK_INVALID_EMAIL` | Inline error email field |
| 4 | `category` không thuộc enum | 400 | `FEEDBACK_INVALID_CATEGORY` | (rare; FE dropdown gates) — toast generic error |
| 4 | `honeypot` non-empty | 400 | `FEEDBACK_HONEYPOT_FILLED` | (silent bot trap; FE never thấy) |
| (gw) | Rate limit | 429 | `RATE_LIMITED` | Toast "Đã gửi quá nhiều — vui lòng thử lại sau"; disable nút 60s |
| (server) | Unexpected 500 | 500 | `INTERNAL_ERROR` | Toast "Lỗi hệ thống; thử lại sau" + retry button |

### FE behavior notes

- Submit button MUST be `disabled` khi `rating=null` OR `comment.trim().length < 5`.
- Optimistic update KHÔNG dùng (POST result cần xác nhận từ server, no rollback path khả thi cho feedback).
- Sau success, modal auto-close + show toast subtle "Đã gửi" tại corner.
- Widget icon hiển thị badge nhỏ "Đã gửi" trong 24h sau submit (localStorage flag).
