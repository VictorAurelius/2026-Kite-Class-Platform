---
title: Wave 104 Bucket E — Partial Live Verify (Infrastructure + Admin Login Baseline)
status: partial
audience: dev
created: 2026-05-22
phase: phase-1-beta
wave: 104
gaps: [GAP-710]
scope: Bucket E Step 1-1.5 + Admin login baseline (steps 2-7 deferred to Wave 104.5)
---

# Wave 104 Bucket E — Partial Live Verify

## TL;DR

Bucket E live verify partial: **infrastructure cascade resolved + 3 services rebuilt với Wave 104 code + admin login baseline PASS**. Remaining 5 verify steps (Owner walk + Mailhog × 5 email types + 2FA via gateway + GAP-707 log scan + comparison audit) defer Wave 104.5 dedicated session per GAP-710 cluster.

| Step | Status | Evidence |
|---|---|---|
| 1. Pre-check infra + images | ✅ PASS | §2 below |
| 1.5. Infra cascade recovery | ✅ PASS (unplanned) | §3 below |
| 1.6. 3-service rebuild post-Wave-104 | ✅ PASS | §4 below |
| 2. Admin login baseline | ✅ PASS | §5 below |
| 2-Bucket-A. Owner walk + `/onboarding-progress` 200 | ⏳ DEFER 104.5 | needs owner persona signup → approve → login flow |
| 3-Bucket-B. Mailhog approval email +1 | ⏳ DEFER 104.5 | dependent on owner walk |
| 4-Bucket-B. Mailhog × 5 email types multipart + List-Unsubscribe | ⏳ DEFER 104.5 | |
| 5-Bucket-C. 2FA enroll via gateway:9000 | ⏳ DEFER 104.5 | needs admin 2FA setup |
| 6-Bucket-D. GAP-707 5×login + WARN absence | ⏳ DEFER 104.5 | |
| 7. Comparison audit pre-fix vs post-fix | ⏳ DEFER 104.5 | covers above |

---

## 1. Session context

