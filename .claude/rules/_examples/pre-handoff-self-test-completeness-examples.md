---
parent_rule: pre-handoff-self-test-completeness.md
purpose: deferred-load §Self-test + §Worked example for context budget compliance
---

# pre-handoff-self-test-completeness — Examples / Self-test

Companion to `.claude/rules/pre-handoff-self-test-completeness.md`. Body moved here per Wave 76 Bucket E streamline.

## Worked self-test — Wave 71b 2026-05-13 incident

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
| (d) Approve action reaches kitehub-subscription | ⚠️ partially (curl GET works, button POST not tested) | browser click → network inspect |

**Verdict:** §2.4 (a)+(b)+(c) all FAIL retroactively. Self-test PASS as a worked example proving the rule fires on the originating incident.

**Cost of the miss:** ~1 user round-trip to discover bugs that should have been surfaced at Wave 71 closure. GAP-518 (role mismatch) + GAP-519 (admin nav missing) filed as P0 follow-ups.
