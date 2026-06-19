# GAP-966: Provisioning saga SLA + metrics + DEPLOYED step dead-job sweep + p99 latency

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Provisioning observability) — SLO + biz-metric + dead-job recovery
**Defer-to:** After Wave flow-kh3 finish

## Problem

(1) `TenantProvisioningSaga` AnalyzerService + PlannerService + PlanExecutor (AI branding). Nếu AI provider hang (Ollama down), saga stuck in GENERATING → tenant unusable indefinitely. KHÔNG có sweep job. (2) `provisioning.infrastructure.timeout-seconds: 120` BR-PROV config nhưng KHÔNG có CloudWatch metric `tenant_provisioning_duration_seconds` ship qua outbox. Admin không thể trả lời "Owner đợi bao lâu trung bình để tenant ready?" → BizDev (free tier promotion) decision không có data. Surfaced: matrix A1×E5×EC5 + persona Finding 4.5.

## Proposed Fix

(1) Wire `@Scheduled` dead-job sweep mỗi 5 phút: scan `frontend_instances WHERE status NOT IN ('DEPLOYED','FAILED') AND updated_at < NOW() - INTERVAL '10 minutes'` → mark FAILED + alert. (2) Add Micrometer `@Timed("tenant_provisioning_duration_seconds")` annotation cho saga. Ship Prometheus endpoint `/actuator/prometheus` exposing. Create CloudWatch dashboard với p50/p95/p99.

## Acceptance Criteria

- [ ] Sweep cron `provisioning-stuck-sweep` runs every 5 min
- [ ] `curl http://localhost:8081/actuator/prometheus | grep -i provision` returns metrics
- [ ] CloudWatch dashboard "Tenant Provisioning SLO" deployed
- [ ] Alert: p95 > 60s → SNS

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,tenant-provisioning}.md (multiple cells)
- Sister: GAP-952 (compensation alert)
- Flow Verification Campaign §4 row KC-1
