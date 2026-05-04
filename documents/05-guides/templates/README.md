# templates — Reusable file templates

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

File templates được referenced bởi guides khác (vd systemd unit, env templates). Audience: ops khi setup service mới.

---

## Directory Map

| File | Purpose | Used by |
|------|---------|---------|
| `claude-remote-control.service` | systemd unit cho Claude Code mobile remote service | [`../remote-access/remote-control-setup.md`](../remote-access/remote-control-setup.md) |

---

## File Placement Rules

- ✅ **Belongs here:** raw template files (systemd, env, docker-compose snippets) referenced bởi guide nào đó
- ❌ **Does NOT belong here:** prose docs (xem subfolder phù hợp), business logic templates (xem `documents/01-business/*/use-cases.md`)

---

## Archive Policy

Move sang `documents/07-archived/templates-YYYY/` khi guide reference template không còn dùng.
