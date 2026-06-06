---
title: Session handoff 2026-06-06 — Wave auth-1 merged + post-wave audit suite
audience: dev
created: 2026-06-06
wave: auth-1
---

# Session handoff 2026-06-06 (PM) — Wave auth-1 merge + post-wave audit

## Shipped this session

1. **Unblocked + merged PR #2186** (Wave auth-1 KC-native login) — `2b01ac93`. 4 red CI gates fixed from 2 auth-1 root causes:
   - **JWT_SECRET missing at boot** → `AuthTokenService` fail-fast (≥64 bytes HS512). Broke Test Core Service (287 ApplicationContext errors) + DB schema drift. Fix: test `jwt.secret` in `src/test/resources/application.yml` + `--jwt.secret` in `check-schema-drift.sh validate_kiteclass_core`. Verified: `OpenApiSpecExportTest` context loads.
   - **auth_credentials RLS_DISABLED** → DB RLS coverage gate. V89 intentionally pre-auth lookup (no tenant GUC). Fix: exempt `auth_credentials` in `check-rls-coverage.sh` (`NOT IN ('instances','auth_credentials')`).
   - Cross-flow sweep confirmed both fixes cover the only sites of each bug class.
2. **PR #2187** post-merge sync — `0c541ece`. GAP-798b OPEN→PARTIAL 17% (gateway X-User-Reference-Id forward shipped); GAP-725 note (parent+teacher pulled forward + shipped, student+OTP remain Phase 2); ROADMAP snapshot + wave-history.jsonl auth-1 entry.
3. **PR #2188** post-wave audit suite (per `post-wave-audit-mandate.md`, 3-day window done same-day). 3 Opus agents parallel → **no P0 across any** (merge was safe):
   - business-logic **64/100 C** PARTIAL FAIL · api-contract **85/100 B** PARTIAL FAIL · ops-readiness **71/100 C** PARTIAL
   - 27 findings → 6 gaps **GAP-1009..1014** (see below). Reports in `documents/04-quality/audits/{business-logic,api-contract,ops-readiness}/2026-06-06-wave-auth-1-*.md` + completion check in `audits/waves/`.

## Gaps filed (GAP-1009..1014, all phase-1-beta)

- **GAP-1009 (P1)** auth-1 business-doc completeness — create `kiteclass/tenant-auth` 3-layer + sync parent/student-portal Option A→B + BR-PARENT-004 flip
- **GAP-1010 (P1)** auth module zero automated test coverage (unit + AuthCredentialPostgresIT + MVC contract)
- **GAP-1011 (P1)** auth_credentials global email-unique vs multi-tenant collision (decide A: doc 1-email-1-tenant / B: unique(instance_id,email))
- **GAP-1012 (P1)** kc-tenant-auth login route no rate-limit + gateway HS512 key-check ≥32→≥64
- **GAP-1013 (P2)** auth credential hardening cluster (setPassword cross-entity / disable-on-deactivate / password policy / PII / jti / timing)
- **GAP-1014 (P1)** kc-core prod deploy parity — not in docker-compose.production.yml + PARENT_PORTAL_ENABLED override + secrets.tf HS512 desc

## State

- main HEAD: `0c541ece` (auth-1 + sync); PR #2188 (audit docs) auto-merging when CI green.
- AWS stack STOPPED (3 EC2 + RDS available) — storage cost only.
- Test creds (local): parent-walk@test.com/Walk@5678, teacher_a@test.com/Teach@1234.

## Remaining (next session)

- **Merge PR #2188** if not auto-merged (docs-only).
- **Auth-1 follow-ups:** GAP-1009 (docs) + GAP-1010 (tests) are the highest-value cleanups; GAP-1011 (email-collision) needs a product decision; GAP-1012 (rate-limit) is a quick security win.
- **Phase 2 auth:** student provisioning + KC-9 build (Bucket E) + OTP Hướng C (Zalo/SMS vendor-dependent) — per GAP-725.
- **Production parity:** GAP-1014 (kc-core prod deploy + JWT_SECRET consumer + PARENT_PORTAL_ENABLED) gated on KC-stack deploy (GAP-444) + AWS restore.
- **G2 human walks** still pending KC-1..8 (Flow Verification Campaign).
