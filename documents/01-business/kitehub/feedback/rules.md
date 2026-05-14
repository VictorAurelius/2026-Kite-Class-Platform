# Feedback Submission — Business Rules

**Domain:** In-app feedback widget submission (Wave 78 — GAP-542)
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)
**Config prefix:** `kitehub.feedback`

File này document business values cho feedback widget flow. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Bucket 0 stub status:** rules dưới là stub form. Bucket F (GAP-542) sẽ enrich theo final implementation.

---

## BR-FEEDBACK-001 — Rating scale 1-5 (Likert-like)

- **Value:** `rating` field MUST be integer trong `[1, 2, 3, 4, 5]`. 1 = "rất tệ", 5 = "rất tốt". Mid-point 3 = neutral.
- **Source:** Industry standard Likert scale 5-point (common in SaaS feedback widgets — Hotjar, Productboard, Intercom). Wave 78 outside-in benchmark.
- **Rationale:** Scale 5 đủ resolution cho sentiment + đơn giản cho user mobile (5 emoji/star ngắn). 7-point (NPS-style) overkill cho in-app quick feedback. 1-3 / 4-5 split tự nhiên cho heatmap admin review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** N/A — rating value không phải PII.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: ≥20% submissions tại rating extremes (1 hoặc 5) → reconsider scale granularity.
- **Code reference:** (planned) `FeedbackSubmission.rating` field validation + `@Min(1) @Max(5)`.

## BR-FEEDBACK-002 — Public submit allowed, PII opt-in only

- **Value:** Endpoint `POST /api/v1/feedback` accept unauthenticated submit (public). Email field optional. Anonymous submit valid + lưu DB với `userId=null`, `tenantId=null`.
- **Source:** Wave 78 plan §1 Brainstorm Q4 — "feedback widget cần kênh feedback NHANH; barrier login = friction".
- **Rationale:** Beta tenant đôi khi muốn report bug nhanh khi đang gặp lỗi auth/JWT (ironic case where login broken). Public submit ensure bug report kênh không bao giờ block bởi auth state. Email optional → user opt-in nếu muốn follow-up, KHÔNG required.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Compliance scout, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 11: email collection chỉ khi user explicit cung cấp (opt-in by typing in field, không pre-fill). Comment text có thể chứa PII nếu user paste accidentally → server-side scrub trước khi log per `logs-format-standard.md` (Bucket F responsibility).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: PDPL implementing-decree mới về public-submit channel.
- **Code reference:** (planned) `FeedbackController.submit()` với `@PreAuthorize` permissive (no auth required).

## BR-FEEDBACK-003 — Comment length 5-2000 chars

- **Value:** `comment` field MUST be 5-2000 chars sau trim. Empty / <5 chars rejected (avoid noise). >2000 rejected (avoid abuse + DB bloat).
- **Source:** *(placeholder — Wave 78 plan no explicit data; informed gut + general SaaS feedback widget benchmark)*
- **Rationale:** *(placeholder — Bucket F sẽ enrich với data sau Phase 1 BETA)*
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14). Informed-gut value; re-review post-Phase-1.
- **Compliance check:** N/A.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** (planned) `@Size(min=5, max=2000)` trên `FeedbackSubmissionRequest.comment`.

---

## Config

Tracking `@Value` wiring status per GAP-555 (Wave 78 Business Logic audit P0 — 15+ config keys documented không wired qua `@Value`).

| Key | Default | Purpose | Wired |
|-----|---------|---------|:-----:|
| `kitehub.feedback.rating-range-min` | `1` | Lower bound for rating | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.rating-range-max` | `5` | Upper bound for rating | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.comment-min-chars` | `5` | Min comment length | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.comment-max-chars` | `2000` | Max comment length | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.public-rate-limit-per-min-per-ip` | `10` | Public rate limit at gateway | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.auth-rate-limit-per-min-per-user` | `30` | Authenticated rate limit | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.categories` | `BUG,USABILITY,FEATURE_REQUEST,GENERAL` | Enum whitelist | 🆕 Wave 79 Bucket A target |
| `kitehub.feedback.survey-cron` | (existing) | Day-7 survey cron schedule | ✅ Wave 78 |

**Wave 79 Bucket A scope (GAP-555):** Add `@Value` injection cho 7 unwired keys ở column "Wired" trên. Survey-cron đã wired Wave 78. Total: 14 remaining keys cross 4 domain modules (feedback/onboarding/beta-status/support) — Bucket A grep target `@Value("\${kitehub.feedback...")` post-Bucket-A → ≥7 matches in feedback module alone.

Config keys nằm trong `application.yml` BE module (Bucket F).
