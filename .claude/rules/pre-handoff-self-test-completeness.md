# Pre-Handoff Self-Test Completeness — verify the FLOW, not the endpoint

**Priority:** 🔴 CRITICAL — verification governance
**Version:** 1.1.1
**Created:** 2026-05-13
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; adds 7 new flow classes §2.5-§2.11 (file-upload, payment, multi-tenant tenant-switch, SSE/WebSocket, background job, time-sensitive, i18n) closing GAP-524 — no constraint loosening, prospectively extends coverage of "user-facing flow" classes. v1.0.0 (kept): new rule with mandatory verify checklist + worked self-test on Wave 71b admin-login bug per §6.5 Enforcement Parity Mandate)
**Applies to:** Every "verify live" / "self-test PASS" claim coordinator makes when handing off a wave/gap closure to user OR another session. Scope explicitly includes any artifact marked `🟢 DONE` whose AC mentions a user-facing path (URL, button, form, login redirect, dashboard, email link, file upload, payment redirect, tenant switch, real-time connection, background job, time-sensitive action, i18n content).

---

## 1. The Rule

> **"API returns 201" ≠ "user can do this." A verify step is complete ONLY when a fresh actor — starting from the prior step's output — can execute the next step end-to-end without hunting for missing pieces.**

Coordinator must verify the FLOW: from user-facing entry point → through any auth gate → to the post-condition the AC claims. If any of these is missing or broken, the gap is NOT done:

- ✅ User has the credential needed for the step (saved/log-accessible OR explicitly user-provided)
- ✅ User has the navigation path (button OR documented URL OR clear next-step note in handoff)
- ✅ The role/permission gate at the navigation path actually grants access to the seeded user
- ✅ The endpoint reached the correct backend AND returned a usable response shape
- ✅ The UI surface (if any) actually renders the response without crash/redirect/blank

Endpoint-level verify (`curl` returns 201 from correct backend) is **necessary but not sufficient**.

---

## 2. Required verify steps by gap class

### 2.1 Auth-gated user-flow gap (login → action)

When AC mentions "user can do X" where X requires login:

| Check | Pass criterion |
|---|---|
| (a) Credential available to next actor | Log secret value in handoff message OR explicit retrieval recipe (e.g., `aws secretsmanager get-secret-value ...`) |
| (b) Login API works (curl) | HTTP 200 + JWT in body |
| (c) Login UI works | Browser → submit credentials → redirects to expected post-login URL |
| (d) Role-guard accepts seeded role | Post-login user sees expected dashboard, NOT 403/redirect/blank |
| (e) Navigation to target page | Either: button visible in dashboard, OR direct URL works without auth bounce |
| (f) Target page renders | Page loads with data, NOT spinner-forever / crash / "loading..." |
| (g) Target action succeeds | The X action (approve, click, submit) returns success + UI updates |

Skip any check → gap stays `🟡 PARTIAL`, file follow-up.

### 2.2 Anonymous/public flow gap

For non-auth-gated flows (e.g., signup, public page):

| Check | Pass criterion |
|---|---|
| (a) URL or form entry point exists in published UI | Visible link on homepage OR documented anchor |
| (b) Form submit works end-to-end | curl AND browser POST both return expected status |
| (c) Confirmation surface visible | Success page renders OR confirmation email arrives |

### 2.3 Email-driven flow gap

| Check | Pass criterion |
|---|---|
| (a) Email actually sent (not queued+dropped) | Provider dashboard shows "delivered" OR check inbox |
| (b) Link in email points to live URL | curl that URL → 200, NOT 404/dev-domain |
| (c) Clicking link advances state | Token validates, downstream action completes |

### 2.4 Admin/privileged action gap

| Check | Pass criterion |
|---|---|
| (a) Admin role grant correctness | Frontend role-guard accepts the role value backend actually seeds |
| (b) Admin sees admin dashboard | Post-login navigation lands on admin home (not user home) |
| (c) Admin can navigate to target page | UI link in admin nav/sidebar OR documented direct URL |
| (d) Admin action triggers correct backend | Network tab shows POST to correct service (not 404 / wrong service) |

### 2.5 File-upload flow gap (added v1.1.0 — GAP-524)

When AC mentions "user uploads file" (image, document, CSV, ZIP):

