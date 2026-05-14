# Feedback Submission — API Contract

**Domain:** In-app feedback widget submission (Wave 78 — GAP-542)
**Source-of-truth controller:** (planned) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/feedback/controller/FeedbackController.java`
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

This contract là source-of-truth cross-layer cho Wave 78 Bucket F, consumed by:
- FE Bucket F (GAP-542) — Feedback widget component (corner button + modal) submit form
- BE Bucket F (GAP-542) — `FeedbackController` + `FeedbackSubmission` entity + Flyway migration `V[N]__create_feedback_submissions_table.sql`
- MSW handler `kitehub-frontend/src/test/msw/handlers/feedback.ts` (this PR — Bucket 0)

---

## Endpoints

### POST /api/v1/feedback

**Use case:** UC-FEEDBACK-001 — User submit feedback từ in-app widget (rating + free-text + optional context)
**Auth:** Public (unauthenticated submit allowed) — beta tenant có thể test feedback widget từ landing page; authenticated submit attach `userId` + `tenantId` từ JWT. Rate-limit per IP enforced tại gateway.

**Request body (`FeedbackSubmissionRequest`):**
```json
{
  "rating": 4,
  "comment": "Onboarding checklist rất rõ ràng, nhưng phần import data chưa có hướng dẫn cụ thể.",
  "email": "feedback@example.edu.vn",
  "pageUrl": "https://kitehub.me/dashboard/onboarding",
  "category": "USABILITY",
  "honeypot": ""
}
```

**Field constraints:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `rating` | integer 1-5 | yes | MUST be in `[1, 2, 3, 4, 5]`. 1 = "rất tệ", 5 = "rất tốt". |
| `comment` | string | yes | 5-2000 chars, non-blank sau trim. UTF-8 (Vietnamese). |
| `email` | string | optional | Nếu cung cấp → RFC-5321 valid, ≤320 chars. Trống khi anonymous submit. |
| `pageUrl` | string | optional | URL FE đang ở khi submit (FE auto-populate). ≤2000 chars. |
| `category` | enum | optional | `BUG` \| `USABILITY` \| `FEATURE_REQUEST` \| `GENERAL`. Default `GENERAL`. |
| `honeypot` | string | yes (empty) | MUST equal `""` (anti-bot trap; FE hidden input). |

**Auth context (auto-attached by BE if Bearer JWT present):**
- `userId` từ JWT subject
- `tenantId` từ JWT claim

**Response 201 Created (`FeedbackSubmissionResponse`):**
```json
{
  "id": "feedback-uuid-v4",
  "rating": 4,
  "category": "USABILITY",
  "createdAt": "2026-05-14T09:00:00Z",
  "status": "RECEIVED"
}
```

**Lưu ý:** response KHÔNG echo back `comment` / `email` / `pageUrl` để giảm payload + tránh leak qua browser cache. Server-side log lưu đầy đủ cho admin review.

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `FEEDBACK_INVALID_RATING` | `rating` không thuộc `[1..5]` hoặc missing |
| 400 | `FEEDBACK_INVALID_COMMENT` | `comment` blank, <5 chars, hoặc >2000 chars |
| 400 | `FEEDBACK_INVALID_EMAIL` | `email` cung cấp nhưng không valid RFC-5321 |
| 400 | `FEEDBACK_INVALID_CATEGORY` | `category` không thuộc enum |
| 400 | `FEEDBACK_HONEYPOT_FILLED` | `honeypot` non-empty (silent reject từ FE perspective; surface trong test) |
| 429 | `RATE_LIMITED` | Per-IP gateway rate limit exceeded |

---

## Side effects

- Submit thành công → emit `feedback.received` event qua outbox (per `design-patterns.md` §3.5). Subscriber: Bucket F day-7 survey trigger logic + future Slack/email notification to platform admin (Wave 79+ scope).
- Email field nếu cung cấp → KHÔNG auto-subscribe user; chỉ lưu cho follow-up của admin nếu user explicitly tick "Liên hệ lại với tôi" (FE-only flag, không gửi BE riêng — implicit qua presence của `email`).

---

## Rate limits

- POST: 10 req/min/IP tại gateway. Vượt → 429.
- Per authenticated user (nếu có JWT): 30 req/min/user. Vượt → 429.

---

## Related

- BR-FEEDBACK-001..003: `documents/01-business/kitehub/feedback/rules.md`
- UC-FEEDBACK-001: `documents/01-business/kitehub/feedback/use-cases.md`
- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- Source migration (planned): `V[N]__create_feedback_submissions_table.sql`
