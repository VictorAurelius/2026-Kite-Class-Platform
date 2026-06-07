# 05-guides — Operational Guides & Runbooks

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Operator-facing documentation — "how to run, deploy, recover, and troubleshoot the system." Khác [`02-architecture/`](../02-architecture/) (design + rationale) và [`03-planning/`](../03-planning/) (roadmap). Mọi doc trong đây viết cho người **đang vận hành** production hoặc dev environment, không phải designer.

**Audience:** SRE, DevOps, On-call engineers, New developers setting up local env, Incident responders.

---

## Directory Map

| Folder | Purpose | Files |
|--------|---------|:-----:|
| [`local-dev/`](local-dev/) | WSL2 + non-WSL setup, mock data, pre-commit hooks | 5 |
| [`remote-access/`](remote-access/) | SSH direct, Claude Code mobile remote, mobile-resilient stack (Tailscale + mosh + tmux) | 2 |
| [`deploy/`](deploy/) | CI/CD pipeline, pre-deploy gate, rollback, restore | 4 |
| [`monitoring/`](monitoring/) | Alerting standards, SLO targets, bundle/resource budgets | 4 |
| [`operations/`](operations/) | DR plan, RTO/RPO matrix, incident response, 23 per-alert runbooks | 3 + runbooks/ |
| [`infrastructure/`](infrastructure/) | DNS, secrets, dependency governance | 3 |
| [`tenant-lifecycle/`](tenant-lifecycle/) | School onboarding (3-day target), off-boarding (PDPL retention) | 2 |
| [`branding/`](branding/) | AI Branding wizard flow + FE integration (operator-facing) | 2 |
| [`contributing/`](contributing/) | Content strategy, template contribution, starter-kit sync | 3 |
| [`scripts/`](scripts/) | Runnable migration/setup scripts (vd `ssh-mobile-migration/`) | 1 subdir |
| [`vietnamese/`](vietnamese/) | VN-language versions cho audience yêu cầu tiếng Việt | 7 |
| [`templates/`](templates/) | Reusable file templates (vd systemd unit) | 1 |
| [`rtk-pilot/`](rtk-pilot/) | Rust Token Killer measurement pilot docs | 2 |

---

## Khái niệm: Runbook (+ template)

**Runbook = tài liệu hướng dẫn vận hành từng-bước** cho quy trình lặp lại được mà con người thực thi tay — biến kiến thức "trong đầu" thành các bước tái lập. Khác **plan** (sẽ làm gì, ở `03-planning/`) và **ADR** (quyết định tại sao, ở `02-architecture/adr/`).

**Phân loại theo WHEN + tần suất** (per [`deployment-naming-convention.md`](../../.claude/rules/deployment-naming-convention.md) §2 — quyết định folder bằng cách hỏi "chạy khi nào? bao lâu một lần?"):

| Folder | Vòng đời | Tần suất | Ví dụ |
|---|---|---|---|
| `account-prep/` | Trước-môi-trường | 1 lần / vendor | đăng ký AWS / SePay / Zalo / Resend |
| `deploy/` | Trước + trong deploy | 1 lần / release | DNS setup, secrets seeding, cutover |
| `operations/` | Sau deploy | Lặp (cron / sự cố / định kỳ) | incident response, secret rotation |
| `operations/runbooks/` | Sau deploy | Mỗi alert cụ thể | 1 runbook / 1 CloudWatch alert |

**Naming** (per §3): `<topic>-<action>-runbook.md` (có nhánh quyết định) HOẶC `<topic>-<action>-procedure.md` (tuyến tính 1 mạch).

**Viết runbook mới:** copy [`_RUNBOOK-TEMPLATE.md`](_RUNBOOK-TEMPLATE.md) (Header → TL;DR → Prerequisites → Steps → Verify + Sad path → Liên quan). Ngôn ngữ narrative tiếng Việt + identifier English per [`dev-readable-doc-language.md`](../../.claude/rules/dev-readable-doc-language.md).

---

## File Placement Rules

- ✅ **Belongs here:**
  - Runbooks (step-by-step operational procedures)
  - Playbooks (decision trees for scenarios: dev setup, incident, rollback)
  - Checklists (go/no-go, pre-flight, post-deploy)
  - Troubleshooting guides (common errors + fixes)
  - Tenant lifecycle operational steps

- ❌ **Does NOT belong here:**
  - Architecture rationale ("why RabbitMQ over batch") → [`02-architecture/adr/`](../02-architecture/adr/)
  - System design docs → [`02-architecture/`](../02-architecture/)
  - Feature planning / waves → [`03-planning/`](../03-planning/)
  - Gap reports / audits → [`04-quality/`](../04-quality/)
  - Business rules per domain → [`01-business/`](../01-business/)