| Check | Pass criterion |
|---|---|
| (a) MIME validation enforced server-side | Upload rejects forbidden types with 415; reject `text/html` even if extension `.png` |
| (b) Size limit enforced + documented | Server returns 413 when exceeded; limit cited in `api-contract.md` |
| (c) Virus/malware scan present (when applicable) | ClamAV or equivalent in pipeline OR documented exemption with risk acceptance |
| (d) Storage location correct (MinIO/S3 bucket per env, NOT local FS) | Uploaded file appears in bucket; bucket policy verified non-public unless intended |
| (e) Retrieval URL works | Issued signed/pre-signed URL returns 200 + expected `Content-Type` |
| (f) Failed upload UI surface visible | Browser shows error toast/inline message — not silent fail |
| (g) Audit log entry created (`uploaded_by`, `uploaded_at`, `file_hash`) | DB row exists OR log line emitted |

### 2.6 Payment flow gap (added v1.1.0 — GAP-524)

When AC mentions "user pays / subscribes / charges card":

| Check | Pass criterion |
|---|---|
| (a) Gateway redirect URL correct | Browser redirected to actual provider (Stripe/MoMo/VNPay), not a stub |
| (b) Return URL handled (success + cancel paths) | Both paths render the right post-payment UI |
| (c) Webhook signature verified server-side | Webhook handler rejects unsigned/invalid-signature requests with 400 |
| (d) Idempotency key honored | Same key replayed → no double-charge; row in `payment_attempts` table with idempotency state |
| (e) Reconciliation table updated | `payments` / `invoices` row matches gateway-side state after webhook |
| (f) Failed payment UI clear | User sees actionable error (insufficient funds, card declined) — not generic 500 |
| (g) Audit log: amount, currency, gateway_txn_id, user_id, timestamp | Row exists in `payment_audit_log` |

### 2.7 Multi-tenant tenant-switch flow gap (added v1.1.0 — GAP-524)

When AC mentions "user with N tenants switches workspace / login picks tenant":

