# Beta Status — Use Cases

**Domain:** Public beta status page content
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

> **Bucket 0 stub status:** use-cases dưới stub form. Bucket B (GAP-539) sẽ enrich theo final FE/BE implementation.

---

## UC-BETA-STATUS-001 — Visitor xem trạng thái Beta KiteHub

**Actor:** Mọi visitor (anonymous public OR authenticated tenant).
**Trigger:** User truy cập URL `https://kitehub.me/beta-status` HOẶC click link "Beta Status" trong footer/dashboard.
**Endpoint:** `GET /api/v1/beta-status`
**Business rules:** BR-BETA-STATUS-001 (static MVP, manual update qua git PR), BR-BETA-STATUS-002 (cache TTL 5 phút)
**Planned rules (Bucket B enrich, GAP-539):** BR-BETA-STATUS-003 (known-issues structured schema), BR-BETA-STATUS-004 (changelog versioning), BR-BETA-STATUS-005 (incident severity classification)

### Happy path

1. FE route `/beta-status` mount → component fetch `GET /api/v1/beta-status` (no auth header).
2. BE đọc markdown content từ `classpath:beta-status/beta-status.md` (Phase 1 static MVP).
3. BE parse `currentStatus` + `knownIssues` từ frontmatter (YAML head section) hoặc separate config file.
4. BE return `200 OK` với `{ version, lastUpdatedAt, contentMarkdown, currentStatus, knownIssues }`.
5. FE render:
   - Status badge top (color-coded theo `currentStatus`: green OPERATIONAL / yellow DEGRADED / orange PARTIAL_OUTAGE / red MAJOR_OUTAGE / blue MAINTENANCE).
   - Known issues table (nếu có).
   - Markdown content rendered với `react-markdown` (Vietnamese typography support).
6. FE display "Cập nhật lần cuối: <lastUpdatedAt formatted>" tại bottom.

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 2 | Markdown file/seed missing | 500 | `BETA_STATUS_CONTENT_MISSING` | FE show fallback "Trạng thái Beta tạm thời không khả dụng. Vui lòng quay lại sau." |
| (network) | Request fail | — | — | FE show retry button + offline message |
| (gw) | Rate limit (>60 req/min/IP) | 429 | `RATE_LIMITED` | FE show "Quá nhiều yêu cầu. Vui lòng thử lại sau ít phút." |

### FE behavior notes

- Page route public — KHÔNG redirect khi unauthenticated.
- Cache response 5 phút client-side (TanStack Query staleTime).
- Status badge color phải có alt-text aria-label cho accessibility (WCAG 1.4.1 — color không phải sole carrier).
- Link "Beta Status" hiển thị trong footer của mọi page (Bucket F responsibility — footer component).