- **Session date:** 2026-05-22 (UTC, per currentDate)
- **Wave 104 merge:** 2026-05-22T09:17:13Z (commit `b5ec57d6` PR #1712 squash)
- **Wave 104 closure-sync:** 2026-05-22T~09:35Z (commit `2aa2898d` PR #1713)
- **Pickup context:** Previous session (~06:30 UTC) drafted "Path A" recovery plan with 4 steps; this session executed steps 1-2 partial + identified remaining 5 deferred to Wave 104.5.

## 2. Pre-check infrastructure

Initial stack inspection found cascade failure:

```
kite-postgres    Exited (255) ~1h ago
kite-rabbitmq    Exited (255) ~1h ago
kite-redis       Exited (255) ~1h ago
kite-minio       Exited (255) ~1h ago
kite-mailhog     Exited (255) ~1h ago
kitehub-email    Up (unhealthy, FailingStreak=254)
kiteclass-core   Restarting (1)
5 backend services Up (health: starting loop)
```

**Diagnosis:** Exit code 255 = killed by signal (likely OOM hoặc Docker Desktop restart). 5 infra containers cùng exit same window → single event killed cả group. kitehub-email unhealthy due to RabbitMQ unreachable (`UnknownHostException: kite-rabbitmq`), not email service bug. Backend services in `health: starting` loop because dependencies dead.

Per `pre-handoff-self-test-completeness.md` §2.4 (a-c) — infrastructure precondition fails before any auth/UI flow can verify.

## 3. Infrastructure cascade recovery

```bash
bash kitehub/scripts/up.sh
```

Outcome: 13/13 services healthy post-restart. All 5 infra containers restored healthy. App services exit `starting` loop within ~60s. Verified:

```
kite-postgres / kite-redis / kite-rabbitmq / kite-minio / kite-mailhog — healthy
kitehub-admin / branding / email / subscription / frontend — healthy
kiteclass-core / kiteclass-frontend — healthy
kite-gateway — healthy
```

## 4. 3-service rebuild for Wave 104 code load

Wave 104 merged 09:17 UTC; container images created 2026-05-21T04:49Z (~29h pre-merge) → live verify against stale images would test PRE-fix code = invalid.

Rebuild sequential (RAM-safe):

| Service | Time | New image timestamp | Wave 104 buckets embedded |
|---|---|---|---|
| `kitehub-subscription` | 6m34s | 2026-05-22T10:15:08Z | Bucket A (JWT tenantId) + B1 (approval email wire) + C2 (ChallengeTokenAuthenticationFilter) + D (LoginAudit cooldown) |
| `kitehub-email` | 2m11s | 2026-05-22T10:17:41Z | Bucket B2 (multipart + RFC 8058 List-Unsubscribe) |
| `kite-gateway` | 2m26s | 2026-05-22T10:22:11Z | Bucket C1 (HS256 challenge token filter) |

All 3 timestamps post Wave 104 merge (09:17 UTC) ✅. Total rebuild ~11 min wall-clock.

### Side-fix: rebuild.sh `gateway` mapping bug

Discovered: `bash kitehub/scripts/rebuild.sh gateway` failed với `no such service: kitehub-gateway`. Root cause: script auto-prepends `kitehub-` prefix when service name doesn't start with `kitehub-` or `kiteclass-`, but actual compose service name = `kite-gateway` (`kite-*` prefix). Same gap likely exists for any future `kite-*` services (kite-mailhog, kite-prometheus, etc. — though these don't typically rebuild).

Patch (single-line):

```diff
-# Add kitehub- prefix if not present (skip kiteclass-* services)
-if [[ "$SERVICE" != kitehub-* ]] && [[ "$SERVICE" != kiteclass-* ]]; then
-    SERVICE="kitehub-$SERVICE"
-fi
+# Special case: gateway compose service is 'kite-gateway' (not 'kitehub-gateway')
+if [ "$SERVICE" = "gateway" ] || [ "$SERVICE" = "kitehub-gateway" ]; then
+    SERVICE="kite-gateway"
+# Add kitehub- prefix if not present (skip kiteclass-* and kite-* services)
+elif [[ "$SERVICE" != kitehub-* ]] && [[ "$SERVICE" != kiteclass-* ]] && [[ "$SERVICE" != kite-* ]]; then
+    SERVICE="kitehub-$SERVICE"
+fi
```

Verified: `bash kitehub/scripts/rebuild.sh gateway` now resolves to `kite-gateway` correctly.

## 5. Admin login baseline (Wave 104 buckets transitively verified)

```bash
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'
```

**First attempt:** HTTP 503 (cold-JVM circuit breaker — gateway fallback HTML "Auth service tạm ngưng"). Cold call exceeded gateway circuit-breaker timeout threshold.
**Retry attempt:** HTTP 200, JWT issued, time 0.26s.

Response JWT payload (decoded base64 middle segment):

```json
{
  "sub": "00000000-0000-0000-0000-000000000099",
  "email": "admin@kitehub.com",
  "role": "PLATFORM_ADMIN",
  "type": "access",
  "iat": 1779445624,
  "exp": 1779532024
}
```

### Bucket A transitive observation

Admin JWT has **NO `tenantId` claim** — this is CORRECT per Wave 104 Bucket A AC. Bucket A scope explicitly: "Add tenantId claim to JWT for tenant-scoped users (OWNER / TEACHER / PARENT / STUDENT in Phase 1 BETA)". PLATFORM_ADMIN spans tenants by design → excluded from enrichment. Cannot fully verify Bucket A AC without OWNER persona JWT (defer Step 2-Bucket-A to Wave 104.5).

### Bucket C1 transitive observation

Gateway routes login via `kite-gateway` 9000 → `kitehub-subscription` 8081 successfully (proves Bucket C1 HS256 challenge filter doesn't break standard JWT path). Full Bucket C1 verify (challenge token path `/api/v1/auth/2fa/**`) requires Step 5-Bucket-C 2FA enrollment.

### Bucket B / D status

Not exercised in this session — defer to Wave 104.5.

## 6. Path A deviation rationale

Previous session's "Path A" outlined 4 steps: diagnose email + rebuild + live verify 4 patterns + audit doc. This session executed:

- Step 1 (diagnose): root cause = infra cascade, not email bug → Step 1.5 added (infra restart)
- Step 2 (rebuild): completed all 3 services
- Step 3 (live verify): admin login baseline done; remaining 5 steps defer Wave 104.5
- Step 4 (audit doc): this document covers steps 1-2

Defer rationale: remaining 5 verify steps require coordinated sequencing (owner signup → admin approve → owner login → endpoint test; 2FA enrollment + via-gateway challenge token; Mailhog inspection per email type; log scan). Each step needs careful state tracking that benefits from dedicated session context.

## 7. Wave 104 PARTIAL gap status — unchanged this session

Per Wave 104 plan §3 Bucket E AC, status update gated by ALL 6 verify steps. With only Step 1-2 done, no Wave 103 PARTIAL gaps advance:

| Gap | Pre-Wave-104 % | Post-this-session % | Path-to-100 |
|---|---|---|---|
| GAP-531 | 70% | 70% | Wave 104.5 Step 2 Owner walk live |
| GAP-516 | 90% | 90% | Wave 104.5 Step 5 2FA via gateway |
| GAP-543 | 65% | 65% | Wave 104.5 Step 4 Mailhog × 5 types |
| GAP-657 | 40% | 40% | Wave 104.5 Step 4 List-Unsubscribe verify |
| GAP-659 | 50% | 50% | Wave 104.5 persona-tone verify (out-of-scope) |
| GAP-702..707 (Wave 104 P0/P1/P2) | NEW | NEW (not flipped DONE) | Wave 104.5 Step 2-6 verify |

## 8. Cross-links

- Wave 104 plan: `documents/03-planning/waves/wave-2026-05-22-104-fix-followup-bugs.md` §3 Bucket E
- Follow-up cluster: `documents/04-quality/gaps/phase-1-beta/GAP-710-wave-104-follow-up-cluster.md`
- Wave 103 walk patterns (re-trigger source):
  - `documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md`
  - `documents/04-quality/audits/local-stack/2026-05-22-wave-103-admin-persona-walk.md`
  - `documents/04-quality/audits/local-stack/2026-05-22-wave-103-2fa-totp-walk.md`
  - `documents/04-quality/audits/local-stack/2026-05-22-wave-103-email-mailhog-verify.md`
- Session handoff: `documents/03-planning/session-handoffs/2026-05-22-wave-104-handoff.md` §"Next session pickup"

## 9. Next session pickup (Wave 104.5)

Recommended order:

1. **Step 2 — Owner persona walk** (largest single piece, ~20 min):
   - Signup via landing page
   - Admin approve via `POST /api/v1/admin/beta-requests/:id/approve`
   - Owner login → extract JWT → decode → verify `tenantId` claim present
   - `curl GET /api/v1/onboarding-progress` Bearer Owner JWT (NO X-Tenant-Id) → expect HTTP 200
2. **Step 3 — Mailhog approval email** (~5 min): inspect localhost:8025 for +1 email
3. **Step 4 — 5 email types × Mailhog** (~15 min): trigger each, verify multipart + List-Unsubscribe
4. **Step 5 — 2FA via gateway** (~10 min): admin → 2FA enroll-init via :9000 với challenge JWT → expect 200
5. **Step 6 — GAP-707 log scan** (~5 min): 5× login + `docker logs kitehub-subscription | grep "Query did not return a unique result"` absence
6. **Step 7 — Comparison audit** (~10 min): pre/post table per gap

Total estimate: ~65 min sequential; ~45 min if buckets share state setup.