| Check | Pass criterion |
|---|---|
| (a) Login as user-with-N-tenants returns tenant picker | UI shows picker; user with 1 tenant skips automatically |
| (b) Picker selection issues new JWT scoped to chosen tenant | DevTools network tab shows new `Authorization: Bearer <token>` with `tenantId` claim |
| (c) Data isolation verified (no cross-tenant leak) | Switch tenant A → tenant B; verify tenant A's records do NOT appear in tenant B's lists |
| (d) Switching back doesn't carry stale cache | Repeat A→B→A; tenant A's data correct (not from B's response cache) |
| (e) URL reflects tenant context (`/t/<slug>/...` or header-based) | URL OR header consistent with active tenant |
| (f) Logout clears all tenant tokens | Subsequent request requires re-login |

### 2.8 SSE / WebSocket / long-polling flow gap (added v1.1.0 — GAP-524)

When AC mentions "real-time updates / live data / push notification":

| Check | Pass criterion |
|---|---|
| (a) Connection establishes via correct protocol (`wss://` not `ws://` in prod) | DevTools network tab shows protocol upgrade |
| (b) Heartbeat / keepalive prevents 30s connection idle drop | Connection stays open >60s in idle test |
| (c) Reconnect-on-drop works (browser network throttle simulation) | Connection re-establishes within 10s after simulated drop |
| (d) Auth-on-reconnect: re-uses valid JWT, rejects expired | New connection rejected if token expired since first connect |
| (e) Server message delivery verified end-to-end | Trigger server-side event → client UI updates within 5s |
| (f) Graceful degradation when WebSocket blocked (proxy/firewall) | Falls back to polling OR shows "real-time unavailable" notice |

### 2.9 Background job / async flow gap (added v1.1.0 — GAP-524)

When AC mentions "queue / async / job / worker / batch":

| Check | Pass criterion |
|---|---|
| (a) Enqueue: caller receives jobId immediately (no blocking wait) | Response time <500ms for enqueue endpoint |
| (b) Worker picks up job (verify via RabbitMQ admin UI or queue depth) | Queue depth decrements; worker log emits "processing jobId=..." |
| (c) Retry on failure (max-attempt configured) | Manually fail job once; verify automatic retry; exhausted retries → DLQ |
| (d) Dead-letter queue (DLQ) collects exhausted jobs | DLQ visible in admin UI; alert fires on DLQ non-empty per `audit-skill-rubric-ops-readiness-audit.md` §2.4 |
| (e) Status query endpoint returns correct state (`pending\|running\|success\|failed`) | Client polls jobId → state transitions visible |
| (f) Completion notification fires (email/webhook/SSE) | Recipient receives notification within SLA window |

### 2.10 Time-sensitive flow gap (added v1.1.0 — GAP-524)

When AC mentions "token expires / clock / TTL / time-based":

| Check | Pass criterion |
|---|---|
| (a) Token expiry honored (no infinite TTL) | Wait past TTL → API returns 401; UI prompts re-login |
| (b) Refresh-token rotation works | Old refresh blacklisted on use; reuse triggers force-logout per `pre-launch-auth-hardening-checklist.md` §2.8 |
| (c) Clock skew tolerance ±60s (server vs client) | Simulate ±60s skew → JWT still validates within tolerance window |
| (d) Time-sensitive UI countdown accurate (e.g., trial ends in N days) | Countdown matches server-issued `valid_until` timestamp |
| (e) Edge cases: TZ boundary, DST, leap second | Test 23:59→00:00 boundary; UTC stored in DB, displayed in user TZ |

### 2.11 i18n flow gap (added v1.1.0 — GAP-524)

When AC mentions "Vietnamese / English / multi-language / localization":

| Check | Pass criterion |
|---|---|
| (a) Locale detection works (`Accept-Language` header OR explicit toggle) | Browser language→matching locale rendered |
| (b) Fallback to default locale (vi) when key missing in user locale | No raw key string visible (e.g., `t('users.title')` literal) |
| (c) Content variant renders for ≥2 locales (vi + en) tested end-to-end | Manual switch + verify both renderings |
| (d) Date/number/currency formatted per locale (`Intl` / `DateTimeFormatter`) | vi: `1.234,56 ₫`; en: `$1,234.56` |
| (e) Pluralization rules correct (vi has 1 form, en has 2) | "1 user" vs "2 users"; vi: "1 người dùng" vs "2 người dùng" |
| (f) Right-to-left support N/A unless Arabic/Hebrew added | Document N/A explicitly when scope only vi+en |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Curl returns 201, gap DONE" | Walk user-facing FLOW (login → nav → action → confirmation) |
| Skip credential delivery to handoff | Log credential value OR explicit retrieval command in handoff message |
| "UI exists, must work" without browser test | Browser/headless test OR explicit "UI verify deferred per <reason>" PARTIAL |
| Assume role names match between BE seed + FE guard | `grep` BE seed role + FE role-guard literal; reconcile |
| Verify in dev environment only | Production-equivalent verify (same image tag, same CORS, same role value) |
| Skip navigation check "URL works in browser" | Verify the LINK exists; new user shouldn't need to type URL by memory |

---

## 4. Worked self-test

See `_examples/pre-handoff-self-test-completeness-examples.md` §Worked self-test (Wave 71b 2026-05-13 admin-login incident — §2.4 admin-flow checklist (a)+(b)+(c) all FAIL retroactively, validates rule fires).

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Pre-handoff checklist in coordinator messages

When coordinator flips any gap to `🟢 DONE` whose AC includes a user-facing path, the message MUST include:

```
## Pre-handoff verify per pre-handoff-self-test-completeness.md §2.<class>

- [ ] Credential available: <method or N/A reason>
- [ ] Login flow works: <evidence>
- [ ] Role-guard accepts: <evidence>
- [ ] Navigation path: <UI button OR documented URL>
- [ ] Target page renders: <evidence>
- [ ] Target action succeeds: <evidence>
```

If any line marked `❌` or skipped → gap MUST stay `🟡 PARTIAL` per `gap-done-discipline.md` §3.

### 5.2 PR template checkbox

`.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist row:

> - [ ] **Pre-handoff self-test completeness** — if PR closes a gap whose AC mentions user-facing flow (login, button, URL, dashboard, email link), §2 class-appropriate checklist in PR body OR explicit `PRE_HANDOFF_PARTIAL: <reason>` trailer

### 5.3 Reviewer-checklist

Reviewer asks before approving DONE flip:
- Does the gap touch a user-facing flow?
- Did coordinator publish §2 checklist results?
- Did they verify role name match between BE seed + FE guard?

### 5.4 Override mechanism

For genuine cases where end-to-end browser test is infeasible (e.g., 3rd-party OAuth in pre-prod, hardware MFA, paid SMS):

```
git commit -m "...
PRE_HANDOFF_PARTIAL: <step> — <reason — what cannot be verified>
PRE_HANDOFF_FOLLOWUP: <gap link scheduling the actual verify within Ndays>"
```

Trailer logged in quarterly retro. Pattern frequency >5%/quarter triggers meta-review.

### 5.5 Detector (deferred per premature-rule guard)

Future: `audit-gate.py` rule scanning PR body for `LIVE VERIFY` / `verified live` / `tested live` claims that DON'T also include §2.x checklist words ("login flow", "role-guard", "navigation"). Defer until 2nd recurrence; reviewer + memory + worked self-test sufficient.

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Use curl HTTP code as sole verify | Add browser/headless UI step OR document PARTIAL |
| "User can figure out the URL" | Provide URL OR add UI button before claiming DONE |
| Trust BE seed role + FE role-guard match | grep both literals, reconcile |
| Hand off without credential | Embed credential OR retrieval recipe in closure message |
| Skip "what does user see at /home after login" check | Walk the flow |
| Use staging-env verify for production-flip gap | Verify on same image tag against production endpoint |

---

## 7. Relationship to other rules

- **`gap-done-discipline.md`** §2 — DONE flip requires AC verified; this rule sharpens "verified" for user-facing AC
- **`audit-to-gap-pipeline.md`** §2.5/§2.6/§2.7/§2.8 — state-check family; this rule adds FLOW-check after state-check
- **`output-review-mandate.md`** §3 — adds row "Pre-handoff verify" tracking this standard
- **`agent-action-bias.md`** §1 Part A — do it yourself; this rule extends to "verify it yourself end-to-end"
- **`incident-to-rule-pipeline.md`** — this rule is direct output of Wave 71b admin-login incident applied through 5-stage
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + checklist embed + worked self-test all ship same PR
- **`feedback_audit_of_trust_pass.md`** (memory) — recurrence #4 of "AC `[x]` ≠ production-verified" — this rule is sharper enforcement of the same principle

---

## 8. Log

- **2026-05-14 (v1.1.1):** PATCH — Wave 76 Bucket E body streamline. §4 Worked self-test moved to `_examples/pre-handoff-self-test-completeness-examples.md`; body replaced with 1-line stub pointer. No constraint change; content preserved in `_examples/` (deferred-load). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-05-14 (v1.1.0):** MINOR — added 7 new flow class checklists §2.5-§2.11 closing GAP-524 META P1 (Wave 72b Bucket E): §2.5 File-upload (MIME/size/scan/storage/retrieval), §2.6 Payment (gateway redirect/webhook signature/idempotency/reconciliation), §2.7 Multi-tenant tenant-switch (picker/JWT swap/data isolation/cache invalidation), §2.8 SSE/WebSocket/long-polling (protocol/heartbeat/reconnect/auth-on-reconnect/degradation), §2.9 Background job/async (enqueue/worker pick/retry/DLQ/notification), §2.10 Time-sensitive (TTL/refresh rotation/clock skew/countdown), §2.11 i18n (locale detection/fallback/format/pluralization). Each class mirrors §2.1-§2.4 4-row checklist structure adapted to its class. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (GAP-524 Wave 71c-meta-Phase-2 discovery — 7 classes likely to cause future verify-claimed-but-flow-broken incidents) → Classify ✓ (existing v1.0.0 covered 4 classes; 7 more enumerable from incident-likely surfaces) → Rule+Enforce ✓ (this v1.1.0 + same-PR sister 6 audit-rubric rules per `rule-change-process.md` §6.5) → Self-Test ✓ (each class's required check is grep-able / observable / verifiable — not aspirational; e.g., 2.5 (c) virus scan = ClamAV presence verifiable) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds prospective coverage for 7 previously-uncovered flow classes; no constraint loosening for prior gap closures; rule applies prospectively from Wave 72b Bucket E forward).

- **2026-05-13 (v1.0.0):** Rule created in response to user-flagged miss — Wave 71b closure claimed "verified live" but admin@kitehub.me UI flow had 3 unblocked bugs (no nav button, no credential in handoff, role-name mismatch). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user "self-test quá tệ") → Classify ✓ (no existing rule mandates flow-level verify; `gap-done-discipline.md` covers DONE flip mechanics; `audit-to-gap-pipeline.md` covers state-check at file time, not flow at closure time) → Rule+Enforce ✓ (this file + paired same-PR with `pre-launch-auth-hardening-checklist.md` + 8 gap files + ROADMAP Wave 71c queue + worked self-test §4 per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on Wave 71b incident — rule fires correctly + 3 checklist items FAIL retroactively, file GAP-518/519/520) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new coverage class, no constraint loosening; existing DONE flips grandfathered, rule applies prospectively).
