# remote-access — SSH / Remote Dev Access

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Setup + workflow cho remote access vào dev machine: SSH direct, Claude Code mobile remote, mobile-resilient stack (Tailscale + mosh + tmux). Audience: dev cần work từ thiết bị ngoài (laptop khác, phone, công ty).

---

## Directory Map

| File | Purpose |
|------|---------|
| `ssh-terminal-direct-access.md` | SSH direct vào WSL2 dev terminal — sshd setup, Tailscale, tmux patterns, mosh layer (§3.4 cho mobile-resilient) |
| `remote-control-setup.md` | Claude Code mobile remote (built-in feature, alternative path không cần SSH) |

**Companion scripts:** [`../scripts/ssh-mobile-migration/`](../scripts/ssh-mobile-migration/) — runnable migration script cho stack mobile-resilient.

---

## File Placement Rules

- ✅ **Belongs here:** remote dev access setup (SSH, Tailscale, mosh, tmux, mobile remote)
- ❌ **Does NOT belong here:** production access (xem ops runbooks), local-only setup (xem [`../local-dev/`](../local-dev/))

---

## Architecture decision

Stack mobile-resilient (cho Android dev qua SSH):
1. **Tailscale** = network layer (peer-to-peer, no port-forward)
2. **mosh** = connection layer (UDP, survives mobile sleep + network roam)
3. **tmux** = process layer (survives SSH/mosh death)

Xem `ssh-terminal-direct-access.md` §3.4 chi tiết.

---

## Archive Policy

Move sang `documents/07-archived/remote-access-YYYY/` khi tool bị thay thế (vd nếu sau này dùng remote dev tool khác như Coder/GitPod).
