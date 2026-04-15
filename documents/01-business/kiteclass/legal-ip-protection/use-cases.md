# Legal / IP Protection — Use Cases

## UC-DMCA-001 — Rights-holder submits DMCA takedown (public intake)

**Actor:** External rights holder (or their agent)
**Trigger:** Submit form on `/legal/dmca` (KiteHub FE) OR `POST /public/dmca` directly
**Preconditions:** —

**Steps:**
1. Claimant completes form (reporter email + name, alleged infringing URL, copyrighted work description).
2. FE validates required fields client-side.
3. FE submits `POST /public/dmca` (rate-limited by gateway).
4. Backend validates DTO (email format + length caps).
5. `DmcaService.receiveTakedown` persists row with `status = PENDING` + writes `dmca.takedown.received` audit row.
6. FE shows confirmation with the case id.

**Errors:**
- 400 — validation failure (missing fields, malformed email).
- 429 — rate limit exceeded (gateway filter).

**FE behavior:** confirmation message; no case-status tracking UI in this sub-PR.

---

## UC-DMCA-002 — Admin triages a DMCA notice

**Actor:** KiteClass legal / ops reviewer (internal)
**Trigger:** New PENDING item surfaces in ops dashboard (dashboard UI deferred).

**Steps:**
1. Reviewer calls `markReviewing(id, reviewerId)` via admin tooling (REST surface for admin deferred — direct service invocation for now).
2. Service transitions PENDING → REVIEWING + writes `dmca.takedown.reviewing` audit row.
3. Reviewer evaluates the notice against DMCA §512 criteria.
4. Reviewer invokes either `markValid` (legitimate) or `markInvalid(id, reviewerId, reason)` (frivolous / incomplete).

**Errors:**
- `IllegalStateException` on invalid state transition.
- `IllegalArgumentException` if id not found.

---

## UC-DMCA-003 — Execute a valid takedown

**Actor:** KiteClass legal ops
**Trigger:** `markValid` completed; asset revert approved.

**Steps:**
1. Ops triggers `DmcaService.execute(id)`.
2. Service transitions VALID → EXECUTED + writes `dmca.takedown.executed` audit row.
3. **Deferred** — branding asset revert (flag in branding module + re-publish TEMPLATE-category resource).

**Errors:** `IllegalStateException` if status is not VALID.

---

## UC-DMCA-004 — Affected tenant contests a takedown

**Actor:** Tenant admin of the affected instance
**Trigger:** Counter-notice submitted to Ops (out-of-band intake in this sub-PR; dedicated FE deferred).

**Steps:**
1. Ops records counter-notice email via `DmcaService.contest(id, counterNoticeEmail)`.
2. Service transitions VALID → CONTESTED + writes `dmca.takedown.contested` audit row.
3. **Deferred** — automatic email to original reporter informing of the counter-notice per §512(g). Ops notifies manually.

**Errors:** `IllegalStateException` if status is not VALID.

---

## UC-TM-001 — Proactive trademark scan on generation

**Actor:** System (GenerateLogoStep — deferred wire-up)
**Trigger:** Tenant kicks off AI logo generation with a prompt / name / tagline.

**Steps:**
1. GenerateLogoStep calls `TrademarkCheckService.checkTextKeywords(text)`.
2. If `TrademarkCheckResult.isClear()` → proceed with AI generation (FULL_AI category).
3. Else → log the hit + route resource to TEMPLATE fallback + surface warning in wizard preview.

**FE behavior:** wizard preview shows "Detected potential trademark reference: X. Using a curated template instead."

**Errors:** none; scan is advisory-at-worst (never blocks tenant flow).

---

## Log
- 2026-04-15 — Use cases drafted (Wave 4 Sub-PR 4.3, GAP-042)
