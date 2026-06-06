# Session handoff — 2026-06-07 (KC-1 saga LIVE + P2 fix wave complete; P3 G3 re-walk next)

**Scope:** KC-1 provisioning saga unblocked end-to-end + G3-before-G2 reorder + P2 security/gateway fix wave closed. NEXT = P3 G3 production-parity re-walks (~17 flow) — large coordinator-serial phase, **start fresh session** (this session hit ~68% context).

## Shipped this session

| PR | Scope | Status |
|----|-------|--------|
| #2223/#2225/#2224 | KC-1 pre-walk HIGH fixes: GAP-949 audit afterCommit + GAP-945 status PENDING→TRIAL + GAP-953 retry idempotent (3 parallel Opus agents) | ✅ merged |
| #2226 | **KC-1 saga keystone unblock**: GAP-1045 (RabbitMQ Jackson converter → Message-param decode) + GAP-1047 (saga TenantContext → instance_id auto-set) | ✅ merged |
| #2227 | G3 production-parity plan doc (campaign reorder G1→G3→G2) | ✅ merged |
| #2228 | Gateway 5 carve-out (C1-C5) + META `audit-gateway-routes.sh` fix (BS#1 scan kiteclass-core + BS#2 TenantResolver-400) — GAP-1042/GAP-1049 | ✅ merged |
| #2229 | Residual InstanceController IDOR (PUT/PATCH ownership guard + GET/owner requireSelfOrAdmin) — GAP-1050 P0 | ⏳ CI pending → merge |

## KC-1 walk evidence (walk#3, production-parity local stack)
signup → saga `initiate→GENERATING→DEPLOYED` (frontend_instance instance_id populated) → TENANT_PROVISIONED audit → tenant-ready email queued → duplicate tenant.created idempotent-skip. Keystone saga **LIVE end-to-end** (was dead — 2 stacked P0 hidden by 8 PARTIAL gaps).

## KEY DISCOVERY — P2 fix wave mostly pre-done
Campaign §4 "blocker" column STALE: GAP-1015/1019/1023/1025/1031/1034/1035/1041 all **DONE** (Wave security-2, 2026-06-06). P2 reduced to: #2228 (5 new gateway collisions) + #2229 (residual InstanceController IDOR). Both shipped. **P2 fix wave COMPLETE.**

## NEXT SESSION — P3 G3 production-parity re-walks
Per `documents/03-planning/roadmap/g3-production-parity-plan.md` P3. ~17 flow chưa-G3 (G1 pass, fixes shipped) cần walk qua **gateway :9000 (JWT→header thật) + Postgres+Flyway+RLS + prod-profile**, verify cross-tenant isolation hold. Coordinator-serial (shared runtime stack — không parallelize qua agents an toàn). Batch theo readiness. Flip campaign §4 → G3 PASS per flow.

### Prerequisites cho P3
1. Merge #2229 nếu chưa.
2. Rebuild gateway (đã có #2228 carve-out) + walk 5 carve-out routes (C1-C5) via :9000 — verify GAP-1042/1049 runtime + flip DONE.
3. Stack: local prod-parity (KHÔNG cần AWS — stopped cost-save). Rebuild stale services per `pre-walk-static-audit-bundle.md` (check-stale-images.sh).

### KC-1 closure remaining (provisioning-1)
GAP-953 retry() path (force FAILED instance → admin retry), GAP-954 delete cascade, GAP-947 TenantSettings GET/PUT — sub-walks → flip GAP-945/946/947/948/952/953/954 DONE.

### Open follow-ups
- GAP-1046 (sweep 4 remaining raw-String RabbitMQ consumers — KC-12/email flows)
- GAP-1048 candidate: `EntityPersistenceListener` System.err.println debug spam cleanup
- TenantOwnershipGuard duplicated 2 modules (subscription + branding) — tech-debt sync

## Stack state
Local: kitehub-subscription + kiteclass-core rebuilt with fixes (LIVE saga). AWS STOPPED (cost-save). Gateway NOT yet rebuilt with #2228 carve-outs → P3 prerequisite.
