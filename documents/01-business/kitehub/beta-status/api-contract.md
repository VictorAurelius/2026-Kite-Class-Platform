# Beta Status — API Contract

**Domain:** Public beta status page content (Wave 78 — GAP-539)
**Source-of-truth controller:** (planned) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/betastatus/controller/BetaStatusController.java`
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

This contract là source-of-truth cross-layer cho Wave 78 Bucket B, consumed by:
- FE Bucket B (GAP-539) — Route `/beta-status` render markdown content trả về từ endpoint
- BE Bucket B (GAP-539) — `BetaStatusController` đọc markdown file từ resources OR DB seed (Phase 1 static MVP)
- MSW handler `kitehub-frontend/src/test/msw/handlers/beta-status.ts` (this PR — Bucket 0)

---

## Design choice — static MVP

Per Wave 78 plan §1 Brainstorm Q3: **static MVP** (không phải live status page như Statuspage.io / Atlassian Statuspage). Lý do:
- Phase 1 BETA cohort nhỏ (5-10 tenant), update manual khi có outage acceptable.
- Live status page có cost overhead (heartbeat checks, incident automation) → defer Wave 79+.
- Markdown content updated bởi platform admin qua git PR (rebuild + redeploy) HOẶC qua admin UI (Wave 79+ scope).

Content được lưu dưới dạng markdown file `kitehub/kitehub-subscription/src/main/resources/beta-status/beta-status.md` (Phase 1 source-of-truth). Endpoint return raw markdown để FE render (`react-markdown` hoặc tương đương).

---

## Endpoints

### GET /api/v1/beta-status

**Use case:** UC-BETA-STATUS-001 — Anonymous visitor hoặc tenant click "Beta Status" link → fetch + render markdown
**Auth:** **Public (unauthenticated).** No JWT required. Endpoint route công khai qua gateway.

**Request:** no body, no query params.

**Response 200 OK (`BetaStatusResponse`):**
```json
{
  "version": "2026-05-14-v3",
  "lastUpdatedAt": "2026-05-14T07:00:00Z",
  "contentMarkdown": "# Trạng thái Beta KiteHub\n\n## Cập nhật mới nhất\n\n**2026-05-14:** Hệ thống hoạt động bình thường...",
  "currentStatus": "OPERATIONAL",
  "knownIssues": [
    {
      "title": "Chậm khi import dữ liệu lớn",
      "severity": "MINOR",
      "since": "2026-05-12"
    }
  ]
}
```

**Field semantics:**

| Field | Type | Mô tả |
|-------|------|------|
| `version` | string | Manual version tag (admin set khi update). Format: `YYYY-MM-DD-vN`. |
| `lastUpdatedAt` | ISO-8601 UTC | Thời điểm content được update lần cuối. |
| `contentMarkdown` | string | Raw markdown content (UTF-8, Vietnamese). FE render với `react-markdown`. |
| `currentStatus` | enum | `OPERATIONAL` \| `DEGRADED` \| `PARTIAL_OUTAGE` \| `MAJOR_OUTAGE` \| `MAINTENANCE`. |
| `knownIssues` | array | List issue đang track. Empty array khi status OPERATIONAL. |
| `knownIssues[].title` | string | Ngắn (≤200 chars), tiếng Việt. |
| `knownIssues[].severity` | enum | `MINOR` \| `MAJOR` \| `CRITICAL`. |
| `knownIssues[].since` | ISO-8601 date | Ngày issue được phát hiện. |

**Caching:** response cache-able. Recommended `Cache-Control: public, max-age=300` (5 min). FE TanStack Query staleTime 5 min.

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 500 | `BETA_STATUS_CONTENT_MISSING` | Markdown file/seed không tồn tại tại server (deploy issue) |
| 503 | `SERVICE_UNAVAILABLE` | (rare) BE service down → gateway return cached/static fallback nếu có |

Không có 401/403 (endpoint public). Không có 404 (always có ít nhất default "no status" content).

---

## Side effects

- KHÔNG có side effect (read-only public endpoint).
- KHÔNG emit outbox event.

---

## Rate limits

- 60 req/min/IP tại gateway (generous vì public + cached). Vượt → 429 `RATE_LIMITED`.

---

## Content update workflow (Phase 1 MVP)

Out-of-band cho endpoint contract, ghi lại để reference:

1. Platform admin edit file `kitehub/kitehub-subscription/src/main/resources/beta-status/beta-status.md`.
2. Commit + PR via normal git flow.
3. Deploy `kitehub-subscription` service (CI/CD).
4. Endpoint reflect update sau cache TTL 5 min.

Wave 79+ scope: admin UI POST/PUT endpoint để update không cần redeploy.

---

## Related

- BR-BETA-STATUS-001..002: `documents/01-business/kitehub/beta-status/rules.md`
- UC-BETA-STATUS-001: `documents/01-business/kitehub/beta-status/use-cases.md`
- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
