# Beta Status — Business Rules

**Domain:** Public beta status page content (Wave 78 — GAP-539)
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)
**Config prefix:** `kitehub.beta-status`

File này document business values cho beta status page. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Bucket 0 stub status:** rules dưới là stub form. Bucket B (GAP-539) sẽ enrich theo final implementation.

---

## BR-BETA-STATUS-001 — Static MVP, manual update qua git PR

- **Value:** Phase 1 BETA, beta-status content được persist dưới dạng markdown file `kitehub/kitehub-subscription/src/main/resources/beta-status/beta-status.md`. Update workflow = git PR + redeploy. KHÔNG có admin UI runtime cập nhật (defer Wave 79+).
- **Source:** Wave 78 plan §1 Brainstorm Q3 outside-in audit — "live status page cost overhead Phase 2".
- **Rationale:** Phase 1 BETA cohort 5-10 tenants, frequency outage thấp + admin update manual acceptable. Live status page (heartbeat check tự động, incident automation) cần investment cao + monitoring infra (Statuspage.io subscription hoặc self-host) → ROI thấp tại Phase 1. Git PR workflow ensure changelog đầy đủ cho audit trail.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Ops, solo-dev, 2026-05-14).
- **Compliance check:** N/A — public content page, không touch PII.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: ≥3 outage incidents/month → reconsider live status investment.
- **Code reference:** (planned) `BetaStatusController` đọc file từ classpath resources.

## BR-BETA-STATUS-002 — Cache TTL 5 phút

- **Value:** Endpoint `GET /api/v1/beta-status` return header `Cache-Control: public, max-age=300`. FE TanStack Query `staleTime: 5 * 60 * 1000`.
- **Source:** Performance benchmark + Phase 1 BETA traffic estimate (≤50 unique visitors/day to status page).
- **Rationale:** *(placeholder — Bucket B sẽ enrich với load test data sau)*
- **Reviewer:** @nguyenvankiet (acting Product Owner + Ops, solo-dev, 2026-05-14).
- **Compliance check:** N/A.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** (planned) `BetaStatusController` `@CacheControl` annotation.

---

## Config

| Key | Default | Purpose |
|-----|---------|---------|
| `kitehub.beta-status.content-source` | `classpath:beta-status/beta-status.md` | Source file path |
| `kitehub.beta-status.cache-ttl-seconds` | `300` | HTTP cache TTL |
| `kitehub.beta-status.rate-limit-per-min-per-ip` | `60` | Gateway rate limit |

Config keys nằm trong `application.yml` BE module (Bucket B).
