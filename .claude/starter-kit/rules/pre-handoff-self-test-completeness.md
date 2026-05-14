# Pre-Handoff Self-Test Completeness — verify the FLOW, not the endpoint

**Priority:** 🔴 CRITICAL — verification governance
**Version:** 1.0.0
**Created:** 2026-05-13
**Last-Reviewed:** 2026-05-13
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds coverage for a previously-uncovered class)
**Applies to:** Every "verify live" / "self-test PASS" claim coordinator makes when handing off a wave/gap closure to user OR another session. Scope explicitly includes any artifact marked `🟢 DONE` whose AC mentions a user-facing path (URL, button, form, login redirect, dashboard, email link).

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

## 4. Worked self-test — Wave 71b 2026-05-13 incident

**Scenario:** Coordinator (me) claimed "Plan 1 Bước 2 LIVE PASS — HTTP 201 + DB row id=1 PENDING" + flipped GAP-509/512/513 → DONE. User attempted Plan 1 Bước 4 (admin approve in UI) and hit 2 bugs:

1. No UI button → had to guess URL `/admin/beta-requests`
2. Direct URL → redirect to `/login` → no admin credential in handoff
3. After credential retrieved manually from AWS, login succeeded → redirects to `/dashboard` (not `/admin`) → `/admin/*` routes blocked by role-guard

**Apply §2.4 admin-flow checklist retroactively:**

| Check | Pre-this-rule | Required outcome |
|---|---|---|
| (a) Role match BE seed `PLATFORM_ADMIN` vs FE guard `'ADMIN'` | ❌ NOT VERIFIED | grep both, find mismatch → file P0 gap |
| (b) Admin sees admin dashboard post-login | ❌ NOT VERIFIED | browser test: login as admin → expect `/admin` URL |
| (c) Admin can navigate to /admin/beta-requests | ❌ NOT VERIFIED | check AdminLayout sidebar/nav |
| (d) Approve action reaches <subscription-service> | ⚠️ partially (curl GET works, button POST not tested) | browser click → network inspect |

**Verdict:** §2.4 (a)+(b)+(c) all FAIL retroactively. Self-test PASS as a worked example proving the rule fires on the originating incident.

**Cost of the miss:** ~1 user round-trip to discover bugs that should have been surfaced at Wave 71 closure. GAP-518 (role mismatch) + GAP-519 (admin nav missing) filed as P0 follow-ups.

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

- **2026-05-13 (v1.0.0):** Rule created in response to user-flagged miss — Wave 71b closure claimed "verified live" but admin@<production-domain> UI flow had 3 unblocked bugs (no nav button, no credential in handoff, role-name mismatch). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user "self-test quá tệ") → Classify ✓ (no existing rule mandates flow-level verify; `gap-done-discipline.md` covers DONE flip mechanics; `audit-to-gap-pipeline.md` covers state-check at file time, not flow at closure time) → Rule+Enforce ✓ (this file + paired same-PR with `pre-launch-auth-hardening-checklist.md` + 8 gap files + ROADMAP Wave 71c queue + worked self-test §4 per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on Wave 71b incident — rule fires correctly + 3 checklist items FAIL retroactively, file GAP-518/519/520) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new coverage class, no constraint loosening; existing DONE flips grandfathered, rule applies prospectively).
