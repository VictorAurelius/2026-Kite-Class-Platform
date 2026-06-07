# G3 production-parity walk — KC-11 P1 fixes (GAP-1039 reports leak + GAP-1040 SSRF)

**Ngày:** 2026-06-07
**Loại:** Post-fix re-walk per `pre-handoff-self-test-completeness.md` §3 + `feature-ship-runtime-walk-mandate.md` §3.4 — production-parity (gateway :9000, Postgres+Flyway+RLS).
**Trigger:** P3 G3 batch 2 — KC-11 had 2 OPEN P1 blockers (cross-tenant reports leak + document-gen SSRF) that would FAIL G3. Fixed via 2 parallel Opus agents (worktree-isolated, disjoint modules), integrated to `feature/kc11-p1-security-fixes`, rebuilt kiteclass-core.

## Stack-up

- kiteclass-core rebuilt từ `feature/kc11-p1-security-fixes` (octopus-merge GAP-1039 `838c7200` + GAP-1040 `f150db9d`) — healthy.
- Schema drift pre-check: `payments` + `attendance` tables có `instance_id` column trong Flyway `kiteclass_shared` (NOT the KC-5 entity↔migration drift class) — explicit predicate works production-parity.
- Fixture: payments tenant `aaaabbbb-…0001`=2,000,000 vs all-tenant=3,500,000 (matches gap walk evidence).

## Walk evidence (gateway :9000, minted HS512 JWT)

| Gap | Probe | Expected | HTTP / result | Verdict |
|---|---|---|---|---|
| GAP-1039 | ADMIN (tenantId=aaaabbbb) GET /api/v1/reports/revenue | scoped 2,000,000 NOT 3,500,000 | 200, `totalRevenue=2000000.00` | ✅ leak fixed — single-tenant scope |
| GAP-1039 | ADMIN no-tenantId-claim GET /reports/revenue | fail-closed (not all-tenant) | **400** | ✅ fail-closed (TenantContext → TenantNotSetException → 400) |
| GAP-1040 | OWNER POST /api/v1/documents/pdf/download `data.logoUrl=http://169.254.169.254/latest/meta-data/` | logoUrl stripped, no SSRF egress | 200 + log `Dropping caller-supplied fetch-able URL key 'logoUrl'... (SSRF guard, GAP-1040)` | ✅ caller URL stripped, server branding authoritative |

## Fixes applied (2 parallel Opus agents)

**GAP-1039 (reports module + config):**
- `RevenueReportRepository` / `AttendanceReportRepository`: explicit `WHERE p.instanceId = :tenantId` / `a.instanceId = :tenantId` (defense-in-depth, not Hibernate-filter-only)
- `ReportServiceImpl`: resolve `TenantContext.getCurrentTenant()` → 3-arg repo call; TenantNotSetException → 400
- `TenantFilterInterceptor`: fail-closed reject 400 scoped to `/api/v1/reports/**` (NOT blanket — preserves public paths)
- IT: `ReportTenantIsolationIT` (Testcontainers Postgres) — tenant A returns 2M not 3.5M; unknown tenant empty

**GAP-1040 (document module):**
- `DocumentBrandingAssembler`: server branding applied first; caller `*Url`/`*url` keys stripped+logged (closes null-branding bypass); non-URL keys keep caller precedence (escaped `th:text`)
- logoUrl host allowlist via `landing.allowed-image-hosts` (reuse GAP-827 sanitizer); disallowed host → skip (no egress, no 500)
- Tests: 8 SSRF cases + 7 updated (18 total)

## Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

- GAP-1039: 2 report repos FIXED; QualityReportRepository EXEMPT (no custom aggregate @Query); ~15 other-module aggregate repos DEFERRED (out of reports scope, mostly gateway-authenticated so filter active) → recommend separate triage wave.
- GAP-1040: invoice.html (only `th:src` template) FIXED via shared assembler covering pdf/xlsx/docx uniformly; docx/xlsx generators EXEMPT (no remote image fetch).

## RST→E2E promotion (per `e2e-rst-test-layer-boundary.md` §3)

Both fixes = functional/security regressions → IT regression guards added same PR: `ReportTenantIsolationIT` (cross-tenant scope) + `DocumentBrandingAssemblerTest` 8 SSRF cases. Prevents recurrence.

## Verdict

**G3 PASS cho KC-11** (reports + document-gen). 2 P1 cross-tenant/SSRF blockers fixed + verified production-parity. KC-11 advances `🔄 walk-pass-pending-human` với **G3 ✅** — chờ G2 human. Residual: GAP-1043 (P2 reschedule past-date, KC-12) + GAP-721 (Zalo stub) non-blocking; ~15 other-module aggregate repos → separate IDOR triage wave (DEFER per sweep).

Deploy note: fixes on `feature/kc11-p1-security-fixes`; production rebuild kiteclass-core image từ main post-merge (ECR) khi deploy AWS.
