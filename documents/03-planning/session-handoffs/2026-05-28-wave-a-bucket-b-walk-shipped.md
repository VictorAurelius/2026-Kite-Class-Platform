---
audience: dev
date: 2026-05-28
session-theme: Wave A Bucket B re-host kitehub-subscription canonical + RST walk PASS + 2 META rules shipped
prs_open: [1929]
prs_merged_this_session: []
gaps_closed: [GAP-786]
gaps_deferred_followup: [Bug #20 STAFF JWT, Bug #22 help pages 404, Bug #27 email brand mismatch, Bug #28 email HTML render]
meta_rules_shipped: [feature-ship-runtime-walk-mandate.md v1.1.0 §3.4, cross-flow-bug-class-sweep.md v1.0.0]
walk_status: PASS end-to-end (curl + human UI) — Bug #17 verified RESOLVED
context_at_close: ~85% Opus 1M
next_session_pickup: PR #1929 CI green → merge → Bucket A Bug #14 (kitehub-email @RabbitListener cho email.send queue)
---

# Session handoff — Wave A Bucket B walk shipped + META rules

## Session arc

User direction sáng 2026-05-28: "continue Plan D execution". 3 Opus 4.7 1M background agents investigated Buckets A/B/C song song:

- **Bucket A** Bug #14 email: kitehub-email NO `@RabbitListener` on `email.send` queue → breaks ALL 17 queued email types
- **Bucket B** Bug #17: original Options A/B/C all non-viable (cross-DB blocker) → user-confirmed **Option D re-host kitehub-subscription canonical**
- **Bucket C** GAP-704 JWT: already DONE Wave 104 + Wave 105; only IT test pending (~80 LOC)

Ship Bucket B end-to-end (code + walk) trong session này. RST walk surfaced 8 bugs total (4 fixed batch + 4 deferred). 2 META rules shipped trong walk session.

## PR #1929 commits (5 total)

1. Initial Bucket B (gateway routing revert + delete kiteclass-core staff 13 files + V72 Flyway deprecation)
2. RST walk evidence + META rule `feature-ship-runtime-walk-mandate.md` v1.1.0 §3.4
3. action-2.md sync (always-commit-action-scratchpad rule)
4. FE batch fixes (Bug #21 + #26) + META rule `cross-flow-bug-class-sweep.md` v1.0.0
5. TS strict null-check fix (Bug #29 build-time)

## Bug catalog từ walk (8 total)

| Bug | Class | Severity | Trạng thái |
|---|---|---|---|
| #18 Gateway whitelist `isPublicPath()` missing public paths | P0 | ✅ Fixed |
| #19 Subscription SecurityConfig default-deny on public paths | P0 | ✅ Fixed |
| #21 FE apiClient thiếu X-Tenant-Id header | P0 | ✅ Fixed (interceptor + JWT decoder) |
| #26 FE↔BE DTO mismatch sau re-host | P0 | ✅ Fixed |
| #29 TS strict null-check trong getTenantIdFromToken | (self-introduced) | ✅ Fixed |
| #20 STAFF JWT thiếu tenantId claim | P1 | ⏭️ Follow-up gap (architectural) |
| #22 Help pages 404 (Wave 79 scope drift) | P2 | ⏭️ Follow-up gap |
| #27 Email brand mismatch (`@kiteclass.com` vs kitehub.me) | P1 | ⏭️ Follow-up gap |
| #28 Email HTML render plain text (template/Content-Type issue) | P1 | ⏭️ Follow-up gap (sweep 17 templates) |
| #23 #24 #25 CSP/CORS/consent | P3 | ⏭️ Defer |

## Walk evidence (2-phase verification)

| Step | Curl walk (coordinator) | Human UI walk (user) |
|---|---|---|
| Owner login + JWT tenantId | ✅ | ✅ |
| Routing → kitehub-subscription | ✅ | ✅ (post Bug #21 fix) |
| Email arrived MailHog | ✅ | ✅ image-6.png evidence |
| Recipient accept + user created | ✅ DB row `a2da980b` | ✅ DB row `d80b2955` |
| Staff login PASS | ✅ HTTP 200 | ✅ user confirmed |

GAP-786 verified RESOLVED end-to-end qua cả 2 walk types.

## 2 META rules shipped (force-multiplier)

### `feature-ship-runtime-walk-mandate.md` v1.0.0 → v1.1.0

Added §3.4 "Catalog-then-batch-fix walk workflow":
- Walk surfaces bug → CATALOG (file path + symptom + workaround) → continue walk
- Reach end-of-walk → BATCH fix all bugs → single Docker rebuild → re-walk
- Banned inline-rebuild thrash (3 rebuilds for 2 bugs ~210s vs 1 rebuild ~120s)
- Self-test on this very session

### `cross-flow-bug-class-sweep.md` v1.0.0 (NEW)

Codify user direction "fix 1 bug ở 1 flow → check flow khác":
- Identify bug class signature → grep similar sites → FIX/DEFER/EXEMPT per site
- Sister rule cho `audit-to-gap-pipeline.md` §2.7 (inverse direction)
- Self-test on Bug #21 originating incident (apiClient interceptor + 4-site sweep)

## State at close

- **Main HEAD:** `4aa6d054` (unchanged — Bucket B PR #1929 chưa merge)
- **Branch active:** `wave/phase2-beta-wave-a-bucket-b-rehost-staff-invitations` (5 commits ahead)
- **Docker stack:** healthy, Docker FE restarted with rebuild (image hiện tại post-fix codebase)
- **GAP-786:** DONE 100%, file moved `phase-1-beta/` → `phase-1-beta/closed/`
- **PR #1929:** CI rebuilding với latest commit, mergeable post-CI

## Next session pickup (priority order)

1. **PR #1929 CI green check → merge** (docs-only auto-merge eligible cho post-merge follow-up)
2. **File 4 follow-up gaps** cho deferred Bugs #20, #22, #27, #28
3. **Bucket A** Bug #14 — kitehub-email `@RabbitListener(queues="email.send")` (~2-3 ed) — fixes 17 email types
4. **Bucket C** GAP-704 Testcontainers IT (~80 LOC, ~0.5-1 ed)
5. **Bucket D** Course/Class CRUD verify walk (~2 ed)
6. **Wave A closure PR** sau cả 4 Buckets shipped + wave-history.jsonl append

## Tooling state

- FE Docker `:3001` đang chạy (NEW image với Bug #21+#26 fixes)
- Backend stack healthy (postgres, redis, rabbitmq, minio, mailhog, gateway, subscription, email, admin, branding, kiteclass-core)
- AWS stack STOPPED (per Phase 1 BETA idle cost saving)

## References

- Wave A plan: `documents/03-planning/waves/wave-2026-05-28-phase2-beta-wave-a-p0-bug14-bug17-jwt-crud.md`
- GAP-786 closure: `documents/04-quality/gaps/phase-1-beta/closed/GAP-786-staff-invite-accept-user-provision-missing.md`
- META rule v1.1.0: `.claude/rules/feature-ship-runtime-walk-mandate.md`
- META rule v1.0.0 (NEW): `.claude/rules/cross-flow-bug-class-sweep.md`
- Walk findings RST: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md`
