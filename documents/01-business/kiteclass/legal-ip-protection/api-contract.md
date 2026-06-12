# Legal / IP Protection — API Contract

## Public DMCA intake

### `POST /public/dmca`

Public, rate-limited by gateway `RateLimitingFilter`. Unauthenticated.

**Request body:**

```json
{
  "reporterEmail": "legal@rightsholder.com",
  "reporterName": "Rights Holder Legal",
  "allegedInfringingUrl": "https://tenant.kitehub.me/logo.svg",
  "copyrightedWorkDescription": "Our registered logo, USPTO #12345"
}
```

**Validation:**

| Field | Rule |
|-------|------|
| reporterEmail | required, RFC 5322 email, ≤255 chars |
| reporterName | required, ≤255 chars |
| allegedInfringingUrl | required, ≤2000 chars |
| copyrightedWorkDescription | required, ≤4000 chars |

**Response 201:**

```json
{
  "success": true,
  "data": { "id": 42, "status": "PENDING" },
  "message": "DMCA notice received; our team will review shortly.",
  "timestamp": "2026-04-15T10:00:00Z"
}
```

**Errors:**
- 400 — validation failure
- 429 — rate limit (gateway filter)

---

## Service-level contract (admin tooling surface)

Admin REST surface for DMCA triage is deferred; consumers call the service directly until that lands.

### `DmcaService.receiveTakedown(request) → DmcaTakedownRequest`
- Always succeeds (no state precondition); returns PENDING row.
- Writes `dmca.takedown.received` audit row.

### `DmcaService.markReviewing(id, reviewerUserId) → DmcaTakedownRequest`
- PENDING → REVIEWING.
- Writes `dmca.takedown.reviewing` audit row.
- Throws `IllegalArgumentException` if id not found.
- Throws `IllegalStateException` if status != PENDING.

### `DmcaService.markValid(id, reviewerUserId) → DmcaTakedownRequest`
- REVIEWING → VALID.
- Writes `dmca.takedown.valid` audit row.
- Logs asset-flagging stub (actual integration deferred).

### `DmcaService.markInvalid(id, reviewerUserId, reason) → DmcaTakedownRequest`
- REVIEWING → INVALID (terminal).
- `reason` recorded on entity + audit row.

### `DmcaService.execute(id) → DmcaTakedownRequest`
- VALID → EXECUTED (terminal).
- Writes `dmca.takedown.executed` audit row.
- Branding asset revert is stubbed (log only).

### `DmcaService.contest(id, counterNoticeEmail) → DmcaTakedownRequest`
- VALID → CONTESTED (terminal).
- Counter-notice email is persisted; Ops notifies reporter manually.

---

## Trademark service contract

### `TrademarkCheckService.checkTextKeywords(text) → TrademarkCheckResult`

| Input | Output |
|-------|--------|
| null / blank text | `TrademarkCheckResult.clear()` |
| no match | `clear()` |
| one or more banned keywords match (case-insensitive substring) | `flagged(hits)` |

Seed list: `legal.trademark.banned-keywords` config.

---

## Future REST surface (deferred)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/internal/dmca` | Admin list PENDING / REVIEWING notices |
| POST | `/api/v1/internal/dmca/{id}/reviewing` | Admin mark reviewing |
| POST | `/api/v1/internal/dmca/{id}/valid` | Admin mark valid |
| POST | `/api/v1/internal/dmca/{id}/invalid` | Admin mark invalid |
| POST | `/api/v1/internal/dmca/{id}/execute` | Admin execute takedown |
| POST | `/api/v1/internal/dmca/{id}/contest` | Record counter-notice |

## Log
- 2026-04-15 — API contract drafted (Wave 4 Sub-PR 4.3, GAP-042)
