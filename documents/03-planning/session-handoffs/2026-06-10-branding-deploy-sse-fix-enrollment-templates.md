---
title: Session Handoff — Branding deploy SSE fix saga + enrollment/import templates
audience: dev
created: 2026-06-10
scope: G2 browser-walk bug fixes (AI Branding deploy) + 3 import/enrollment features + 2 architecture reports
branch: feature/tier-ui-fix-g2-browser-2026-06-09
push_status: NOT pushed (per no-push-without-explicit-ask) — all commits local
---

# Session Handoff 2026-06-10

## TL;DR

Session driven by user G2 browser-walks. Shipped (local commits, NOT pushed):
1. **AI Branding deploy-stream bug saga** — 5 distinct bugs, ALL fixed; **root cause = EventSource relative URL**. Re-walk pending user confirm → flip GAP-1105 DONE.
2. **E: KH sales-lead /contact (GAP-1101)** — ✅ DONE (runtime-walk PASS).
3. **A: student bulk-import template (GAP-1102)** + **B: enroll dialog + bulk-enroll xlsx (GAP-1103/1104)** — cherry-picked, agent-tested green, ⏳ PARTIAL (need stack rebuild + runtime-walk).
4. **DOC-2 multi-tenant landing report** — ✅ integrated. **DOC-1 SSE/branding report** — ⚠️ STALE (worktree base from main lacked GAP-1021 feature work), NOT integrated, needs redo.

## 1. AI Branding deploy-stream bug saga (GAP-1105) — root cause found

User re-walked wizard Step 6 (Phê duyệt) repeatedly; backend deploy SUCCEEDED every time (lifecycle → DEPLOYED) but FE showed errors. 5 bugs, all FE-display:

| # | Bug | Root cause | Fix (commit) | State |
|---|---|---|---|---|
| 1 | `/lifecycle/events` 500 | Postgres 42P18 `(:since IS NULL OR ...)` untyped param (H2 masked) | drop null branch — `BrandingLifecycleEventRepository` | ✅ verified BE 200 |
| 2 | "Tiến trình" stuck, UI shows jobId as Instance | FE polled lifecycle/events with **jobId** not instanceId | capture `job.tenantId` → wizard `instanceId` → DeployingStep (`Step6Preview.tsx` + `wizard-shared.tsx`) | ✅ user-confirmed (UI shows `7862ab7e` + full history) |
| 3 | STREAM_DISCONNECTED post-complete | EventSource native error after clean stream close | `completedRef` guard in `useDeployStream.ts` | ✅ (hardening; kept) |
| 4 | "Lỗi triển khai (UNKNOWN)" | EventSource native error delivered to named-'error' listener (null data) → `errorCode ?? 'UNKNOWN'` | ignore named-'error' when `!e.data` | ✅ (hardening; kept) |
| 5 | **STREAM_DISCONNECTED (real root cause)** | **EventSource relative URL `/api/v1/...` resolved against frontend origin `:3001` → Next.js 404** (axios baseURL points to gateway `:9000`, EventSource ignored it) | prepend `NEXT_PUBLIC_API_URL` (gateway base) — `useDeployStream.ts` (commit `db8cfa8f`) | ⏳ **re-walk pending user confirm** |

**Bug 5 explains all SSE symptoms** — the stream NEVER reached the gateway (Network tab: `localhost:3001/...deploy-stream` → 404 Next.js). Bugs 3+4 guards are symptom-level but kept (harden genuine disconnects).

**NEXT (re-walk verify):** user walks Step 6 → expect deploy-stream connect 200, progress, complete; NO STREAM_DISCONNECTED/UNKNOWN. **Watch for CORS** — EventSource now cross-origin (`:3001`→`:9000`) with `withCredentials:true`; apiClient cross-origin works so gateway CORS likely OK, but if re-walk shows CORS error (not 404) → fix gateway CORS allow-credentials for `:3001` on deploy-stream route. After clean walk → flip GAP-1105 PARTIAL→DONE.

## 2. Features shipped this session