- **Naming:** `kebab-case.md`. Mỗi file ở đúng subfolder theo bảng trên — KHÔNG còn file mới ở root (chỉ README.md).
- **Sub-bucket README mandate:** mỗi subfolder phải có `README.md` per [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md). Liệt kê file + placement rules + archive policy của bucket đó.

---

## Subdirectories quick reference

Mỗi subfolder tự đầy đủ — README riêng giải thích bucket, file placement rules, related links. Bắt đầu từ subfolder bạn cần (vd: setup máy mới → [`local-dev/`](local-dev/); on-call sự cố → [`operations/`](operations/); release prod → [`deploy/`](deploy/)).

---

## Current Guides Coverage (production readiness checklist)

### 🔴 Production operations (must-have trước GA)
- ✅ Incident response runbook ([`operations/incident-response-runbook.md`](operations/incident-response-runbook.md))
- ✅ Rollback procedure ([`deploy/rollback-procedure.md`](deploy/rollback-procedure.md))
- ✅ Deploy go/no-go checklist ([`deploy/deploy-go-nogo-checklist.md`](deploy/deploy-go-nogo-checklist.md))
- ✅ Secret management ([`infrastructure/SECRET-MANAGEMENT.md`](infrastructure/SECRET-MANAGEMENT.md))
- ✅ Monitoring + alerting standards ([`monitoring/alerting-standards.md`](monitoring/alerting-standards.md))
- ✅ DR plan + RTO/RPO ([`operations/disaster-recovery-plan.md`](operations/disaster-recovery-plan.md), [`operations/dr-rto-rpo-matrix.md`](operations/dr-rto-rpo-matrix.md))
- ✅ Restore procedure ([`deploy/restore-procedure.md`](deploy/restore-procedure.md))
- ✅ CI/CD release procedure ([`deploy/cicd-release-procedure.md`](deploy/cicd-release-procedure.md))
- ❌ Security incident playbook (GAP-102, future)

### 🟠 Developer experience
- ✅ WSL2 fresh setup ([`local-dev/wsl2-fresh-setup.md`](local-dev/wsl2-fresh-setup.md))
- ✅ WSL migration playbook ([`local-dev/wsl-migration-playbook.md`](local-dev/wsl-migration-playbook.md))
- ✅ Local dev non-WSL ([`local-dev/local-dev-setup-non-wsl.md`](local-dev/local-dev-setup-non-wsl.md))
- ✅ SSH direct + mobile-resilient ([`remote-access/ssh-terminal-direct-access.md`](remote-access/ssh-terminal-direct-access.md))

### 🟡 Tenant lifecycle
- ✅ Tenant onboarding (3-day) ([`tenant-lifecycle/tenant-onboarding-checklist.md`](tenant-lifecycle/tenant-onboarding-checklist.md))
- ✅ Tenant off-boarding ([`tenant-lifecycle/tenant-off-boarding-runbook.md`](tenant-lifecycle/tenant-off-boarding-runbook.md))

### 🟢 AI Branding (operator)
- ✅ Wizard flow + support runbook ([`branding/ai-branding-wizard-flow.md`](branding/ai-branding-wizard-flow.md))
- ✅ FE integration ([`branding/branding-integration.md`](branding/branding-integration.md))

Completion tracked trong [GAP-102](../04-quality/gaps/GAP-102-guides-completion-adr-kickoff.md).

---

## Archive Policy

Move sang `documents/07-archived/guides-YYYY/` khi:
- Procedure obsoleted (vd. rollback strategy changed fundamentally)
- Technology retired (vd. if we drop Oracle Cloud, archive VN Oracle guide)
- Guide replaced by auto-generated runbook

Keep version history — runbooks là "living docs", update in-place với changelog section ở cuối.

---

## Related

- **Gaps:** [GAP-086](../04-quality/gaps/GAP-086-incident-response-runbook.md), [GAP-087](../04-quality/gaps/GAP-087-deploy-go-no-go.md), [GAP-088](../04-quality/gaps/GAP-088-rollback-procedure.md), [GAP-102](../04-quality/gaps/GAP-102-guides-completion-adr-kickoff.md)
- **Wave 6** — Monitoring + Observability drives monitoring runbooks
- **Wave 9** — Compliance MVP drives security incident playbook với legal sign-off

---

## Log

- **2026-05-04** — Restructured: 28 root-level files → 0 (only README.md). 8 new subfolders (local-dev, remote-access, deploy, monitoring, infrastructure, tenant-lifecycle, branding, contributing) + reorganized operations/ to absorb 3 root files + merged orphan runbooks/ → operations/runbooks/. Per `docs-folder-structure.md` rule. ~363 inbound references updated repo-wide.
