# GAP-119: Platform-Wide DR Runbook + RTO/RPO

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps / Business Continuity
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Entire platform business continuity

## Problem

GAP-030 (AI branding DR, P2 OPEN) cover AI-specific recovery nhưng **không có platform-wide DR plan**. Các domain khác (core tenant data, subscription/billing, email queue, multi-tenant isolation) chưa có RTO/RPO targets hoặc runbook.

Evidence:
- `documents/05-guides/` không có file tên `disaster-recovery*` hoặc `business-continuity*`
- GAP-030 targets AI-scope only: Ollama/MinIO/Postgres branding tables/RabbitMQ
- Không có RTO/RPO cho: tenant DBs, subscription DB, billing records, email outbox, Redis session

Risk: khi full outage (region down, DB crash, ransomware), không có playbook → improvise → prolonged downtime.

## Root Cause

DR planning chỉ được làm ad-hoc per domain khi audit/gap surfaces. Thiếu platform-level ownership.

## Proposed Fix

1. Create `documents/05-guides/disaster-recovery-plan.md` với:
   - **RTO/RPO Matrix:**
     | Component | RTO | RPO | Recovery mode |
     |-----------|-----|-----|---------------|
     | kitehub-subscription DB | 1h | 15min | RDS PITR |
     | kiteclass tenant DBs | 2h | 1h | pg_dump + S3 |
     | MinIO assets | 4h | 24h | S3 versioning + replication |
     | RabbitMQ queues | 10min | 5min | Durable + mirrored |
     | Redis session | 5min | N/A (re-login) | Skip, redirect to login |
   - **Scenario runbooks:**
     - S1: Region failure (AWS ap-southeast-1 down)
     - S2: DB crash (RDS instance lost)
     - S3: Ransomware (data encrypted by attacker)
     - S4: Mass tenant provision failure
     - S5: AI provider (OpenAI) down >2h
2. Quarterly DR exercise schedule
3. DR coordinator role + escalation path
4. Communication templates (tenants, regulators)
5. Integrate vào `deploy-go-nogo-checklist.md`: DR plan reviewed = GO criterion

## Acceptance Criteria

- [ ] `disaster-recovery-plan.md` tạo trong 05-guides với 5+ scenarios
- [ ] RTO/RPO matrix documented cho 6+ components
- [ ] Communication templates (tenant email, regulator notice)
- [ ] DR coordinator identified trong team
- [ ] First quarterly exercise scheduled
- [ ] Link từ CLAUDE.md vào plan

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §6
- Extends: GAP-030 (AI-scope DR, P2) — GAP-119 bổ sung platform-scope
- Depends: GAP-117 (restore drill), GAP-118 (MinIO backup)
- Related: `incident-response-runbook.md` (incident vs DR — different scopes)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
