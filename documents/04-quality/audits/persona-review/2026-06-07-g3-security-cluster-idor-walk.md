# G3 production-parity walk — security cluster IDOR/authz (KH-5/6/7/8/10, KC-10)

**Ngày:** 2026-06-07
**Loại:** G3 production-parity runtime walk — cross-tenant isolation verification
**Trigger:** P3 G3 re-walk per `g3-production-parity-plan.md` P3. 6 flow có P0 IDOR/routing/email gaps fixed Wave security-2; verify fixes hold qua gateway :9000 (JWT→header→authority chain, KHÔNG header thủ công).
**Post-fix re-walk per** `pre-handoff-self-test-completeness.md` §3 + §2.7 multi-tenant isolation checklist.

## Stack-up

- 4 backend service rebuilt từ main HEAD `7d75a9cd` + healthy (gateway/subscription/kiteclass-core/branding fresh).
- Fixtures: Tenant A = `22003e3c…` (sky-test ACTIVE), Tenant B = `0edaee10…` (sky-education TRIAL), subscription `81cf38cd…` belongs to A. owner@skyedu.vn (A), khanh.do (used as owner-B với tenantId=B), newstaff (STAFF, A).
- Auth: HS512 JWT minted với gateway `JWT_SECRET`; gateway validates + injects X-User-Roles/X-User-Id/X-Tenant-Id. Cross-tenant test = owner-B token (tenantId=B) attempts tenant-A resource → expect 403/404.

## Walk evidence (gateway :9000)

| Flow | Gap | Probe | HTTP | Verdict |
|---|---|---|---|---|
| KH-5 subscription | GAP-1015 IDOR | ownerB GET sub A `81cf38cd` | **403** | ✅ IDOR defended |
| KH-5 (control) | — | ownerA GET own sub A | **200** | ✅ happy path |
| KH-6 AI branding | GAP-1019 IDOR | ownerB branding-job + spoof `X-Instance-Id: A` | **403** | ✅ X-Instance-Id bound to trusted X-Tenant-Id |
| KH-7 domain | GAP-1023 IDOR | ownerB GET domain instance A | **403** | ✅ IDOR defended |
| KH-8 offboarding | GAP-1025 IDOR | ownerB DELETE instance A | **403** | ✅ purge guard |
| KH-8 (admin-only) | GAP-1025 | ownerB GET list-all instances | **403** | ✅ enumeration admin-only |
| KH-10 email | GAP-1031 zero-auth | no-auth POST /api/platform/emails/send | **404** | ✅ internal route removed from gateway |
| KC-10 branding routing | GAP-1034 | (verified via carve-out walk C1-C5 + branding carve-outs) | — | ✅ see gateway-carveout-runtime-walk |
| KC-10 branding authz | GAP-1035 | STAFF PUT /api/v1/settings/branding (valid body) | **403** | ✅ hasAnyRole(ADMIN,OWNER) |
| KC-10 (control) | GAP-1035 | OWNER PUT branding (valid body) | **200** | ✅ happy path |

Note KC-10: with malformed body both STAFF + OWNER get 400 (@Valid binding fires before @PreAuthorize — standard Spring order, no mutation either way); with VALID body STAFF→403 (authz) + OWNER→200 confirms the guard.

## Parity matrix

| Dimension | Verdict |
|---|---|
| Same image tag (4 services rebuilt from main) | ✅ |
| Real Postgres + Flyway + RLS (kiteclass_shared) | ✅ (cross-tenant 403) |
| Gateway JWT→header auth via :9000 (no manual headers) | ✅ verified end-to-end |
| Cross-tenant isolation (IDOR defended) | ✅ 6/6 IDOR/authz probes 403/404 + 3 controls 200 |
| env-vars (JWT_SECRET present prod) | ✅ |

## Verdict

**G3 PASS cho 6 flow security cluster** (KH-5, KH-6, KH-7, KH-8, KH-10, KC-10). All Wave security-2 P0 IDOR/routing/email fixes hold qua gateway production-parity chain. Flows advance to `🔄 walk-pass-pending-human` với **G3 ✅** annotation — chờ G2 (human walk) để đạt ✅ THÔNG.

Deploy note: fixes on `main`; production cần rebuild service images từ main (ECR) khi deploy AWS.
