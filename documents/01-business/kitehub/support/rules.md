# Support Tickets — Business Rules

**Domain:** Support inquiry / ticket submission (Wave 78 — GAP-540)
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)
**Config prefix:** `kitehub.support`

File này document business values cho support ticket flow. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Bucket 0 stub status:** rules dưới là stub form. Bucket F (GAP-540) sẽ enrich theo final implementation.

---

## BR-SUPPORT-001 — In-house route MVP Phase 1, defer external

- **Value:** Phase 1 BETA dùng in-house BE endpoint `POST /api/v1/support-tickets`. Ticket lưu DB table `support_tickets`. KHÔNG dùng Zendesk/Freshdesk/Intercom external tool.
- **Source:** Wave 78 plan §1 Brainstorm — "Phase 1 BETA cohort 5-10 tenant, ticket volume thấp, manual handling đủ; external tool $15-30/user/month cost overhead không cần thiết".
- **Rationale:** Cost-benefit Phase 1 ngả về in-house: minimal infra (1 table, 1 controller), data ownership preserved, migration path optional sau (BE proxy hoặc direct widget). External tool ROI tăng khi volume >50 tickets/tháng — predicted reach Phase 2/3 (P3 medium-center, K-12) per `release-1-plan-2026.md` §3.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Ops + Cost-control, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — PDPL 2023: `email` + `body` chứa PII; lưu DB cần retention rule. Inherit retention chung của tenant data (per BR-TENANT-DATA-RETENTION) — 36 tháng post-account-deletion. Body PII scrub không apply (user explicit cung cấp cho support purpose; opt-in implicit).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: >50 tickets/month sustained → evaluate Zendesk migration; user complaint về thiếu auto-reply / ticket tracking UI.
- **Code reference:** (planned) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/support/`.

## BR-SUPPORT-002 — Email required (admin reply route)

- **Value:** `email` field MUST be provided + valid RFC-5321 cho mọi submit. Khác với feedback (email optional), support REQUIRES email vì admin cần reply.
- **Source:** Wave 78 plan §3 Scope — "support channel discoverability" implies follow-up reply expected.
- **Rationale:** *(placeholder — Bucket F sẽ enrich với UX data sau)*. Support workflow inherently 2-way (user submit → admin reply → user clarify) → email là minimum contact channel. Anonymous submit không match support semantics.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 (explicit consent): user typing email field = explicit opt-in cho support follow-up purpose. Privacy notice tại support form footer cần ghi: "Email được lưu để phản hồi yêu cầu hỗ trợ; xem Chính sách Bảo mật."
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** (planned) `@Email @NotBlank` trên `SupportTicketRequest.email`.

---

## Config

| Key | Default | Purpose |
|-----|---------|---------|
| `kitehub.support.subject-min-chars` | `5` | Min subject length |
| `kitehub.support.subject-max-chars` | `200` | Max subject length |
| `kitehub.support.body-min-chars` | `10` | Min body length |
| `kitehub.support.body-max-chars` | `5000` | Max body length |
| `kitehub.support.public-rate-limit-per-min-per-ip` | `5` | Public rate limit |
| `kitehub.support.auth-rate-limit-per-min-per-user` | `20` | Authenticated rate limit |
| `kitehub.support.categories` | `AUTH_ISSUE,BILLING,BUG,FEATURE_REQUEST,DATA_ISSUE,OTHER` | Category enum |
| `kitehub.support.priorities` | `LOW,NORMAL,HIGH,URGENT` | Priority enum |
| `kitehub.support.ticket-number-prefix` | `KH-` | Human-readable ticket number prefix |
| `kitehub.support.sla-first-response-hours` | `24` | Advertised first-response SLA Phase 1 |

Config keys nằm trong `application.yml` BE module (Bucket F).
