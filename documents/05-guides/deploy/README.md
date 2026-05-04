# deploy — CI/CD, Release, Rollback, Restore

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Procedures cho release lifecycle: pre-deploy gate, CI/CD pipeline, rollback khi deploy fail, restore từ backup. Audience: release manager, on-call, devs đẩy code lên prod.

---

## Directory Map

| File | Purpose |
|------|---------|
| `cicd-release-procedure.md` | PR merge → prod deploy procedure đầy đủ |
| `deploy-go-nogo-checklist.md` | Pre-deploy gate checklist (GAP-087) |
| `rollback-procedure.md` | Per-service rollback steps (GAP-088) |
| `restore-procedure.md` | Restore từ backup khi data loss (DR companion) |

---

## File Placement Rules

- ✅ **Belongs here:** release lifecycle (pre-deploy, deploy, rollback, restore)
- ❌ **Does NOT belong here:** infrastructure config (xem [`../infrastructure/`](../infrastructure/)), incident response (xem [`../operations/`](../operations/)), DR strategy (xem [`../operations/disaster-recovery-plan.md`](../operations/disaster-recovery-plan.md))
- Naming: action-oriented (`*-procedure`, `*-checklist`)

---

## Related

- DR plan + RTO/RPO matrix: [`../operations/`](../operations/)
- Incident runbooks: [`../operations/runbooks/`](../operations/runbooks/)
- Deployment Helm charts: `infrastructure/helm/`

---

## Archive Policy

Move sang `documents/07-archived/deploy-YYYY/` khi pipeline thay đổi major (vd switch GitHub Actions → GitLab CI).
