---
title: Wave auth-1 Completion Check
audience: dev
created: 2026-06-06
wave: auth-1
---

# Wave auth-1 Completion Check (2026-06-06)

**Wave:** auth-1 — KC-native login (Option B), parent+teacher persona auth pulled forward from Phase 2
**PRs merged:** #2186 (feature, `2b01ac93`) + #2187 (post-merge sync, `0c541ece`)
**Main HEAD:** post-`0c541ece`

## Results (per `wave-completion-check.md` levels)

| Level | Check | Status | Detail |
|-------|-------|--------|--------|
| 0 | No false-DONE gaps | ✅ | GAP-725 PLANNED (parent+teacher shipped, student+OTP remain Phase 2); GAP-798b PARTIAL 17% — both honest, not DONE |
| 1 | CI green on main | ✅ | #2186 all gates green (after JWT_SECRET + RLS-exempt fix); #2187 docs gates green |
| 2 | Integration consistency | ✅ | No conflict markers; gateway X-User-Reference-Id strip+reinject consistent; JWT HS512 issuer↔gateway (gateway keylen mismatch tracked GAP-1012) |
| 3 | Business logic | ⚠️ | Logic correct (BCrypt/HS512/anti-spoof/entity_type CHECK/tenant binding/uniform-401); multi-tenant email-unique edge tracked GAP-1011 |
| 5 | Doc sync | ⚠️ | gap-status.csv + ROADMAP + wave-history synced (#2187). Business 3-layer docs NOT updated same-PR (Living Docs gap) → tracked GAP-1009 |
| 7 | Audit suite (≤7d) | ✅ | business-logic 64/100 + api-contract 85/100 + ops-readiness 71/100 (all 2026-06-06); security 2026-06-05 ≤7d; no FE → ui-review N/A |

## Post-wave audit suite outcome

| Audit | Score | Verdict | Gaps |
|-------|-------|---------|------|
| Business-logic | 64/100 C | PARTIAL FAIL (no P0) | GAP-1009/1010/1011/1013 |
| API-contract | 85/100 B | PARTIAL FAIL (no P0) | GAP-1009/1010 |
| Ops-readiness | 71/100 C | PARTIAL (no P0) | GAP-1012/1013/1014 |

**No P0 across any audit** — confirms the merge was safe. Score loss concentrated in: business-doc drift (Living Docs not updated same-PR), zero automated auth tests, multi-tenant email-unique edge, and production deploy parity (kc-core not in prod compose).

## Gaps filed (6)

- GAP-1009 (P1) auth-1 business-doc completeness — tenant-auth 3-layer + portal Option B sync
- GAP-1010 (P1) auth module zero test coverage
- GAP-1011 (P1) auth_credentials global email-unique vs multi-tenant
- GAP-1012 (P1) kc-tenant-auth rate-limit + gateway HS512 keylen ≥64
- GAP-1013 (P2) auth credential hardening cluster
- GAP-1014 (P1) kc-core prod deploy parity + PARENT_PORTAL_ENABLED + secrets.tf desc

## Verdict

✅ **Wave auth-1 complete** — feature merged, CI green, audit suite run + findings tracked. `post-wave-audit-mandate.md` 3-day obligation satisfied same-day. Remaining auth work (student provisioning + KC-9 + OTP Hướng C) is Phase 2 per GAP-725; production deploy parity + hardening tracked GAP-1009..1014.