- **GAP-1101 (E)** ✅ DONE — KH PLATFORM sales-lead `/contact` full-stack. Runtime-walk PASS (POST 201 + DB row + sad-paths 400). Caught+fixed `@EntityScan` miss (`SalesLead` not registered → boot crash; sweep clean). Migration renumbered V68→**V69** (V68 collision). `closed/`.
- **GAP-1102 (A)** PARTIAL/85 — student bulk-import `.xlsx` template: BE `XlsxTemplateGenerator` + `GET /api/v1/students/bulk-import/template` + FE "Tải template mẫu" on `/admin/bulk-import` + docs BR-BI-007. Agent tests BE 16/16 + FE 8/8. **Pending: rebuild + walk.**
- **GAP-1103 (B1)** PARTIAL/85 — FE "Thêm học sinh vào lớp" dialog on `classes/[id]` (BE `POST /api/v1/enrollments` pre-existing). **Pending: walk.**
- **GAP-1104 (B2)** PARTIAL/85 — bulk-enroll module: `GET .../enrollments/bulk-import/template` + preview/commit (cols `student_email|student_phone|class_code|tuition_amount|discount_percent|note`) + FE `classes/[id]/bulk-enroll`. Agent tests BE 12/12 + FE 6/6 + prod build. **Pending: walk.**

**NEXT (enrollment walk):** `bash kitehub/scripts/rebuild.sh core` (kiteclass-core for B2 BE) + `rebuild.sh frontend`... NOTE kiteclass-frontend rebuild — verify service name. Then runtime-walk: (a) GET student template → 200 + xlsx parses; (b) enroll dialog → POST 201; (c) bulk-enroll GET template + POST preview/commit → 201. Watch for `@EntityScan` miss like GAP-1101 (kiteclass-core — bulk-enroll reuses existing entities, low risk). Flip GAP-1102/1103/1104 DONE after walk.

## 3. Architecture reports (user-requested)

- **DOC-2** ✅ `documents/02-architecture/tenant-domain-landing-architecture.md` §7 added (FE-render chain: Host→slug→instanceId → branding package → CSS var inject → TemplateRenderer). Reconciled stale "no middleware" claims (middleware.ts/resolveTenant exist GAP-811/813). Commit `2334f5a1`.
- **DOC-1** ⚠️ NOT integrated. Agent worktree base lacked GAP-1021 feature work → its `ai-branding-deploy-flow.md` describes pre-GAP-1021 state (claimed no MockProvisioningService/token-in-query/completedRef — all DO exist on feature branch). **Redo:** write SSE + branding-job-lifecycle report grounded on CURRENT feature-branch code (DeployStreamController poller 2s + heartbeat 30s + 6 event types; useDeployStream EventSource token-in-query + completedRef + absolute-URL fix; MockProvisioningService.provisionAsync; lifecycle state machine ADR-004). Worktree `worktree-agent-a34070804e118d335` exists but stale — write fresh, don't cherry-pick.

## 4. Deferred (filed, OPEN)

- **GAP-1106** P1 — subscription cursor queries Postgres 42P18 `(:cursorId IS NULL OR ...)` sweep (`InstanceRepository:143`, `PaymentRepository:84,90`). Verify Testcontainers + split-query fix per GAP-1028 precedent + ship CI detector `:param IS NULL OR`.
- **GAP-1107** P1 — branding mock-provision (1) rollback-only INTERMITTENT (`Transaction silently rolled back` during REGENERATE; audit-service-isolation class; needs repro + REQUIRES_NEW/txn-boundary fix) + (2) `AssetStorageController.parseAssetsJson` Object-vs-Array → 0 assets (mock provision writes metadata object, parser expects `List<BrandingAsset>`).
- **GAP-1021** (Agent D deploy pipeline) — flag needs-rework (curl-walk PASS but browser-walk found 5 bugs; per `g1-browser-walk-before-flip`).

## 5. State

- Branch `feature/tier-ui-fix-g2-browser-2026-06-09`, **all local, NOT pushed**.
- Recent commits: `db8cfa8f` (EventSource abs URL) ← `2334f5a1` (DOC-2) ← `87a977d1` (B) ← `e3fe0298` (A) ← branding bug 1/2/3/UNKNOWN + GAP-1105/1106/1107 + GAP-1101.
- CSV `gap-status.csv` 894 rows validated. Working tree clean.
- Worktrees still present (not cleaned): `agent-aba375fdc5ab41490` (A), `agent-a4be2dd216ca42b1d` (B), `agent-a51f0c2ea1f734125` (DOC-2), `agent-a34070804e118d335` (DOC-1 stale) — cherry-picks done from A/B/DOC-2; can `git worktree remove` to clean.

## 6. Pickup order (next session)

1. User re-walk branding Step 6 → confirm SSE clean → flip GAP-1105 DONE (+ CORS fix if surfaces).
2. Rebuild kiteclass-core + kiteclass-frontend → runtime-walk 3 import/enroll features → flip GAP-1102/1103/1104 DONE.
3. Redo DOC-1 (SSE + branding-job report) on feature-branch state.
4. GAP-1106 + GAP-1107 BE fixes (own focused wave).
5. Clean stale worktrees.
6. Push + PR when user authorizes.
