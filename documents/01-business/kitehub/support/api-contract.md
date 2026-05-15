# Support Tickets — API Contract

**Domain:** Support inquiry / ticket submission (Wave 78 — GAP-540)
**Source-of-truth controller:** (planned, deferred Wave 80+) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/support/controller/SupportTicketController.java`
**Last verified:** 2026-05-14 (Wave 79 Bucket E — scope clarification per GAP-556)

> **⚠️ Wave 78 scope: DISCOVERABILITY ONLY (per GAP-556)** — endpoints document dưới là **planned future scope**, KHÔNG phải đã implement. Wave 78 chỉ ship footer `mailto:support@kitehub.me` discoverability. Full BE controller + `support_tickets` table + admin queue UI deferred Wave 80+ (tracked cùng GAP-040 support impersonation BE). Future readers: KHÔNG tưởng nhầm `POST /api/v1/support-tickets` đã hoạt động.

This contract là source-of-truth cross-layer cho Wave 78 Bucket F, consumed by:
- FE Bucket F (GAP-540) — Footer "Liên hệ hỗ trợ" link → modal hoặc page `/support` → submit form
- BE Bucket F (GAP-540) — `SupportTicketController` + `SupportTicket` entity (in-house route MVP)
- MSW handler `kitehub-frontend/src/test/msw/handlers/support.ts` (this PR — Bucket 0)

---

## Design choice — in-house MVP vs external (Zendesk-like)

**Phase 1 BETA: in-house route MVP.** Lý do:
- 5-10 beta tenants → ticket volume thấp, manual handling đủ.
- Zendesk/Freshdesk subscription ~$15-30/user/month → cost overhead không cần thiết cho Phase 1.
- Phase 2/3 (P3 medium-center, K-12) khi ticket volume tăng → đánh giá lại migration tới external tool.

**External option (Wave 79+ decision):** Nếu chuyển sang Zendesk/Freshdesk, endpoint shape có thể stay same (in-house BE acts as proxy), HOẶC FE direct call provider widget. Quyết định defer to Wave 79+ post-Phase-1.

---

## Endpoints

### POST /api/v1/support-tickets

**Use case:** UC-SUPPORT-001 — User submit support inquiry (subject + body + email)
**Auth:** Public (unauthenticated allowed) — user có thể gặp lỗi auth/JWT cần report nhanh. Authenticated submit attach `userId` + `tenantId` từ JWT.

**Request body (`SupportTicketRequest`):**
```json
{
  "subject": "Không thể đăng nhập sau khi reset password",
  "body": "Tôi đã reset password qua email link, nhưng login form vẫn báo 'Sai mật khẩu'. Đã thử 3 lần.",
  "email": "owner@example.edu.vn",
  "category": "AUTH_ISSUE",
  "priority": "NORMAL",
  "honeypot": ""
}
```

**Field constraints:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `subject` | string | yes | 5-200 chars trim, non-blank, UTF-8 |
| `body` | string | yes | 10-5000 chars trim, non-blank, UTF-8 |
| `email` | string | yes | RFC-5321 valid, ≤320 chars. Required vì admin cần reply (anonymous không có route reply) |
| `category` | enum | optional | `AUTH_ISSUE` \| `BILLING` \| `BUG` \| `FEATURE_REQUEST` \| `DATA_ISSUE` \| `OTHER`. Default `OTHER` |
| `priority` | enum | optional | `LOW` \| `NORMAL` \| `HIGH` \| `URGENT`. Default `NORMAL`. Admin re-evaluate sau khi đọc |
| `honeypot` | string | yes (empty) | MUST equal `""` (anti-bot trap) |

**Response 201 Created (`SupportTicketResponse`):**
```json
{
  "id": "ticket-uuid-v4",
  "ticketNumber": "KH-2026-00042",
  "subject": "Không thể đăng nhập sau khi reset password",
  "category": "AUTH_ISSUE",
  "priority": "NORMAL",
  "status": "OPEN",
  "createdAt": "2026-05-14T09:30:00Z"
}
```

**Field semantics:**

| Field | Mô tả |
|-------|------|
| `ticketNumber` | Human-readable format `KH-YYYY-NNNNN` (KiteHub prefix + year + sequence). User reference khi follow-up qua email. |
| `status` | `OPEN` \| `IN_PROGRESS` \| `WAITING_USER` \| `RESOLVED` \| `CLOSED`. Khởi tạo luôn `OPEN`. |

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `SUPPORT_INVALID_SUBJECT` | `subject` blank, <5, hoặc >200 chars |
| 400 | `SUPPORT_INVALID_BODY` | `body` blank, <10, hoặc >5000 chars |
| 400 | `SUPPORT_INVALID_EMAIL` | `email` missing hoặc không valid RFC-5321 |
| 400 | `SUPPORT_INVALID_CATEGORY` | `category` không thuộc enum |
| 400 | `SUPPORT_INVALID_PRIORITY` | `priority` không thuộc enum |
| 400 | `SUPPORT_HONEYPOT_FILLED` | `honeypot` non-empty (silent bot trap) |
| 429 | `RATE_LIMITED` | Per-IP / per-user rate limit exceeded |

---

## Side effects

- Submit thành công → emit `support.ticket.created` event qua outbox.
  - Subscriber 1: Send confirmation email tới `email` field — "Đã nhận yêu cầu hỗ trợ. Ticket #KH-2026-00042. Chúng tôi sẽ phản hồi trong 24h." (Bucket E email template scope).
  - Subscriber 2 (Wave 79+): Notify platform admin via Slack/email khi `priority=URGENT`.
- KHÔNG auto-reply tới `email` nếu submit lỗi (avoid email noise).

---

## Rate limits

- Public POST: 5 req/min/IP. Lower than feedback vì support ticket "heavier" + ít abuse case legit.
- Authenticated POST: 20 req/min/user.
- Vượt → 429 `RATE_LIMITED`.

---

## External option note (Wave 79+ decision)

Nếu migrate sang Zendesk/Freshdesk:
- Option A — BE proxy: endpoint shape giữ nguyên, BE forward tới external API. FE không đổi.
- Option B — Direct widget: FE embed Zendesk Web Widget; remove endpoint. Trade-off: loss of in-house DB → mất control + ownership data.

Default tentative: **Option A** (BE proxy) — preserve data ownership + endpoint shape stable.

---

## Related

- BR-SUPPORT-001..002: `documents/01-business/kitehub/support/rules.md`
- UC-SUPPORT-001: `documents/01-business/kitehub/support/use-cases.md`
- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
