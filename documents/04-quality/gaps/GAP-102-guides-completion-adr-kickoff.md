# GAP-102: 05-guides Completion + ADR Kickoff

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Documentation — Operations & Architecture Decision Records
**Found:** 2026-04-18 (session Q&A về 05-guides purpose)
**Affects:** `documents/05-guides/`, `documents/02-architecture/adr/`

## Problem

**Part 1 — `documents/05-guides/` thiếu operational guides quan trọng:**

Hiện có 7 items:
- `SECRET-MANAGEMENT.md`
- `deploy-go-nogo-checklist.md`
- `incident-response-runbook.md`
- `rollback-procedure.md`
- `wsl-migration-playbook.md`
- `operations/runbooks/deployment-procedures.md`
- `vietnamese/huong-dan-deploy-oracle-cloud.md`

Thiếu 6 guides operational quan trọng:
1. **Local dev setup (non-WSL)** — Mac/Linux native developers
2. **Monitoring & alerting runbook** — Prometheus/Grafana setup, alert response SOP (Wave 6 dependency)
3. **Database backup/restore SOP** — procedure khi cần restore, test restore quarterly
4. **CI/CD release procedure** — step-by-step từ PR merge → deploy to prod
5. **Security incident playbook** — breach response, PDPL violation, credentials rotation
6. **Tenant onboarding operational checklist** — new school provisioning end-to-end

**Part 2 — `documents/02-architecture/adr/` rỗng, thiếu ADR template + ADR-001:**

Folder `adr/` đã tồn tại nhưng không có file nào. Rule `output-review-mandate.md` §5.3 bắt buộc ADR template cho architectural decisions, hiện chưa tồn tại.

User Q&A ví dụ: "tại sao không dùng batch mà dùng jobs + RabbitMQ?" — đây là **architectural decision rationale**, đúng chỗ là ADR chứ không phải guide.

Hiểu lầm thường gặp: developers tạo file "why-rabbitmq.md" ở `05-guides/` (sai — đó là operator-facing) thay vì `02-architecture/adr/` (đúng — decision rationale).

## Root Cause

- `05-guides/` thiếu philosophy documented → không rõ cái gì thuộc đây
- ADR folder tồn tại nhưng chưa kick off → decisions tản mác trong commit messages, planning docs, PR descriptions
- "Architectural decision" vs "operational guide" boundary chưa định nghĩa

## Proposed Fix

### Part 1: 05-guides completion (batch 2 PRs)

**PR 1 — High-priority ops guides (P1 for prod):**
- `monitoring-alerting-runbook.md` (Wave 6 kickoff doc)
- `database-backup-restore-sop.md` (GAP-093 completion)
- `security-incident-playbook.md` (compliance requirement)

**PR 2 — Nice-to-have ops guides (P2):**
- `local-dev-setup-non-wsl.md`
- `cicd-release-procedure.md`
- `tenant-onboarding-checklist.md`

### Part 2: ADR kickoff (1 PR)

**File tạo:**
1. `documents/02-architecture/adr/README.md` — ADR process + template + index
2. `documents/02-architecture/adr/TEMPLATE.md` — MADR-style template (Context, Decision Drivers, Considered Options, Decision Outcome, Consequences)
3. `documents/02-architecture/adr/ADR-001-async-jobs-over-batch.md` — first ADR explaining jobs+RabbitMQ vs batch choice:
   - Context: AI generation takes 2-5 min, email delivery ≥1000 tenants
   - Options: sync, Spring Batch, RabbitMQ + consumer, cron jobs
   - Decision: RabbitMQ + consumer + priority queues
   - Consequences: better UX (non-blocking), complex ops (DLQ, retry, backpressure), infra dep

**ADR naming:** `ADR-NNN-{kebab-case-title}.md`, chronological increment.

## Acceptance Criteria

### Part 1 (05-guides)
- [ ] 3 P1 guides (monitoring, backup, security) tạo và reviewed
- [ ] 3 P2 guides (local dev, CI/CD, tenant onboarding) tạo
- [ ] `05-guides/README.md` (từ GAP-101) list đầy đủ guides

### Part 2 (ADR)
- [ ] ADR template + README published
- [ ] ADR-001 (jobs+RabbitMQ vs batch) written + reviewed
- [ ] Rule `output-review-mandate.md` §5.3 mark resolved
- [ ] CLAUDE.md link tới ADR folder

## Dependencies

- **GAP-101** — README cho `05-guides/` (từ GAP-101) cần tồn tại trước (hoặc trong cùng PR)
- **Wave 6 (AI Billing + Observability)** — `monitoring-alerting-runbook.md` nên align với Prometheus setup của Wave 6
- **GAP-086, 087, 088 (P0 ops gaps)** — đã có, content ops đã mature để viết runbook

## Related

- Rule `output-review-mandate.md` §5.3 ADR violation
- GAP-093 (backup not functional) — backup-restore-sop consume GAP-093 implementation
- GAP-101 (docs folder READMEs) — prerequisite
- Wave 6 — monitoring runbook timing
