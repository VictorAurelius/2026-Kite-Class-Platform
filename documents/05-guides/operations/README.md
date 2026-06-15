# operations — DR, Incident Response, Runbooks

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

SRE/on-call procedures: disaster recovery, incident triage, per-alert runbooks. Audience: on-call engineers, SRE, incident commanders.

---

## Directory Map

| Path | Purpose |
|------|---------|
| `disaster-recovery-plan.md` | DR strategy + recovery scenarios (xem cũng `dr-rto-rpo-matrix.md`) |
| `dr-rto-rpo-matrix.md` | RTO/RPO targets per service tier |
| `incident-response-runbook.md` | SEV1-SEV3 triage procedure (GAP-086) |
| `beta-invite-flow.md` | End-to-end Beta invite luồng 5 bước: request → admin review → email → signup → first login (GAP-480) |
| `aws-cost-and-credits-runbook.md` | Giảm chi phí AWS + kiếm credits + maintenance keep-stopped + monitoring (sau sự cố bill spike 2026-06-15) |
| `runbooks/` | 23 per-alert runbooks (mỗi alert trong Prometheus có 1 runbook) |

---

## File Placement Rules

- ✅ **Belongs here:** ops procedures khi production có vấn đề (DR, incident, alert response)
- ❌ **Does NOT belong here:**
  - Pre-deploy procedures (xem [`../deploy/`](../deploy/))
  - Alert config (xem `infrastructure/helm/prometheus/`)
  - Alert standards (xem [`../monitoring/alerting-standards.md`](../monitoring/alerting-standards.md))

---

## Per-alert runbook discovery

Mỗi Prometheus alert PHẢI có `runbook_url` annotation pointing vào 1 file trong `runbooks/`. Naming: `<alert-id-kebab-case>.md`. Xem [`../monitoring/alerting-standards.md`](../monitoring/alerting-standards.md) cho mandate chi tiết.

---

## Related

- Restore procedure: [`../deploy/restore-procedure.md`](../deploy/restore-procedure.md) (companion cho DR)
- Rollback procedure: [`../deploy/rollback-procedure.md`](../deploy/rollback-procedure.md) (incident response)
- Alert standards: [`../monitoring/alerting-standards.md`](../monitoring/alerting-standards.md)

---

## Archive Policy

Move sang `documents/07-archived/operations-YYYY/` khi DR strategy thay đổi major. Per-alert runbooks: nếu alert bị remove khỏi Prometheus → archive corresponding runbook.
