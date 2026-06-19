# GAP-733: EmailController v1 namespace migration `/api/platform/emails/*` → `/api/v1/email/*`

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (API contract namespace consistency)
**Detected:** 2026-05-24 (Wave beta-readiness-2 Bucket D follow-up — GAP-662 Option B sync only; Option A deferred)
**Affects:** `kitehub-email` `EmailController` + 5+ caller services + gateway routing config

## Problem

Wave beta-readiness-2 Bucket D (GAP-662) closed contract drift via **Option B** (update api-contract.md to match code path `/api/platform/emails/*`). The longer-term **Option A** migration to `/api/v1/email/*` namespace (matching admin Wave 97 + email v1 namespace convention) was deferred do scope size + risk.

Option A scope (10+ files):

| File | Change |
|---|---|
| `kitehub-email/src/main/java/.../EmailController.java` | `@RequestMapping("/api/platform/emails")` → `/api/v1/email` |
| `kitehub-email/src/test/.../EmailControllerTest.java` | 3 mockMvc paths update |
| `kitehub-subscription/.../EmailServiceClient.java` | line 866 path update |
| `kitehub-subscription/.../EmailConsumer.java` | line 52 path update |
| `kitehub-subscription/.../EmailSenderService.java` | lines 57+96 |
| `kitehub-subscription/src/test/.../EmailConsumerTest.java` | line 79 mock URL |
| `kitehub-gateway/src/main/resources/application.yml` | line 273 routing pattern |
| `kitehub-email/README.md` + `docs/QUICK-START.md` | doc URL updates |
| `documents/01-business/kitehub/email/api-contract.md` | revert Option B note + cite Option A canonical |
| `documents/03-planning/prs/04-kitehub-prs.md` | line 1125 endpoint listing |

## Root Cause

Wave 35-ish controller naming experiment used `/api/platform/emails/*` legacy namespace. Admin Wave 97 + email v1 namespace convention picked `/api/v1/email/*`. Option B Wave beta-readiness-2 Bucket D sync doc-vs-code without full rename do scope risk (10+ files multi-service rename) + context pressure (3 background agents fail autocompact thrash).

## Proposed Fix

Sequential approach (Wave 109+ standalone wave hoặc dedicated Bucket):
1. Rename `EmailController @RequestMapping` → `/api/v1/email`
2. Update 5 caller services Maven module-by-module
3. Update gateway routing `application.yml`
4. Update MockMvc tests + EmailConsumerTest URL mock
5. Update docs (api-contract.md + README + QUICK-START + 04-kitehub-prs.md)
6. `mvn verify` multi-module — verify zero `/api/platform/emails` hits post-rename
7. Live verify post-deploy → email send still works via new namespace

## Acceptance Criteria

- [ ] EmailController `@RequestMapping("/api/v1/email")` ship
- [ ] All callers updated: `grep "/api/platform/emails" kitehub/ kiteclass/ infrastructure/ documents/ scripts/` → 0 functional hits (historical refs in changelog OK)
- [ ] Gateway routing pattern updated
- [ ] api-contract.md revert Option B note + cite `/api/v1/email/send` canonical
- [ ] `mvn verify -P strict-warnings` multi-module PASS
- [ ] Live verify post-deploy (gated GAP-612 AWS restore) — email send works on new namespace

## Out-of-scope

- Backward-compat dual-routing (both `/api/platform/emails` + `/api/v1/email` active) — complexity not warranted; one-shot rename + caller updates same wave

## Priority Rationale (P2)

Functional correctness DONE via GAP-662 Option B (doc-vs-code aligned). Option A là consistency-only improvement — defer Phase 1.5 hoặc Wave 109+ standalone consolidation wave.

## Related

- GAP-662 — parent (Option B sync DONE Wave beta-readiness-2 Bucket D)
- Admin Wave 97 — v1 namespace pattern precedent
- `dev-readable-doc-language.md` — Vietnamese narrative compliance for follow-up PR

## Log

- **2026-05-24 (filed):** Wave beta-readiness-2 Bucket D ship Option B (doc-vs-code sync); Option A v1 namespace migration deferred do scope 10+ files + context pressure. Track follow-up wave standalone.
