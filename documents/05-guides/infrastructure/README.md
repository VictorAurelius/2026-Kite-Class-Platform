# infrastructure — Network, Secrets, Dependencies

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Cross-cutting infrastructure concerns: DNS, secret management, dependency governance. Audience: DevOps, security, devs cần update deps.

---

## Directory Map

| File | Purpose |
|------|---------|
| `SECRET-MANAGEMENT.md` | K8s Sealed Secrets + Vault setup, rotation policy |
| `dependabot-guide.md` | Dependabot config + workflow + troubleshooting (audience: Devs + Claude) |
| `dns-operations.md` | DNS zone setup, subdomain provisioning per tenant |

---

## File Placement Rules

- ✅ **Belongs here:** infra-level concerns (DNS, secrets, deps) cross all services
- ❌ **Does NOT belong here:** per-service config (xem service README), CI/CD (xem [`../deploy/`](../deploy/)), monitoring (xem [`../monitoring/`](../monitoring/))

---

## Vietnamese version

`SECRET-MANAGEMENT.md` có VN translation tại [`../vietnamese/SECRET-MANAGEMENT.md`](../vietnamese/SECRET-MANAGEMENT.md).

---

## Archive Policy

Move sang `documents/07-archived/infrastructure-YYYY/` khi tool stack thay đổi (vd switch Sealed Secrets → External Secrets Operator).
