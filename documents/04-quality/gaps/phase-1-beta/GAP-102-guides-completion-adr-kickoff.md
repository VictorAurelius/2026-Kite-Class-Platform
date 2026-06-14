# GAP-102: 05-guides Completion + ADR Kickoff

**Status:** 🟡 PARTIAL (Part 2 + Part 1 P2 batch done; Part 1 P1 batch open)
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

**Part 2 — `documents/02-architecture/adr/` index stale + thiếu ADR cho async jobs decision:**

[Correction 2026-04-18] Folder `adr/` KHÔNG rỗng — đã có 13 ADRs (001-013) shipped 2026-04-14 + `_TEMPLATE.md` (Michael Nygard format). Vấn đề thực tế:
- `adr/README.md` index stale — chỉ list 5/13 ADRs
- Chưa có ADR cho "async jobs + RabbitMQ vs Spring Batch" decision (implicit từ Wave 1, explicit từ Wave 3)

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

### Part 2: ADR index backfill + ADR-014 (DONE — PR #348)

**Files updated/created:**
1. `documents/02-architecture/adr/README.md` — backfill index từ 5 → 14 ADRs (all 13 existing + ADR-014)
2. `documents/02-architecture/adr/ADR-014-async-jobs-queue-over-batch.md` — retroactive ADR capturing RabbitMQ-over-Batch decision theo existing Nygard `_TEMPLATE.md` format

**ADR naming:** `ADR-NNN-{kebab-case-title}.md`, chronological increment (next = ADR-015 cho GAP-103).

## Acceptance Criteria

### Part 1 (05-guides)
- [ ] 3 P1 guides (monitoring, backup, security) tạo và reviewed — blocked by Wave 6 + legal input
- [x] 3 P2 guides (local dev, CI/CD, tenant onboarding) tạo — PR #352
- [x] `05-guides/README.md` list đầy đủ guides shipped (philosophy section updated)

### Part 2 (ADR) — DONE PR #348
- [x] ADR index backfilled (5 → 14 ADRs listed)
- [x] ADR-014 (async jobs queue vs batch framework) written theo existing Nygard template
- [x] `02-architecture/README.md` statement corrected (folder đã có 13 ADRs, không phải rỗng)
- [x] `_TEMPLATE.md` existing used — không cần template mới

## Dependencies

- **GAP-101** — README cho `05-guides/` (từ GAP-101) cần tồn tại trước (hoặc trong cùng PR)
- **Wave 6 (AI Billing + Observability)** — `monitoring-alerting-runbook.md` nên align với Prometheus setup của Wave 6
- **GAP-086, 087, 088 (P0 ops gaps)** — đã có, content ops đã mature để viết runbook

## Related

- Rule `output-review-mandate.md` §5.3 ADR violation
- GAP-093 (backup not functional) — backup-restore-sop consume GAP-093 implementation
- GAP-101 (docs folder READMEs) — prerequisite
- Wave 6 — monitoring runbook timing

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (05-guides docs completion + ADR kickoff; docs/meta active).
- **2026-05-11:** PR# backfill (Wave 60 Bucket D-2). Verified shipped work cross-references:
  - PR #346 — `docs(gaps): GAP-101/102/103 docs folder governance + generic docs-folder-structure rule` (2026-04-18) — initial governance scaffold.
  - PR #348 — Part 2 ADR shipped (ADR-014 async jobs queue over batch + ADR README backfill 5 → 14 ADRs).
  - PR #350 — `docs(adr): GAP-102 Part 2 — ADR index backfill + ADR-014 async jobs queue over batch`.
  - PR #352 — `docs(guides): GAP-102 Part 1 P2 — 3 operational guides` (local-dev, CI/CD, tenant onboarding).

  Code-verify: Part 2 (ADR) 4/4 AC ticked. Part 1 P2 (3 nice-to-have guides) 2/2 AC ticked. Part 1 P1 (3 production-critical guides — monitoring/backup/security) 0/1 AC unchecked, **explicitly blocked on Wave 6 (AI Billing + Observability) + legal input** per gap §Dependencies.

  Verdict: 🟡 PARTIAL maintained — Part 2 + Part 1 P2 shipped; Part 1 P1 blocked per explicit Wave 6 dependency per `gap-done-discipline.md` §3 PARTIAL exit ramp.
