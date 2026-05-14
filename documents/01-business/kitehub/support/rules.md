# Support Tickets — Business Rules

**Domain:** Support inquiry / ticket submission (Wave 78 — GAP-540)
**Last verified:** 2026-05-14 (Wave 79 Bucket E — scope clarification per GAP-556)
**Config prefix:** `kitehub.support`

File này document business values cho support ticket flow. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **⚠️ Wave 78 scope: DISCOVERABILITY ONLY (per GAP-556)** — Wave 78 chỉ ship:
> - Footer `mailto:support@kitehub.me` link discoverable trên public + dashboard layout
> - Tài liệu mention `support@kitehub.me` channel
>
> **CHƯA implement trong Wave 78 / 79:**
> - Backend table `support_tickets` (mô tả ở rules.md dưới là **PLANNED**, không phải đã ship)
> - `POST /api/v1/support-tickets` endpoint
> - Admin ticket queue management UI / triage workflow
> - SLA tracking, auto-reply, status updates
>
> **Khi nào implement?** Full ticket/queue management deferred Wave 80+ — tracked cùng GAP-040 (support impersonation BE). Tham khảo `release-1-plan-2026.md` §3 cho phase scope.
>
> **Mục đích note:** Tránh future reader hiểu nhầm bảng `support_tickets` mô tả ở §1-§2 đã được implement. Hiện tại chỉ là **design intent + planned BE scope** cho release sau.

> **Bucket 0 stub status:** rules dưới là stub form cho design intent. Bucket F (GAP-540) đã ship discoverability slice; full implementation defer Wave 80+.

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

Tracking `@Value` wiring status per GAP-555 (Wave 78 Business Logic audit P0).

| Key | Default | Purpose | Wired |
|-----|---------|---------|:-----:|
| `kitehub.support.subject-min-chars` | `5` | Min subject length | 🆕 Wave 79 Bucket A target |
| `kitehub.support.subject-max-chars` | `200` | Max subject length | 🆕 Wave 79 Bucket A target |
| `kitehub.support.body-min-chars` | `10` | Min body length | 🆕 Wave 79 Bucket A target |
| `kitehub.support.body-max-chars` | `5000` | Max body length | 🆕 Wave 79 Bucket A target |
| `kitehub.support.public-rate-limit-per-min-per-ip` | `5` | Public rate limit | 🆕 Wave 79 Bucket A target |
| `kitehub.support.auth-rate-limit-per-min-per-user` | `20` | Authenticated rate limit | 🆕 Wave 79 Bucket A target |
| `kitehub.support.categories` | `AUTH_ISSUE,BILLING,BUG,FEATURE_REQUEST,DATA_ISSUE,OTHER` | Category enum | 🆕 Wave 79 Bucket A target |
| `kitehub.support.priorities` | `LOW,NORMAL,HIGH,URGENT` | Priority enum | 🆕 Wave 79 Bucket A target |
| `kitehub.support.ticket-number-prefix` | `KH-` | Human-readable ticket number prefix | 🆕 Wave 79 Bucket A target |
| `kitehub.support.sla-first-response-hours` | `24` | Advertised first-response SLA Phase 1 | 🆕 Wave 79 Bucket A target |

**Wave 79 Bucket A scope (GAP-555):** Add `@Value` injection cho 10 keys above ở module `kitehub-subscription/support/service`. Total across 4 domains = 7 (feedback) + 2 (onboarding) + 3 (beta-status) + 10 (support) = **22 keys** to wire (exceeds plan §3 estimate "15+"); Bucket A grep verify post-fix: `grep -c "@Value(.\${kitehub" kitehub/kitehub-subscription/src/main/java -r` → ≥22 matches expected.

Config keys nằm trong `application.yml` BE module (Bucket F).
