# GAP-952: Saga compensation failure chỉ log warn — admin không biết để clean orphan

**Status:** 🟡 PARTIAL (60% — app-level metric + alert + sweep + tests shipped; CloudWatch live-apply + fault-injection verify deferred to AWS restore)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning saga compensation) + observability
**Defer-to:** After Wave flow-kh3 finish

## Problem

BR-PROV-005: "Compensation failure is logged but never rethrown (best-effort)". `compensate()` catch RuntimeException + `log.error`. KHÔNG emit alert / metric / dead-letter. Khi saga fail → markFailed compensation cũng fail (vd DB connection lost) → instance row stuck status `GENERATING` forever. Admin không nhận alert → 3 tháng sau audit "0 healthy instance metric drop" thì mới phát hiện. Surfaced: persona Finding 4.3.

## Proposed Fix

Wire `compensate()` failure thành CloudWatch metric `tenant_provisioning_compensation_failed` + SNS alarm threshold >0. Dead-job sweep cron (`@Scheduled`) scan instances stuck `GENERATING`/`INITIALIZING` > 10 min → escalate.

## Current State (verified 2026-06-06 — Wave provisioning-1 Bucket D)

App-level (kiteclass-core) shipped:
- `TenantProvisioningSaga.compensate()` — emits Micrometer counter `tenant_provisioning.compensation{result=success|failed}` + structured alert log token `TENANT_PROVISIONING_COMPENSATION_FAILED` when compensation itself throws (instance stuck pre-FAILED). Original saga failure still rethrown (secondary not masked).
- `ProvisioningStuckSweep` — NEW `@Scheduled` cron (default `0 */5 * * * *`, every 5 min; configurable) scans INITIALIZING/GENERATING instances older than threshold (default 10 min, configurable), marks them FAILED via `InstanceLifecycleService.markFailed`, emits counter `tenant_provisioning.stuck{result=swept|sweep_failed}` + alert token `TENANT_PROVISIONING_STUCK`. Cron never propagates.
- Tests: `TenantProvisioningSagaTest` 7/7 (+2 compensation metric tests), `ProvisioningStuckSweepTest` 6/6 — both PASS via `./mvnw test`.

IaC (infrastructure/terraform-aws/cloudwatch-provisioning-alarms.tf) shipped (live-apply deferred):
- Log group `/kite/kiteclass-core/app` + 2 log-metric-filters (tokens → CW metrics `tenant_provisioning_compensation_failed` + `tenant_provisioning_stuck` namespace `KiteClass/Provisioning`) + 2 alarms threshold `>0` → SNS `production_alerts`.

App-level-vs-IaC split: no CloudWatch meter registry in this stack → alarm driven by LOG TOKEN (metric filter), Micrometer counter serves /actuator + Prometheus only.

## Acceptance Criteria

- [ ] CloudWatch alarm `tenant_provisioning_compensation_failed > 0` fires SNS — IaC declared; **live-apply deferred** (kc-app CloudWatch agent must ship app logs to `/kite/kiteclass-core/app` + AWS restore per GAP-612; `terraform import` may be needed if log group auto-created)
- [x] Sweep cron job `provisioning-stuck-sweep` scheduled (every 5 min) — `ProvisioningStuckSweep` + 6 tests
- [ ] Manual fail injection → alert arrives within 5 min — **deferred** (requires live AWS stack; fault-injection walk per `feature-ship-runtime-walk-mandate.md` §2)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 4.3
- Sister: matrix A1×E5×EC5 (saga DEPLOYED step no SLA sweep)
- Flow Verification Campaign §4 row KC-1
- Wave: wave-2026-06-06-provisioning-1-tenant-saga (Bucket D)

## Log

- **2026-06-06** Wave provisioning-1 Bucket D — app-level compensation alert (metric + log token) + `ProvisioningStuckSweep` @Scheduled cron + 2 CloudWatch metric-filter alarms → SNS shipped. NO Flyway migration (uses existing `initializing_at`/`generating_at`/`status` columns). Status OPEN → PARTIAL 60%: AC #2 DONE (cron + tests); AC #1 + #3 deferred (CloudWatch live-apply + fault-injection require AWS restore per GAP-612). Only `TenantProvisioningSaga.compensate()` edited (NOT the consumer — Bucket C owns the consumer hook).
