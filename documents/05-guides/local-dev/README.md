# local-dev — Local Development Environment Setup

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Hướng dẫn dựng môi trường dev local: WSL2 trên Windows, Mac/Linux native, mock data setup, pre-commit hooks. Audience: dev mới setup máy + dev di chuyển environment.

---

## Directory Map

| File | Purpose |
|------|---------|
| `wsl2-fresh-setup.md` | Clean-room reproducer cho Windows + WSL2 dev env. Bắt đầu ở đây cho máy mới. |
| `wsl-migration-playbook.md` | Migration WSL2 cho install Windows hiện có (preserves Claude Code memory) |
| `local-dev-setup-non-wsl.md` | Mac/Linux native dev setup |
| `local-dev-mock-data.md` | MSW mock data setup cho frontend dev |
| `local-dev-pre-commit.md` | Pre-commit hooks (husky, lefthook) cấu hình + troubleshooting |

---

## File Placement Rules

- ✅ **Belongs here:** OS-level setup, dev tooling install, mock data, local-only configs
- ❌ **Does NOT belong here:** CI/CD (xem [`../deploy/`](../deploy/)), production deploy (xem [`../deploy/`](../deploy/)), infra config (xem [`../infrastructure/`](../infrastructure/))
- Naming: `local-dev-*` cho dev-specific, `wsl*` cho WSL-specific

---

## Archive Policy

Move sang `documents/07-archived/local-dev-YYYY/` khi:
- Dev tool bị deprecated (vd guide cho tool không dùng nữa)
- OS version cũ hơn ≥2 major bản
- Doc >180 ngày không update + không có reference gần
