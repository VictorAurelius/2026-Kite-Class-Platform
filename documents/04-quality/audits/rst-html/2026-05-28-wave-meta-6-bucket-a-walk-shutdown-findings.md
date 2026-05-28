---
audience: dev
date: 2026-05-28
session-theme: Wave meta-6 Bucket A staff-invite flow walk — shutdown findings
walk_status: SHUTDOWN at Bước 2.10 (login as new staff impossible — Bug #17 accept doesn't create user)
walk_branch: fix/admin-layout-owner-staff-access (cumulative + Bug #8/10/11/12/13/15 fixes)
walk_started_at_head: 57935a55
walk_shutdown_at_head: cb7ada33
bugs_surfaced: 17
gaps_to_file: 17 (1 META rule + 16 individual bugs)
wave_meta_6_bucket_a_verdict: NOT eligible for DONE per gap-done-discipline.md §2 — 2 P0 feature paths missing (email send + user provision); current "DONE" marker invalid
---

# Wave meta-6 Bucket A staff-invite flow — RST walk shutdown findings

## Session arc

Continuing 2026-05-28-wave-meta-6-followup-walk session handoff (`documents/03-planning/session-handoffs/2026-05-28-wave-meta-6-followup-walk.md`). Prior session blocked at Bug #8 (GAP-783). This session resumed walk from Bước 2.6 after applying initial GAP-783 fix.

**Walk reached Bước 2.9 (accept invitation via curl) PASS, Bước 2.10 (login as new staff) FAIL — walk SHUTDOWN.**

## Verdict

Wave meta-6 Bucket A (`feat(wave-meta-6-bucket-a): BE MVP staff invitation flow GAP-772` PR #1904) **SHOULD NOT have been marked DONE**. The flow is fundamentally non-functional in production:

1. Owner clicks "Mời" → DB row created ✅
2. Staff never receives email ❌ (Bug #14 — no email/event/outbox logic exists)
3. Even if staff somehow obtains token, accept call marks invitation done but never creates user account ❌ (Bug #17 — code comment self-documents deferral to "paired GAP-779 which doesn't exist")
4. Staff can never log in ❌ (consequence of #17)

Per `gap-done-discipline.md` §2: DONE requires AC verified. Wave meta-6 Bucket A AC included end-to-end happy path but verification was unit-test only. **The DONE flip violated the rule.**

## 17 bugs surfaced (chronological)

### Bug class A — Architecture/auth mismatches (root cause: kiteclass-core legacy auth context vs gateway UUID/JWT)

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| 8 | `@PreAuthorize` annotation doesn't fire — SecurityConfig is `.anyRequest().permitAll()` so no Authentication object exists, hasAnyRole/hasAnyAuthority both eval false | P0 | Replaced with `@RequestHeader("X-User-Roles")` + manual check (mirror `VettingController.requireSafeguardingOfficer` pattern) | Sweep ALL `@PreAuthorize` annotations in kiteclass-core controllers — likely many similar ghost-guards. Audit recommendation: file MUST-be-header-RBAC sweep gap. |
| 13 | `UserContext.CURRENT_USER: ThreadLocal<Long>` (legacy school numeric IDs) but gateway forwards `X-User-Id` as UUID string from JWT sub claim → always null → `AUTH_REQUIRED` 401 | P0 | Removed null check on `inviterId`; allow null (column `invited_by_user_id` is nullable) | LARGE refactor — UserContext must accept UUID/String, all `@RequestHeader("X-User-Id") Long` controller params must change, JpaConfig auditor must convert, ~40+ touchpoints |
| 16 | Gateway TenantResolver rejects public-but-tenant-scoped endpoints (`/by-token`, `/accept`) on localhost — recipient has no JWT, no subdomain → 400 from gateway before reaching kiteclass-core | P0 | Walked via curl + `X-Instance-Subdomain: sky-edu-test` header (dev-only header) | Architectural: either derive tenant from invitation token lookup at gateway (resolve invitation→instance) OR document that production recipients arrive via tenant subdomain link |

### Bug class B — FE↔BE contract drift (root cause: FE Wave 80 era + BE Wave meta-6 ship reshaped DTOs/endpoints without FE catch-up)

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| 7 | FE invite form sends `{email, fullName}`; BE DTO requires `{email, role}` (no fullName field) → 400 `must not be blank` on role | P1 | Added role dropdown (TEACHER/STAFF/MANAGER); fullName kept UI-only (not sent) | Extend `api-contract-audit` Cat 2 to grep FE call sites + verify payload shape ↔ BE @Valid DTO. Pre-existing GAP-784 already filed by prior session. |
| 12 | FE `/admin/staff/page.tsx` calls `setRows(resp.data)` directly — axios `.data` is the wrapped ApiResponse `{success, data: [], timestamp}`, not the array → `TypeError: e.map is not a function` → page renders "Đã xảy ra lỗi" | P1 | Defensive unwrap: `Array.isArray(body) ? body : body?.data ?? []` | Sweep ALL FE pages consuming list endpoints. Likely many similar bugs in other Wave 80 era admin/customer pages. Consider response interceptor that auto-unwraps ApiResponse globally. |
| 15 | FE accept-invite page calls `GET /api/v1/staff-invitations/by-token/{token}` for preview; BE Wave meta-6 controller has POST/GET-list/DELETE/accept but NO by-token preview endpoint → 404/400 → "Không tải được thông tin lời mời" | P0 | Added `GET /by-token/{token}` endpoint to controller; returns preview shape FE expects | Audit other potentially missing endpoints (FE references in `endpoints.staffInvitations.{resend, revoke}` etc.). Resend endpoint also referenced but not implemented in BE. |

### Bug class C — Test fixture / dev environment incompleteness

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| 9 | Test fixture `owner.test@test.vn` exists but `tenant_id = NULL` + doesn't own any instance — Owner JWT has no `tenantId` claim → TenantResolver fails on ALL Owner test sessions | P2 (dev only) | DB UPDATE: link owner.test → sky-edu-test instance + UPDATE instance.owner_id | Fix `scripts/seed-data.sh` to provision owner.test with full tenant link. Otherwise no future Owner walk session will work without manual SQL hack. |

### Bug class D — Reactor / async architecture violations

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| 10 | `FallbackController.html503()` etc. return synchronous `ResponseEntity<String>` containing `brandingClient.fetch()` which internally calls `.block()` on `Mono<>`. In WebFlux gateway, controller runs on reactor parallel-N thread → `IllegalStateException: block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-1` → circuit breaker opens → 503 cascading | P0 | Refactored all 9 FallbackController methods to return `Mono<ResponseEntity<String>>` + `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` | Audit ALL synchronous controllers in webflux gateway. Likely 5-10 more sites with same pattern. Also consider: `@Cacheable` is fundamentally non-reactive — eventual fix is reactor-native cache. |

### Bug class E — UX / navigation completeness

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| 11 | Owner customer dashboard sidebar has no link/menu to `/admin/staff` — user must manually type URL. Pure UX gap: feature shipped BE+FE pages but FE wiring incomplete | P1 | Added `{href: '/admin/staff', label: 'Nhân viên', icon: Users, requiresRole: ['OWNER']}` to `Sidebar.customerNav` | Audit nav coverage across all Owner-scoped features (Wave 80 + Wave meta-6). Likely other features have orphaned pages too. |

### Bug class F — Critical feature gaps (incomplete MVP ship)

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| **14** | `StaffInvitationServiceImpl.invite()` ONLY saves DB row. **Zero outbox/event/notification/email logic.** Invite is non-functional — staff never receives email in production | **P0 — FEATURE INCOMPLETE** | ❌ no walk-fix possible — feature path missing | LARGE: implement outbox event → kitehub-email consumer → email template → SES/MailHog binding. Pair with Bug #16 — recipient link must resolve tenant. |
| **17** | `StaffInvitationServiceImpl.accept()` marks invitation ACCEPTED + acceptedAt, but **does NOT create user record**. Code comment self-documents: "acceptedUserId set by gateway after it provisions the User row; gateway calls back via internal endpoint OR we update via a follow-up attach call. For MVP the field remains null until gateway integration lands (paired GAP-779 KH auth /me endpoint)." | **P0 — FEATURE INCOMPLETE** | ❌ no walk-fix possible — password from request dropped on floor, no user provisioning | LARGE: implement user-create on accept (or gateway callback). Decide architecture: should kiteclass-core create user OR call back to kitehub-platform? GAP-779 mentioned in comment **doesn't exist** in gap-status.csv. |

### Bug class G — Other (peripheral)

| # | Bug | Severity | Walk-fix applied | Production-fix scope |
|---|---|---|---|---|
| 1-6 | Pre-session bugs from prior RST walk (`documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md`) — duplicate banner / login remember-me / role-route mismatch / TenantResolver architectural / Wave 80 dead code / RabbitMQ queue auto-declare / FE role param drift | mixed | All applied locally (cumulative branch) | Already filed in prior session per handoff |
| (CSP) | Browser CSP report-only blocks `connect-src http://localhost:9000` (production config). Non-blocking but noise. | P2 | None | Update CSP for `kitehub-frontend` to include `localhost:9000` in dev profile OR document that CSP report-only is expected on dev. |
| (404) | `/docs/data-reset-policy` Footer link → 404 (page not in Next.js routes) | P2 | None | Either remove broken link from Footer OR create the docs/data-reset-policy page |

## Walk fix vs production fix comparison

**Walk-fixes applied (8 bugs):** all are minimum-viable local hacks to get walk to next step. None are production-ready:

- Header RBAC (Bug #8): correct pattern but only applied to 1 controller; sweep gap needed
- DB UPDATE (Bug #9): one-time SQL hack; seed script unchanged
- Reactor refactor (Bug #10): controller-level fix; underlying `@Cacheable + WebClient.block()` antipattern still present
- Sidebar nav (Bug #11): correct addition but role-route audit reveals other orphaned features
- ApiResponse unwrap (Bug #12): defensive page-level; global interceptor or audit Cat 2 ext needed
- UserContext null (Bug #13): null-allowed workaround; ~40+ touchpoints still hardcode Long
- by-token endpoint (Bug #15): added but no test, no IT, no FE integration verification
- Tenant subdomain header (Bug #16): walked via dev-only header; production architecture undecided

**Bugs without walk-fix (2 bugs — block actual feature use):**

- Bug #14: email send — entire path missing
- Bug #17: user provisioning on accept — entire path missing

## META pattern: trust-pass anti-pattern recurrence ≥7 (now quantified)

Per session handoff line 92-100, audits ALL passed yet RST walk found 17 bugs. This is the highest recurrence count yet:

| Wave | Trust-pass instance | RST findings count |
|---|---|---|
| Wave 71b (2026-05-13) | Admin login 500 — unit tests + IT pass production fail | 3 bugs (GAP-518/519/520) |
| Wave meta-6 prior (2026-05-28 first half) | 5 audits 76-94/100 PASS | 8 bugs (GAP-782/783/784/785 + 4 docs-only) |
| **Wave meta-6 this walk (2026-05-28 second half)** | Same 5 audits PASS, plus 25 Mockito tests PASS | **17 bugs** (this doc) |

**Recurrence ≥7 instances of "audit + tests pass + feature broken in production" pattern.** Per `incident-to-rule-pipeline.md` §3.1, recurrence count ≥2 alone justifies detector + tightened legitimate-deferral. Count ≥7 demands META rule.

## META rule proposed (paired same-PR)

`feature-ship-runtime-walk-mandate.md` v1.0.0 — see `.claude/rules/feature-ship-runtime-walk-mandate.md` (draft this same shutdown PR per Enforcement Parity Mandate §6.5).

Mandate: every gap with scope "user-facing feature" requires manual RST walkthrough on production-equivalent stack BEFORE DONE flip. Audit + unit + IT tests cannot substitute. Walk evidence pasted in gap closure.

Specifically NOT covered by existing rules:
- `pre-handoff-self-test-completeness.md` v1.2.0 §3 covers POST-FIX re-walk (this incident shows ORIGINAL ship needs same)
- `audit-to-gap-pipeline.md` §2.8 covers fix-time state-check (this incident shows feature-DONE-time state-check missing)
- `gap-done-discipline.md` covers DONE flip mechanics (this incident shows AC verification mechanism missing)

## Recommendations

### Immediate (this PR or next session)

1. **File 17 gaps from §Bugs surfaced** — each Bug # = 1 gap. Severity per table. Wave-future placement.
2. **Ship META rule** `feature-ship-runtime-walk-mandate.md` v1.0.0 — paired same-PR per Enforcement Parity Mandate §6.5
3. **Revert Wave meta-6 Bucket A DONE flag** — gap GAP-772 should NOT be DONE. Re-classify as PARTIAL (P0 PR shipped infra; feature gaps #14 + #17 block real use).
4. **Block "Wave meta-6 Bucket A DONE" in audits-index.csv + roadmap** — annotation "RST walk 2026-05-28 surfaced 17 bugs incl 2 P0 feature paths missing"

### Wave-future (Phase 1.5 / Phase 2 BETA)

5. **Implement Bug #14 email path** (outbox + kitehub-email consumer + template + binding)
6. **Implement Bug #17 user provisioning** (decide architecture: kiteclass-core creates user OR gateway callback OR kitehub-platform owns)
7. **Sweep Bug #8 @PreAuthorize ghost-guards** across all kiteclass-core controllers
8. **Refactor Bug #13 UserContext** Long → UUID (~40+ touchpoints)
9. **Architecture decide Bug #16** — public-but-tenant-scoped endpoint tenant resolution path
10. **Sweep Bug #12 FE ApiResponse unwrap** across all FE pages OR global axios interceptor
11. **Sweep Bug #10 reactor blocking** across all webflux gateway controllers
12. **Audit Bug #11 role-route nav coverage** across all Owner/Admin features
13. **Fix Bug #9 seed-data.sh** to provision owner.test fully

### Cross-cutting

14. **Extend `api-contract-audit` Cat 2** — grep FE call sites against BE controller endpoints; verify payload shape + response shape match
15. **Build `ops-readiness-audit` ext** — probe each mutation endpoint produces expected side effects (email, event, broker message)
16. **Consider `business-logic-audit` ext** — walk AC manually (or via automated RST script) for each P0 feature

## References

- Prior session handoff: `documents/03-planning/session-handoffs/2026-05-28-wave-meta-6-followup-walk.md`
- Prior RST walk: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md`
- Wave meta-6 plan: `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md`
- Trust-pass class memory: `feedback_audit_of_trust_pass.md`
- META rule draft: `.claude/rules/feature-ship-runtime-walk-mandate.md` (paired same shutdown PR)
- Gap files to file: 17 (see §Bugs surfaced — each Bug # = 1 gap)
